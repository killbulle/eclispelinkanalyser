package com.eclipselink.analyzer;

import com.eclipselink.analyzer.graph.GraphAnalyzer;
import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import com.eclipselink.analyzer.rules.*;
import com.eclipselink.analyzer.rules.EagerFetchRule;
import com.eclipselink.analyzer.rules.MappingRule;
import com.eclipselink.analyzer.rules.RelationshipOwnerRule;
import com.eclipselink.analyzer.rules.RedundantUpdateRule;
import com.eclipselink.analyzer.rules.OptimisticLockingRule;
import com.eclipselink.analyzer.rules.ForeignKeyIndexRule;
import com.eclipselink.analyzer.rules.LargeCollectionRule;
import com.eclipselink.analyzer.rules.SelfReferencingRule;
import com.eclipselink.analyzer.rules.InheritanceRule;
import com.eclipselink.analyzer.rules.GraphAnalysisRule;
import com.eclipselink.analyzer.rules.LobRule;
import com.eclipselink.analyzer.rules.TemporalRule;
import com.eclipselink.analyzer.rules.VersionRule;
import com.eclipselink.analyzer.rules.BatchFetchRule;
import com.eclipselink.analyzer.rules.InheritanceStrategyRule;
import com.eclipselink.analyzer.rules.DiscriminatorRule;
import com.eclipselink.analyzer.rules.NPlusOneQueryRule;
import com.eclipselink.analyzer.rules.CartesianProductRule;
import com.eclipselink.analyzer.rules.IndexRule;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Persistence;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.jpa.JpaHelper;
import com.eclipselink.analyzer.agent.AgentAnalysisTrigger;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting EclipseLink Mapping Analyzer...");

        // 1. Cleanup old reports as requested
        cleanupCatalog();

        // 2. Generate Real Catalog for All Levels (Extracts from Metamodel)
        generateRealCatalogAll();


        // 4. OFBiz stress test if available
        java.io.File ofbizDir = new java.io.File("ofbiz-stress-test");
        if (!ofbizDir.exists())
            ofbizDir = new java.io.File("../ofbiz-stress-test");
        if (ofbizDir.exists()) {
            generateOfbizReport();
        }

        System.out.println("All mapping reports regenerated successfully!");
    }

    private static void cleanupCatalog() {
        System.out.println("Cleaning up old catalog reports...");
        java.io.File catalogDir = new java.io.File("reports/catalog");
        if (catalogDir.exists() && catalogDir.isDirectory()) {
            java.io.File[] files = catalogDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (java.io.File f : files) {
                    f.delete();
                }
            }
        }
    }

    private static void generateOfbizReport() throws Exception {
        com.eclipselink.analyzer.stress.OFBizConverter converter = new com.eclipselink.analyzer.stress.OFBizConverter();
        String path = "ofbiz-stress-test";
        if (!new java.io.File(path).exists()) {
            path = "../ofbiz-stress-test";
        }
        List<EntityNode> nodes = converter.convertFolder(path);
        System.out.println("Extracted " + nodes.size() + " entities from OFBiz.");
        runAnalysis(nodes, "OFBiz_Stress_Test", "ofbiz-report.json");
    }

    private static void runAnalysis(List<EntityNode> nodes, String dbName, String outputPath) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "");
        // Simplified DDL generation
        for (EntityNode node : nodes) {
            // Skip non-table types: MAPPED_SUPERCLASS, EMBEDDABLE, ABSTRACT_ENTITY
            if ("MAPPED_SUPERCLASS".equals(node.getType()) ||
                    "EMBEDDABLE".equals(node.getType()) ||
                    "ABSTRACT_ENTITY".equals(node.getType())) {
                continue;
            }
            StringBuilder ddl = new StringBuilder("CREATE TABLE " + node.getName().toUpperCase() + " (");
            boolean first = true;
            for (AttributeMetadata attr : node.getAttributes().values()) {
                if (!first)
                    ddl.append(", ");
                ddl.append(attr.getColumnName()).append(" ").append(attr.getDatabaseType());
                first = false;
            }
            ddl.append(")");
            conn.createStatement().execute(ddl.toString());
        }

        DDLInspector inspector = new DDLInspector();
        Map<String, DDLInspector.TableMetadata> schema = inspector.inspectSchema(conn);

        AnalysisRunner runner = new AnalysisRunner();
        List<MappingRule> rules = Arrays.asList(
                new EagerFetchRule(),
                new RelationshipOwnerRule(),
                new RedundantUpdateRule(),
                new OptimisticLockingRule(),
                new ForeignKeyIndexRule(),
                new LargeCollectionRule(),
                new SelfReferencingRule(),
                new CacheRule(),
                new InheritanceRule(),
                new LobRule(),
                new TemporalRule(),
                new VersionRule(),
                new BatchFetchRule(),
                new InheritanceStrategyRule(),
                new DiscriminatorRule(),
                new NPlusOneQueryRule(),
                new CartesianProductRule(),
                new MappedSuperclassRule(), // New
                new IndirectionPolicyRule(), // New
                new ObjectTypeConverterRule(), // Phase 1
                new DirectMapRule(), // Phase 1
                new AggregateCollectionRule(), // Phase 1
                new IndexRule(),
                // Phase 2 Mappings
                new TransformationMappingRule(),
                new VariableOneToOneRule(),
                new DirectCollectionRule(),
                new ArrayMappingRule(),
                new NestedTableRule(),
                new ConverterRule());

        List<GlobalMappingRule> globalRules = Arrays.asList(
                new GraphAnalysisRule());

        java.io.File reportsDir = new java.io.File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        String finalPath = "reports/" + outputPath;
        runner.runAnalysis(nodes, schema, rules, globalRules, finalPath);
    }

    private static void generateRealCatalogAll() throws Exception {
        System.out.println("Generating Real Catalog for ALL Levels (1-6)...");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("RealAgentTest");
        EntityManager em = emf.createEntityManager();
        Session session = JpaHelper.getServerSession(emf);
        if (session == null) {
            session = ((org.eclipse.persistence.internal.jpa.EntityManagerImpl) em.getDelegate()).getActiveSession();
        }

        List<EntityNode> allNodes = AgentAnalysisTrigger.run(session);

        // Level 1: Basic
        runRealAnalysis(allNodes, "1-1-basic-entity-real.json", "l1_basic");

        // Level 2: Relationships
        runRealAnalysis(allNodes, "2-1-onetoone-real.json", "l2_relationships.onetoone");
        runRealAnalysis(allNodes, "2-2-onetomany-real.json", "l2_relationships.onetomany");
        runRealAnalysis(allNodes, "2-3-manytomany-real.json", "l2_relationships.manytomany");
        runRealAnalysis(allNodes, "2-4-element-collection-real.json", "l2_relationships.element");
        runRealAnalysis(allNodes, "2-5-embedded-real.json", "l2_relationships.embedded");
        runRealAnalysis(allNodes, "2-5-mapped-superclass.json", "l2_relationships.mapped_superclass");

        // Level 3: Converters
        runRealAnalysis(allNodes, "3-1-basic-converter-real.json", "l3_converters.basic");
        runRealAnalysis(allNodes, "3-2-object-type-real.json", "l3_converters.objecttype");
        runRealAnalysis(allNodes, "3-3-serialized-object-real.json", "l3_converters.serialized");

        // Level 4: EclipseLink Specific
        runRealAnalysis(allNodes, "4-1-batch-fetch-real.json", "l4_specific.batch");
        runRealAnalysis(allNodes, "4-2-cache-config-real.json", "l4_specific.cache");
        runRealAnalysis(allNodes, "4-3-indirection-real.json", "l4_specific.indirection");
        runRealAnalysis(allNodes, "4-4-private-owned-real.json", "l4_specific.privateowned");

        // Level 5: Advanced Mappings
        runRealAnalysis(allNodes, "5-1-transformation-real.json", "l5.transform");
        runRealAnalysis(allNodes, "5-2-variable-onetoone-real.json", "l5.variable");
        runRealAnalysis(allNodes, "5-3-direct-collection-real.json", "l5.direct");
        runRealAnalysis(allNodes, "5-4-aggregate-collection-real.json", "l5.aggregate");
        runRealAnalysis(allNodes, "5-5-array-real.json", "l5.array");

        // Level 6: Anti-Patterns
        runRealAnalysis(allNodes, "6-1-circular-refs-real.json", "l6_antipatterns.circular");
        // For Cartesian, include circular package as it refers to CyclicA/B
        runRealAnalysis(allNodes, "6-2-cartesian-product-real.json", "l6_antipatterns.cartesian",
                "l6_antipatterns.circular");
        // For Optimization, include circular package as it refers to CyclicA/B
        runRealAnalysis(allNodes, "6-3-missing-optimizations-real.json", "l6_antipatterns.opt",
                "l6_antipatterns.circular");

        em.close();
        emf.close();
    }

    private static void runRealAnalysis(List<EntityNode> allNodes, String outputFile, String... packageFilters)
            throws Exception {
        List<EntityNode> filtered = new ArrayList<>();
        for (EntityNode n : allNodes) {
            String pkg = n.getPackageName();
            if (pkg != null) {
                for (String filter : packageFilters) {
                    if (pkg.contains(filter)) {
                        filtered.add(n);
                        break;
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            System.out.println("Warning: No nodes found for filters: " + Arrays.toString(packageFilters));
            return;
        }

        // Use empty schema and standard rules for now
        Map<String, DDLInspector.TableMetadata> schema = new HashMap<>();

        List<MappingRule> rules = Arrays.asList(
                new EagerFetchRule(),
                new RelationshipOwnerRule());

        AnalysisRunner runner = new AnalysisRunner();
        runner.runAnalysis(filtered, schema, rules, "reports/catalog/" + outputFile);
    }
}
