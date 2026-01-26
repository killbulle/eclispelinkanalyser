package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.rules.GlobalMappingRule;
import com.eclipselink.analyzer.rules.MappingRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisRunner {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void runAnalysis(List<EntityNode> nodes, Map<String, DDLInspector.TableMetadata> schema,
            List<MappingRule> rules, String outputPath) throws Exception {
        runAnalysis(nodes, schema, rules, new ArrayList<>(), outputPath);
    }

    public void runAnalysis(List<EntityNode> nodes, Map<String, DDLInspector.TableMetadata> schema,
            List<MappingRule> rules, List<GlobalMappingRule> globalRules, String outputPath) throws Exception {
        ComparisonEngine engine = new ComparisonEngine();
        List<ComparisonEngine.Anomaly> anomalies = engine.compare(nodes, schema);

        // Run DDD Analysis
        DDDAnalyzer dddAnalyzer = new DDDAnalyzer();
        dddAnalyzer.analyze(nodes);

        List<MappingRule.Violation> violations = new ArrayList<>();

        // Check entity-specific rules
        for (EntityNode node : nodes) {
            for (MappingRule rule : rules) {
                violations.addAll(rule.check(node));
            }
        }

        // Check global rules
        for (GlobalMappingRule rule : globalRules) {
            violations.addAll(rule.checkAll(nodes));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("anomalies", anomalies);
        result.put("violations", violations);

        mapper.writeValue(new File(outputPath), result);
        System.out.println("Analysis report generated at: " + outputPath);
    }
}
