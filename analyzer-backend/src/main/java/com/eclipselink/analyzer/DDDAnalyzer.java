package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.*;
import java.util.stream.Collectors;

public class DDDAnalyzer {

    public void analyze(List<EntityNode> nodes) {
        if (nodes == null || nodes.isEmpty())
            return;

        Map<String, EntityNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(EntityNode::getName, n -> n));

        // 1. Identify Roles based on JPA hints and connectivity
        for (EntityNode node : nodes) {
            // Initial guess: EMBEDDABLE is a VALUE_OBJECT
            if ("EMBEDDABLE".equals(node.getType())) {
                node.setDddRole("VALUE_OBJECT");
            } else {
                node.setDddRole("ENTITY");
            }
        }

        // 2. Identifying Aggregate Roots
        // A root is usually not owned by anyone (via cascading lifecycle) and controls
        // others
        for (EntityNode node : nodes) {
            if ("EMBEDDABLE".equals(node.getType()) || "ABSTRACT_ENTITY".equals(node.getType())
                    || "MAPPED_SUPERCLASS".equals(node.getType())) {
                continue;
            }

            boolean isStronglyOwned = false;
            for (EntityNode other : nodes) {
                if (other == node)
                    continue;
                if (other.getRelationships() != null) {
                    for (RelationshipMetadata rel : other.getRelationships()) {
                        if (rel.getTargetEntity().equals(node.getName())) {
                            // If someone has a relationship to us with cascade, we are likely NOT a root
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

            // Strong indicators for root:
            // - Not strongly owned by anyone else
            // - Has cascading relationships to others
            // - High number of outgoing relationships (centrality)
            long outDegree = node.getRelationships() != null ? node.getRelationships().size() : 0;
            long cascadeCount = node.getRelationships() != null
                    ? node.getRelationships().stream().filter(r -> r.isCascadePersist()).count()
                    : 0;

            if (!isStronglyOwned && (cascadeCount > 0 || outDegree > 3)) {
                node.setDddRole("AGGREGATE_ROOT");
            }
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
