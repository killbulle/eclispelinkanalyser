package com.eclipselink.analyzer.graph;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

public class GraphAnalyzer {

    private final List<EntityNode> entities;
    private final Graph<String, RelationshipEdge> graph;
    private final Map<String, EntityNode> entityMap;

    public GraphAnalyzer(List<EntityNode> entities) {
        this.entities = entities;
        this.entityMap = new HashMap<>();
        for (EntityNode entity : entities) {
            entityMap.put(entity.getName(), entity);
        }
        this.graph = new DefaultDirectedGraph<>(RelationshipEdge.class);
        buildGraph();
    }

    private void buildGraph() {
        // Add vertices for all entities
        for (EntityNode entity : entities) {
            graph.addVertex(entity.getName());
        }

        // Add edges for relationships
        for (EntityNode entity : entities) {
            if (entity.getRelationships() != null) {
                for (RelationshipMetadata rel : entity.getRelationships()) {
                    String source = entity.getName();
                    String target = rel.getTargetEntity();

                    // Check if target entity exists in our graph
                    if (graph.containsVertex(target)) {
                        RelationshipEdge edge = new RelationshipEdge(rel);
                        graph.addEdge(source, target, edge);
                    }
                }
            }
        }
    }

    public Graph<String, RelationshipEdge> getGraph() {
        return graph;
    }

    public List<String> detectCycles() {
        CycleDetector<String, RelationshipEdge> cycleDetector = new CycleDetector<>(graph);
        return new ArrayList<>(cycleDetector.findCycles());
    }

    public List<String> detectDeepCycles() {
        List<String> allCycleNodes = detectCycles();
        List<String> deepCycleNodes = new ArrayList<>();

        for (String node : allCycleNodes) {
            if (isNodeInDeepCycle(node, node, new HashSet<>(), 0)) {
                deepCycleNodes.add(node);
            }
        }
        return deepCycleNodes;
    }

    private boolean isNodeInDeepCycle(String current, String target, Set<String> visited, int depth) {
        if (depth > 0 && current.equals(target)) {
            return depth >= 3;
        }
        if (depth > 0 && visited.contains(current)) {
            return false;
        }

        visited.add(current);
        for (RelationshipEdge edge : graph.outgoingEdgesOf(current)) {
            String next = graph.getEdgeTarget(edge);
            if (isNodeInDeepCycle(next, target, visited, depth + 1)) {
                return true;
            }
        }
        visited.remove(current);
        return false;
    }

    public List<Set<String>> detectStronglyConnectedComponents() {
        StrongConnectivityAlgorithm<String, RelationshipEdge> scAlg = new KosarajuStrongConnectivityInspector<>(graph);
        return scAlg.stronglyConnectedSets();
    }

    public List<String> findEntitiesWithSelfReferences() {
        List<String> selfRefs = new ArrayList<>();
        for (EntityNode entity : entities) {
            if (entity.getRelationships() != null) {
                for (RelationshipMetadata rel : entity.getRelationships()) {
                    if (rel.getTargetEntity().equals(entity.getName())) {
                        selfRefs.add(entity.getName());
                        break;
                    }
                }
            }
        }
        return selfRefs;
    }

    public Map<String, Integer> calculateInDegrees() {
        Map<String, Integer> inDegrees = new HashMap<>();
        for (String vertex : graph.vertexSet()) {
            inDegrees.put(vertex, graph.inDegreeOf(vertex));
        }
        return inDegrees;
    }

    public Map<String, Integer> calculateOutDegrees() {
        Map<String, Integer> outDegrees = new HashMap<>();
        for (String vertex : graph.vertexSet()) {
            outDegrees.put(vertex, graph.outDegreeOf(vertex));
        }
        return outDegrees;
    }

    public List<String> findRootEntities() {
        Map<String, Integer> inDegrees = calculateInDegrees();
        return inDegrees.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> findLeafEntities() {
        Map<String, Integer> outDegrees = calculateOutDegrees();
        return outDegrees.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<List<String>> findPaths(String source, String target) {
        // Simple BFS for path finding (could be enhanced with more sophisticated
        // algorithms)
        List<List<String>> paths = new ArrayList<>();
        Queue<List<String>> queue = new LinkedList<>();
        queue.add(Arrays.asList(source));

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String last = path.get(path.size() - 1);

            if (last.equals(target)) {
                paths.add(path);
            } else {
                Set<RelationshipEdge> outgoing = graph.outgoingEdgesOf(last);
                for (RelationshipEdge edge : outgoing) {
                    String next = graph.getEdgeTarget(edge);
                    if (!path.contains(next)) { // Avoid cycles
                        List<String> newPath = new ArrayList<>(path);
                        newPath.add(next);
                        queue.add(newPath);
                    }
                }
            }
        }
        return paths;
    }

    public Map<String, List<String>> findInheritanceHierarchies() {
        Map<String, List<String>> hierarchies = new HashMap<>();

        // Build inheritance map based on parentEntity field
        Map<String, List<String>> parentToChildren = new HashMap<>();

        for (EntityNode entity : entities) {
            String parent = entity.getParentEntity();
            if (parent != null && !parent.isEmpty()) {
                parentToChildren.computeIfAbsent(parent, k -> new ArrayList<>()).add(entity.getName());
            }
        }

        // Also detect by naming patterns for backward compatibility
        for (EntityNode entity : entities) {
            String name = entity.getName();
            if (name.toLowerCase().contains("project") ||
                    name.toLowerCase().contains("employee") ||
                    name.toLowerCase().contains("address")) {

                // Find related entities by name pattern
                List<String> related = entities.stream()
                        .map(EntityNode::getName)
                        .filter(other -> !other.equals(name) &&
                                (other.toLowerCase().contains(name.toLowerCase()) ||
                                        name.toLowerCase().contains(other.toLowerCase())))
                        .collect(Collectors.toList());

                if (!related.isEmpty()) {
                    hierarchies.put(name, related);
                }
            }
        }

        // Merge with parent-child relationships
        for (Map.Entry<String, List<String>> entry : parentToChildren.entrySet()) {
            String parent = entry.getKey();
            List<String> children = entry.getValue();

            // Check if parent exists in our entity set
            if (entityMap.containsKey(parent)) {
                hierarchies.put(parent, children);
            } else {
                // Parent might be external (not in our analysis scope)
                for (String child : children) {
                    hierarchies.computeIfAbsent(child, k -> new ArrayList<>()).add("(extends " + parent + ")");
                }
            }
        }

        return hierarchies;
    }

    public List<RelationshipEdge> getRelationshipsBetween(String entity1, String entity2) {
        Set<RelationshipEdge> edges = graph.getAllEdges(entity1, entity2);
        if (edges == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(edges);
    }

    public static class RelationshipEdge extends DefaultEdge {
        private final RelationshipMetadata metadata;

        public RelationshipEdge(RelationshipMetadata metadata) {
            this.metadata = metadata;
        }

        public RelationshipMetadata getMetadata() {
            return metadata;
        }

        @Override
        public String toString() {
            return metadata.getAttributeName() + " [" + metadata.getMappingType() + "]";
        }
    }
}