package com.eclipselink.analyzer.agent;

import com.eclipselink.analyzer.AnalysisRunner;
import com.eclipselink.analyzer.DDLInspector;
import com.eclipselink.analyzer.MetamodelExtractor;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.rules.EagerFetchRule;
import com.eclipselink.analyzer.rules.RelationshipOwnerRule;
import com.eclipselink.analyzer.rules.RedundantUpdateRule;
import com.eclipselink.analyzer.rules.MappingRule;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.internal.sessions.AbstractSession;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AgentAnalysisTrigger {

    public static List<EntityNode> run(Session session) throws Exception {
        // 1. Extract Metamodel
        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        // 2. Get Connection from Session
        Connection conn = null;
        boolean isSecondaryConn = false;
        if (session instanceof AbstractSession) {
            AbstractSession as = (AbstractSession) session;
            if (as.getAccessor() != null && as.getAccessor().getConnection() != null) {
                conn = as.getAccessor().getConnection();
            } else if (session instanceof org.eclipse.persistence.sessions.server.ServerSession) {
                org.eclipse.persistence.sessions.server.ServerSession ss = (org.eclipse.persistence.sessions.server.ServerSession) session;
                System.out.println("[AnalyzerAgent] ServerSession detected. Login: " + ss.getLogin());

                org.eclipse.persistence.internal.databaseaccess.Accessor accessor = null;
                if (ss.getDefaultConnectionPool() != null) {
                    try {
                        System.out.println("[AnalyzerAgent] Trying DefaultConnectionPool...");
                        accessor = ss.getDefaultConnectionPool().acquireConnection();
                    } catch (Exception e) {
                        System.err.println("[AnalyzerAgent] DefaultPool failed: " + e.getMessage());
                    }
                }
                if (accessor == null && ss.getReadConnectionPool() != null) {
                    try {
                        System.out.println("[AnalyzerAgent] Trying ReadConnectionPool...");
                        accessor = ss.getReadConnectionPool().acquireConnection();
                    } catch (Exception e) {
                        System.err.println("[AnalyzerAgent] ReadPool failed: " + e.getMessage());
                    }
                }

                if (accessor != null) {
                    conn = accessor.getConnection();
                    isSecondaryConn = true;
                }
            } else if (as.getLogin() != null && as.getLogin().getConnector() != null) {
                try {
                    System.out.println("[AnalyzerAgent] Primary accessor null, creating secondary connection...");
                    java.util.Properties props = as.getLogin().getProperties();
                    if (props == null)
                        props = new java.util.Properties();
                    conn = as.getLogin().getConnector().connect(props, as);
                    isSecondaryConn = true;
                } catch (Exception e) {
                    System.err.println("[AnalyzerAgent] Failed to create secondary connection: " + e.getMessage());
                }
            }
        }

        if (conn == null) {
            System.err.println("[AnalyzerAgent] Could not obtain JDBC connection from session.");
            // Return nodes anyway, even if schema analysis fails (or just return empty, but
            // nodes are valuable)
            return nodes;
        }

        try {
            // 3. Inspect Schema
            DDLInspector inspector = new DDLInspector();
            Map<String, DDLInspector.TableMetadata> schema = inspector.inspectSchema(conn);

            // 4. Run Rules
            List<MappingRule> rules = Arrays.asList(
                    new EagerFetchRule(),
                    new RelationshipOwnerRule(),
                    new RedundantUpdateRule());

            // 5. Export Report
            AnalysisRunner runner = new AnalysisRunner();
            java.io.File reportsDir = new java.io.File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            String outputPath = "reports/agent-report.json";
            runner.runAnalysis(nodes, schema, rules, outputPath);

            System.out.println("[AnalyzerAgent] Analysis report exported to: " + outputPath);
        } finally {
            if (isSecondaryConn && conn != null) {
                try {
                    conn.close();
                    System.out.println("[AnalyzerAgent] Secondary connection closed.");
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        return nodes;
    }
}
