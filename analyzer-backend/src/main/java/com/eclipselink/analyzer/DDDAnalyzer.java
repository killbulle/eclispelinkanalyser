package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import javax.script.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DDDAnalyzer {

    private ScriptEngine engine;

    public DDDAnalyzer() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
        loadRules();
    }

    private void loadRules() {
        try {
            // Try different paths to find the shared rules
            Path rulesPath = Paths.get("../shared/ddd-rules.js").toAbsolutePath();
            if (!Files.exists(rulesPath)) {
                rulesPath = Paths.get("shared/ddd-rules.js").toAbsolutePath();
            }

            if (Files.exists(rulesPath)) {
                String script = new String(Files.readAllBytes(rulesPath), java.nio.charset.StandardCharsets.UTF_8);
                // Strip ESM exports for Java ScriptEngine compatibility
                if (script.contains("export default")) {
                    script = script.substring(0, script.indexOf("export default"));
                }
                engine.eval(script);
            }
        } catch (Exception e) {
            System.err.println("Could not load shared JS rules: " + e.getMessage());
        }
    }

    public void analyze(List<EntityNode> nodes) {
        if (nodes == null || nodes.isEmpty())
            return;

        Map<String, EntityNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(EntityNode::getName, n -> n));

        // 1 & 2. Identify Roles using JS if possible, otherwise native
        for (EntityNode node : nodes) {
            String role = "ENTITY";
            try {
                if (engine != null && engine.get("DDDRules") != null) {
                    Invocable inv = (Invocable) engine;
                    Object result = inv.invokeMethod(engine.get("DDDRules"), "identifyRole", node, nodes);
                    role = result.toString();
                } else {
                    role = identifyRoleNative(node, nodes);
                }
            } catch (Exception e) {
                role = identifyRoleNative(node, nodes);
            }
            node.setDddRole(role);
        }

        // 3. Cluster into Aggregates
        // Default: package-based
        for (EntityNode node : nodes) {
            String pkg = node.getPackageName();
            if (pkg != null && pkg.contains(".")) {
                String aggregateName = pkg.substring(pkg.lastIndexOf('.') + 1);
                node.setAggregateName(aggregateName.substring(0, 1).toUpperCase() + aggregateName.substring(1));
            } else {
                node.setAggregateName("Default");
            }
        }

        // Refining aggregates: if Entity A is owned by Root R, it joins R's aggregate
        List<EntityNode> roots = nodes.stream().filter(n -> "AGGREGATE_ROOT".equals(n.getDddRole()))
                .collect(Collectors.toList());

        // Sort roots by "importance" (out-degree) to ensure they claim children
        // correctly
        roots.sort((a, b) -> {
            int aOut = a.getRelationships() != null ? a.getRelationships().size() : 0;
            int bOut = b.getRelationships() != null ? b.getRelationships().size() : 0;
            return Integer.compare(bOut, aOut);
        });

        for (EntityNode root : roots) {
            String aggName = root.getName(); // Simple: Aggregate named after its root
            propagateAggregate(root, aggName, nodeMap, new HashSet<>());
        }
    }

    private String identifyRoleNative(EntityNode node, List<EntityNode> allNodes) {
        if ("EMBEDDABLE".equals(node.getType()))
            return "VALUE_OBJECT";
        if ("ABSTRACT_ENTITY".equals(node.getType()) || "MAPPED_SUPERCLASS".equals(node.getType()))
            return "ENTITY";

        // Calculate incoming and outgoing relations
        int incomingRelations = 0;
        int outgoingRelations = node.getRelationships() != null ? node.getRelationships().size() : 0;
        boolean hasCollections = false;
        
        // Check for collection relationships (OneToMany, ManyToMany)
        if (node.getRelationships() != null) {
            for (RelationshipMetadata rel : node.getRelationships()) {
                String mappingType = rel.getMappingType();
                if (mappingType != null && (mappingType.contains("OneToMany") || mappingType.contains("ManyToMany"))) {
                    hasCollections = true;
                    break;
                }
            }
        }
        
        // Count incoming relations
        for (EntityNode other : allNodes) {
            if (other == node) continue;
            if (other.getRelationships() != null) {
                for (RelationshipMetadata rel : other.getRelationships()) {
                    if (rel.getTargetEntity().equals(node.getName())) {
                        incomingRelations++;
                    }
                }
            }
        }
        
        int totalRelations = incomingRelations + outgoingRelations;
        int attributeCount = node.getAttributes() != null ? node.getAttributes().size() : 0;
        
        // Reference entity heuristic: highly connected, few attributes, no collections, more incoming than outgoing
        if (totalRelations > 3 && 
            incomingRelations > outgoingRelations * 1.5 && 
            attributeCount < 5 && 
            !hasCollections &&
            "ENTITY".equals(node.getType())) {
            return "REFERENCE_ENTITY";
        }

        // Strong ownership check
        boolean isStronglyOwned = false;
        for (EntityNode other : allNodes) {
            if (other == node)
                continue;
            if (other.getRelationships() != null) {
                for (RelationshipMetadata rel : other.getRelationships()) {
                    if (rel.getTargetEntity().equals(node.getName())) {
                        if (rel.isCascadePersist() || rel.isCascadeRemove()) {
                            isStronglyOwned = true;
                            break;
                        }
                    }
                }
            }
            if (isStronglyOwned)
                break;
        }

        long outDegree = outgoingRelations;
        long cascadeCount = node.getRelationships() != null
                ? node.getRelationships().stream().filter(r -> r.isCascadePersist()).count()
                : 0;

        if (!isStronglyOwned && (cascadeCount > 0 || outDegree > 3)) {
            return "AGGREGATE_ROOT";
        }
        return "ENTITY";
    }

    private void propagateAggregate(EntityNode parent, String aggName, Map<String, EntityNode> nodeMap,
            Set<String> visited) {
        if (visited.contains(parent.getName()))
            return;
        visited.add(parent.getName());

        parent.setAggregateName(aggName);

        if (parent.getRelationships() != null) {
            for (RelationshipMetadata rel : parent.getRelationships()) {
                // Focus on composition/ownership: cascade persist OR not lazy (Eager loading
                // often implies tight coupling)
                if (rel.isCascadePersist() || !rel.isLazy()) {
                    EntityNode child = nodeMap.get(rel.getTargetEntity());
                    // Only propagate to non-roots (don't steal children of other roots unless they
                    // are specifically cascading)
                    if (child != null && (!"AGGREGATE_ROOT".equals(child.getDddRole()) || rel.isCascadePersist())) {
                        propagateAggregate(child, aggName, nodeMap, visited);
                    }
                }
            }
        }
    }
}
