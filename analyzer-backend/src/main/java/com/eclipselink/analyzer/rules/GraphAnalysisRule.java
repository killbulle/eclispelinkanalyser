package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.graph.GraphAnalyzer;
import com.eclipselink.analyzer.model.EntityNode;
import java.util.*;

public class GraphAnalysisRule implements GlobalMappingRule {
    @Override
    public String getId() {
        return "GRAPH_ANALYSIS";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Analyzes entity relationship graph for structural issues.";
    }

    @Override
    public List<MappingRule.Violation> checkAll(List<EntityNode> entities) {
        List<MappingRule.Violation> violations = new ArrayList<>();
        GraphAnalyzer graphAnalyzer = new GraphAnalyzer(entities);

        // Detect deep cycles (length >= 3)
        List<String> cycles = graphAnalyzer.detectDeepCycles();
        if (!cycles.isEmpty()) {
            for (String entityName : cycles) {
                violations.add(new MappingRule.Violation(getId(), "ERROR",
                        "Entity '" + entityName + "' is part of a deep cyclic dependency (length 3 or more). " +
                                "These cycles are structural issues that can cause problems with serialization and transaction boundaries. "
                                +
                                "Consider breaking the cycle by using ID references or @XmlIDREF."));
            }
        }

        // Detect self-references
        List<String> selfRefs = graphAnalyzer.findEntitiesWithSelfReferences();
        for (String entityName : selfRefs) {
            violations.add(new MappingRule.Violation(getId(), "INFO",
                    "Entity '" + entityName + "' has a self-referencing relationship. " +
                            "Ensure proper handling for graph traversal to avoid infinite loops."));
        }

        // Check for entities with many relationships (high fan-out)
        Map<String, Integer> outDegrees = graphAnalyzer.calculateOutDegrees();
        for (Map.Entry<String, Integer> entry : outDegrees.entrySet()) {
            if (entry.getValue() > 5) {
                violations.add(new MappingRule.Violation(getId(), "INFO",
                        "Entity '" + entry.getKey() + "' has " + entry.getValue() +
                                " outgoing relationships. High fan-out can lead to complex object graphs."));
            }
        }

        // Check for entities with many incoming relationships (high fan-in)
        Map<String, Integer> inDegrees = graphAnalyzer.calculateInDegrees();
        for (Map.Entry<String, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() > 3) {
                violations.add(new MappingRule.Violation(getId(), "INFO",
                        "Entity '" + entry.getKey() + "' is referenced by " + entry.getValue() +
                                " other entities. This entity may be a central hub in your data model."));
            }
        }

        // Detect strongly connected components
        List<Set<String>> components = graphAnalyzer.detectStronglyConnectedComponents();
        for (Set<String> component : components) {
            if (component.size() > 2) {
                for (String entityName : component) {
                    violations.add(new MappingRule.Violation(getId(), "WARNING",
                            "Entity '" + entityName + "' is part of a strongly connected component with " +
                                    component.size() + " entities. These entities are tightly coupled."));
                }
            }
        }

        // Find root entities (no incoming edges)
        List<String> roots = graphAnalyzer.findRootEntities();
        if (roots.isEmpty()) {
            violations.add(new MappingRule.Violation(getId(), "WARNING",
                    "No root entities found (all entities have incoming relationships). " +
                            "This may indicate a circular model with no clear entry points."));
        } else if (roots.size() > 5) {
            violations.add(new MappingRule.Violation(getId(), "INFO",
                    "Found " + roots.size() + " root entities. Many root entities may indicate " +
                            "a fragmented data model without clear aggregate roots."));
        }

        // Find leaf entities (no outgoing edges)
        List<String> leaves = graphAnalyzer.findLeafEntities();
        if (leaves.isEmpty()) {
            violations.add(new MappingRule.Violation(getId(), "INFO",
                    "No leaf entities found (all entities have outgoing relationships). " +
                            "This may indicate a highly interconnected model."));
        }

        // Analyze inheritance hierarchies
        Map<String, List<String>> hierarchies = graphAnalyzer.findInheritanceHierarchies();
        for (Map.Entry<String, List<String>> entry : hierarchies.entrySet()) {
            violations.add(new MappingRule.Violation(getId(), "INFO",
                    "Potential inheritance hierarchy detected with base class '" + entry.getKey() +
                            "' and " + entry.getValue().size() + " subclasses. Ensure proper @Inheritance strategy."));
        }

        return violations;
    }
}