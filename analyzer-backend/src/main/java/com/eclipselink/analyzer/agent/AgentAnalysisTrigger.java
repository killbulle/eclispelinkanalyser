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

    public static void run(Session session) throws Exception {
        // 1. Extract Metamodel
        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        // 2. Get Connection from Session
        Connection conn = null;
        if (session instanceof AbstractSession) {
            AbstractSession as = (AbstractSession) session;
            if (as.getAccessor() != null) {
                conn = as.getAccessor().getConnection();
            }
        }

        if (conn == null) {
            System.err.println("[AnalyzerAgent] Could not obtain JDBC connection from session.");
            return;
        }

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
        String outputPath = "eclipselink-analysis-report.json";
        runner.runAnalysis(nodes, schema, rules, outputPath);

        System.out.println("[AnalyzerAgent] Analysis report exported to: " + outputPath);
    }
}
