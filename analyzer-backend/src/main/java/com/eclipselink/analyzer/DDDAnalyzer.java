package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import javax.script.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DDDAnalyzer {

    private ScriptEngine engine;

    // Configurable thresholds (can be externalized to config file)
    private static final int REF_ENTITY_MIN_TOTAL_RELATIONS = 3;
    private static final double REF_ENTITY_IN_OUT_RATIO = 1.5;
    private static final int REF_ENTITY_MAX_ATTRIBUTES = 5;
    private static final int ROOT_MIN_OUT_DEGREE = 3;

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

        // 1. Identify Roles using JS if available, otherwise native
        // Also compute embedded entity status for each node
        Map<String, Boolean> embeddedStatus = new HashMap<>();
        for (EntityNode node : nodes) {
            embeddedStatus.put(node.getName(), isEmbeddedEntity(node, nodes));
        }

        // Store embedded status in node metadata for frontend use
        for (EntityNode node : nodes) {
            if (embeddedStatus.getOrDefault(node.getName(), false)) {
                // Find which entity embeds this one
                String embeddedBy = findEmbeddingOwner(node, nodes);
                node.setAggregateName(embeddedBy != null ? embeddedBy : node.getAggregateName());
            }
        }

        for (EntityNode node : nodes) {
            String role = "ENTITY";
            try {
                if (engine != null && engine.get("DDDRules") != null) {
                    Invocable inv = (Invocable) engine;
                    Object result = inv.invokeMethod(engine.get("DDDRules"), "identifyRole", node, nodes);
                    role = result.toString();
                } else {
                    role = identifyRoleNative(node, nodes, embeddedStatus);
                }
            } catch (Exception e) {
                role = identifyRoleNative(node, nodes, embeddedStatus);
            }
            node.setDddRole(role);
        }

        // 2. Cluster into Aggregates using improved logic
        clusterIntoAggregates(nodes, nodeMap);
    }

    /**
     * Check if an entity is embedded (has incoming Embedded relationship)
     */
    private boolean isEmbeddedEntity(EntityNode node, List<EntityNode> allNodes) {
        // EMBEDDABLE type is already handled
        if ("EMBEDDABLE".equals(node.getType())) {
            return true;
        }

        // Check for incoming Embedded relationships
        for (EntityNode other : allNodes) {
            if (other == node)
                continue;
            if (other.getRelationships() != null) {
                for (RelationshipMetadata rel : other.getRelationships()) {
                    if (rel.getTargetEntity().equals(node.getName()) &&
                            "Embedded".equals(rel.getMappingType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find which entity embeds the given node
     */
    private String findEmbeddingOwner(EntityNode node, List<EntityNode> allNodes) {
        for (EntityNode other : allNodes) {
            if (other == node)
                continue;
            if (other.getRelationships() != null) {
                for (RelationshipMetadata rel : other.getRelationships()) {
                    if (rel.getTargetEntity().equals(node.getName()) &&
                            "Embedded".equals(rel.getMappingType())) {
                        return other.getName();
                    }
                }
            }
        }
        return null;
    }

    private String identifyRoleNative(EntityNode node, List<EntityNode> allNodes, Map<String, Boolean> embeddedStatus) {
        if ("EMBEDDABLE".equals(node.getType())) {
            return "VALUE_OBJECT";
        }
        if ("ABSTRACT_ENTITY".equals(node.getType()) ||
                "MAPPED_SUPERCLASS".equals(node.getType())) {
            return "ENTITY";
        }

        // If already identified as embedded, it cannot be an aggregate root
        if (embeddedStatus.getOrDefault(node.getName(), false)) {
            return "ENTITY";
        }

        // Calculate metrics
        int incomingRelations = 0;
        int outgoingRelations = node.getRelationships() != null ? node.getRelationships().size() : 0;
        boolean hasCollections = false;
        boolean hasEmbeddedRelations = false;
        long cascadeCount = 0;
        boolean isStronglyOwned = false;

        // Analyze outgoing relationships
        if (node.getRelationships() != null) {
            for (RelationshipMetadata rel : node.getRelationships()) {
                String mappingType = rel.getMappingType();

                if (mappingType != null && (mappingType.contains("OneToMany") || mappingType.contains("ManyToMany"))) {
                    hasCollections = true;
                }

                if (mappingType != null && mappingType.equals("Embedded")) {
                    hasEmbeddedRelations = true;
                }

                if (rel.isCascadePersist() || rel.isCascadeRemove()) {
                    cascadeCount++;
                }
            }
        }

        // Analyze incoming relationships and ownership
        for (EntityNode other : allNodes) {
            if (other == node)
                continue;
            if (other.getRelationships() != null) {
                for (RelationshipMetadata rel : other.getRelationships()) {
                    if (rel.getTargetEntity().equals(node.getName())) {
                        incomingRelations++;

                        // Strong ownership: cascade OR privateOwned (EclipseLink composition marker)
                        if (rel.isCascadePersist() || rel.isCascadeRemove() || rel.isPrivateOwned()) {
                            isStronglyOwned = true;
                            break;
                        }
                    }
                }
            }
            if (isStronglyOwned)
                break;
        }

        int totalRelations = incomingRelations + outgoingRelations;
        int attributeCount = node.getAttributes() != null ? node.getAttributes().size() : 0;

        // Reference entity heuristic
        // Allow all entity types except EMBEDDABLE
        if (!"EMBEDDABLE".equals(node.getType()) &&
                totalRelations >= REF_ENTITY_MIN_TOTAL_RELATIONS &&
                incomingRelations > outgoingRelations * REF_ENTITY_IN_OUT_RATIO &&
                attributeCount <= REF_ENTITY_MAX_ATTRIBUTES &&
                !hasCollections) {

            return "REFERENCE_ENTITY";
        }

        long outDegree = outgoingRelations;

        // Aggregate Root heuristic
        if (!isStronglyOwned && (cascadeCount > 0 || outDegree >= ROOT_MIN_OUT_DEGREE)) {
            return "AGGREGATE_ROOT";
        }

        return "ENTITY";
    }

    private void clusterIntoAggregates(List<EntityNode> nodes, Map<String, EntityNode> nodeMap) {
        // 1. Initial package-based clustering (as fallback)
        for (EntityNode node : nodes) {
            if (node.getAggregateName() == null || "Default".equals(node.getAggregateName())) {
                String pkg = node.getPackageName();
                if (pkg != null && pkg.contains(".")) {
                    String aggregateName = pkg.substring(pkg.lastIndexOf('.') + 1);
                    node.setAggregateName(aggregateName.substring(0, 1).toUpperCase() + aggregateName.substring(1));
                } else {
                    node.setAggregateName("General");
                }
            }
        }

        // 2. Identify aggregate roots
        List<EntityNode> roots = nodes.stream()
                .filter(n -> "AGGREGATE_ROOT".equals(n.getDddRole()) && !isEmbeddedByRelation(n, nodeMap))
                .collect(Collectors.toList());

        // 3. Sort roots by out-degree to ensure they claim children correctly
        roots.sort((a, b) -> {
            int aOut = a.getRelationships() != null ? a.getRelationships().size() : 0;
            int bOut = b.getRelationships() != null ? b.getRelationships().size() : 0;
            return Integer.compare(bOut, aOut);
        });

        // 4. Propagate aggregate names through cascade relationships
        Set<String> processedRoots = new HashSet<>();
        for (EntityNode root : roots) {
            String aggName = root.getName(); // Aggregate named after its root
            if (!processedRoots.contains(aggName)) {
                propagateAggregate(root, aggName, nodeMap, new HashSet<>());
                processedRoots.add(aggName);
            }
        }
    }

    /**
     * Check if entity is embedded by an incoming relationship (not type-based
     * check)
     */
    private boolean isEmbeddedByRelation(EntityNode node, Map<String, EntityNode> nodeMap) {
        for (EntityNode other : nodeMap.values()) {
            if (other == node)
                continue;
            if (other.getRelationships() != null) {
                for (RelationshipMetadata rel : other.getRelationships()) {
                    if (rel.getTargetEntity().equals(node.getName())) {
                        // Check if this is an embedded/cascade relationship
                        if ("Embedded".equals(rel.getMappingType()) ||
                                rel.isCascadePersist() ||
                                rel.isCascadeRemove() ||
                                rel.isPrivateOwned()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void propagateAggregate(EntityNode parent, String aggName, Map<String, EntityNode> nodeMap,
            Set<String> visited) {
        if (visited.contains(parent.getName()))
            return;
        visited.add(parent.getName());

        // Set aggregate name on the parent
        parent.setAggregateName(aggName);

        if (parent.getRelationships() != null) {
            for (RelationshipMetadata rel : parent.getRelationships()) {
                EntityNode target = nodeMap.get(rel.getTargetEntity());
                if (target == null)
                    continue;

                // Focus on composition/ownership: cascade persist, cascade remove, privateOwned
                // (EclipseLink composition marker)
                // Embedded relationships also indicate ownership
                boolean isOwnership = "Embedded".equals(rel.getMappingType()) ||
                        rel.isCascadePersist() ||
                        rel.isCascadeRemove() ||
                        rel.isPrivateOwned();

                if (isOwnership) {
                    // Only propagate to non-roots or if explicitly cascading
                    // Embedded entities are always part of the aggregate
                    if (!isEmbeddedByRelation(target, nodeMap)) {
                        propagateAggregate(target, aggName, nodeMap, visited);
                    }
                }
            }
        }
    }
}
