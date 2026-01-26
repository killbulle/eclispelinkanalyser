package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.rules.EagerFetchRule;
import com.eclipselink.analyzer.rules.MappingRule;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExampleAnalysisRunner {
    public static void main(String[] args) {
        System.out.println("Starting EclipseLink Mapping Analyzer on Real Examples...");

        // 1. Setup Persistence Properties
        Map<String, String> properties = new HashMap<>();
        properties.put("javax.persistence.transactionType", "RESOURCE_LOCAL");
        properties.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
        properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:employee_test;DB_CLOSE_DELAY=-1");
        properties.put("javax.persistence.jdbc.user", "sa");
        properties.put("javax.persistence.jdbc.password", "");
        properties.put("eclipselink.ddl-generation", "create-tables");
        properties.put("eclipselink.logging.level", "OFF");

        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            System.out.println("Initializing EntityManagerFactory for unit 'employee'...");
            emf = Persistence.createEntityManagerFactory("employee", properties);
            if (emf == null) {
                System.err.println("CRITICAL: EntityManagerFactory is null!");
                return;
            }

            System.out.println("Creating EntityManager...");
            em = emf.createEntityManager();
            if (em == null) {
                System.err.println("CRITICAL: EntityManager is null!");
                return;
            }

            System.out.println("Extracting EclipseLink Session...");
            Object delegate = em.getDelegate();
            if (!(delegate instanceof JpaEntityManager)) {
                System.err.println("CRITICAL: Delegate is not a JpaEntityManager. Class: "
                        + (delegate == null ? "null" : delegate.getClass().getName()));
                return;
            }

            Session session = ((JpaEntityManager) delegate).getActiveSession();
            if (session == null) {
                System.err.println("CRITICAL: Active Session is null!");
                return;
            }

            System.out.println("Running Metamodel Extraction...");
            MetamodelExtractor extractor = new MetamodelExtractor();
            List<EntityNode> nodes = extractor.extract(session);
            System.out.println("Extracted " + nodes.size() + " entity nodes.");

            System.out.println("Inspecting DDL from H2...");
            Connection conn = DriverManager.getConnection("jdbc:h2:mem:employee_test;DB_CLOSE_DELAY=-1", "sa", "");
            DDLInspector inspector = new DDLInspector();
            Map<String, DDLInspector.TableMetadata> schema = inspector.inspectSchema(conn);
            System.out.println("Inspected " + schema.size() + " tables.");

            System.out.println("Performing Analysis and Rule Checks...");
            AnalysisRunner runner = new AnalysisRunner();
            List<MappingRule> rules = Arrays.asList(new EagerFetchRule());

            String outputPath = "real-example-report.json";
            runner.runAnalysis(nodes, schema, rules, outputPath);

            System.out.println("Analysis complete! Result written to: " + outputPath);

        } catch (Throwable t) {
            System.err.println("ANALYSIS FAILED with exception:");
            t.printStackTrace();
        } finally {
            if (em != null) {
                try {
                    em.close();
                } catch (Exception e) {
                }
            }
            if (emf != null) {
                try {
                    emf.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
