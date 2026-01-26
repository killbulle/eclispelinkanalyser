/**
 * DDD Aggregate Heuristics Module
 * 
 * This module provides multiple strategies for detecting DDD aggregates
 * from JPA entity relationship graphs. Each heuristic can be tested independently.
 */

import type { Node, Edge } from 'reactflow';

export interface AggregateResult {
    aggregateName: string;
    isRoot: boolean;
    confidence: number; // 0-1
}

export type AggregateHeuristic = (
    node: Node,
    allNodes: Node[],
    edges: Edge[]
) => AggregateResult;

/**
 * Heuristic 1: Package-based clustering
 * Uses the last segment of the package name as aggregate name
 */
export const packageBasedHeuristic: AggregateHeuristic = (node, _allNodes, _edges) => {
    const pkg = node.data.packageName || '';
    const segments = pkg.split('.');
    const aggregateName = segments.length > 0 ? segments[segments.length - 1] || 'General' : 'General';

    return {
        aggregateName,
        isRoot: false,
        confidence: 0.3, // Low confidence - just based on naming
    };
};

/**
 * Heuristic 2: Cascade-based clustering
 * Entities with CASCADE relationships belong to the same aggregate.
 * The entity with most outgoing cascades is the aggregate root.
 */
export const cascadeBasedHeuristic: AggregateHeuristic = (node, allNodes, edges) => {
    const relationships = node.data.relationships || [];

    // Find relationships with cascade operations
    const cascadeRelations = relationships.filter((r: { cascade?: string[] }) =>
        r.cascade && r.cascade.length > 0 && !r.cascade.includes('NONE')
    );

    // Count outgoing cascade relationships
    const outgoingCascades = cascadeRelations.filter((r: { owningSide?: boolean }) => r.owningSide);

    // If this entity has many outgoing cascades, it's likely an aggregate root
    const isRoot = outgoingCascades.length >= 2;

    // Determine aggregate name: use entity name if root, or find parent via cascade
    if (isRoot) {
        return {
            aggregateName: node.data.name,
            isRoot: true,
            confidence: 0.7,
        };
    }

    // Look for incoming cascade relationship to find parent aggregate
    const incomingEdges = edges.filter(e => e.target === node.id);
    for (const edge of incomingEdges) {
        const sourceNode = allNodes.find(n => n.id === edge.source);
        if (sourceNode && edge.data?.cascade && edge.data.cascade.length > 0) {
            return {
                aggregateName: sourceNode.data.name,
                isRoot: false,
                confidence: 0.8,
            };
        }
    }

    // Fallback to package-based
    return packageBasedHeuristic(node, allNodes, edges);
};

/**
 * Heuristic 3: Connectivity-based clustering (In-degree analysis)
 * Entities with high in-degree (many entities pointing to them) are likely aggregate roots.
 * Other entities belong to the aggregate of their most connected neighbor.
 */
export const connectivityBasedHeuristic: AggregateHeuristic = (node, allNodes, edges) => {
    // Calculate in-degree for this node
    const inDegree = edges.filter(e => e.target === node.id).length;
    const outDegree = edges.filter(e => e.source === node.id).length;

    // High in-degree suggests this is a central entity (aggregate root candidate)
    const isRoot = inDegree >= 3 || (inDegree >= 2 && outDegree === 0);

    if (isRoot) {
        return {
            aggregateName: node.data.name,
            isRoot: true,
            confidence: 0.6 + (inDegree * 0.05), // Higher in-degree = higher confidence
        };
    }

    // Find the most connected neighbor to determine aggregate
    const outgoingEdges = edges.filter(e => e.source === node.id);
    let bestTarget: Node | null = null;
    let bestScore = 0;

    for (const edge of outgoingEdges) {
        const targetNode = allNodes.find(n => n.id === edge.target);
        if (targetNode) {
            const targetInDegree = edges.filter(e => e.target === targetNode.id).length;
            const score = targetInDegree + (edge.data?.owningSide ? 2 : 0);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = targetNode;
            }
        }
    }

    if (bestTarget) {
        return {
            aggregateName: bestTarget.data.name,
            isRoot: false,
            confidence: 0.5,
        };
    }

    // Fallback to package-based
    return packageBasedHeuristic(node, allNodes, edges);
};

/**
 * Heuristic 4: Combined heuristic (Recommended)
 * Combines cascade analysis + connectivity + package naming
 * Weighted scoring system
 */
export const combinedHeuristic: AggregateHeuristic = (node, allNodes, edges) => {
    const relationships = node.data.relationships || [];
    const inDegree = edges.filter(e => e.target === node.id).length;
    const outDegree = edges.filter(e => e.source === node.id).length;

    // Check for CASCADE relationships
    const hasCascade = relationships.some((r: { cascade?: string[] }) =>
        r.cascade && r.cascade.length > 0 &&
        (r.cascade.includes('ALL') || r.cascade.includes('PERSIST') || r.cascade.includes('MERGE'))
    );

    // Check for lifecycle control (OneToMany with cascade)
    const controlsLifecycle = relationships.filter((r: { mappingType: string; cascade?: string[] }) =>
        r.mappingType === 'OneToMany' && r.cascade && r.cascade.includes('ALL')
    ).length;

    // Root detection scoring
    let rootScore = 0;
    rootScore += inDegree * 0.3;               // More entities pointing = more central
    rootScore += controlsLifecycle * 0.5;       // Controls child lifecycles
    rootScore += hasCascade ? 0.2 : 0;          // Has cascade control
    rootScore -= outDegree * 0.1;               // Many outgoing = likely not root

    const isRoot = rootScore >= 0.8 || (inDegree >= 3 && controlsLifecycle >= 1);

    if (isRoot) {
        return {
            aggregateName: node.data.name,
            isRoot: true,
            confidence: Math.min(0.95, 0.5 + rootScore * 0.3),
        };
    }

    // Find parent aggregate via relationships
    const incomingEdges = edges.filter(e => e.target === node.id);
    let bestParent: Node | null = null;
    let bestParentScore = 0;

    for (const edge of incomingEdges) {
        const sourceNode = allNodes.find(n => n.id === edge.source);
        if (sourceNode) {
            const sourceRels = sourceNode.data.relationships || [];
            const sourceControlsThis = sourceRels.some((r: { targetEntity: string; cascade?: string[] }) =>
                r.targetEntity === node.data.name && r.cascade && r.cascade.includes('ALL')
            );

            let score = 0;
            score += sourceControlsThis ? 2 : 0;
            score += edges.filter(e => e.target === sourceNode.id).length * 0.2;

            if (score > bestParentScore) {
                bestParentScore = score;
                bestParent = sourceNode;
            }
        }
    }

    if (bestParent && bestParentScore >= 1) {
        return {
            aggregateName: bestParent.data.name,
            isRoot: false,
            confidence: Math.min(0.9, 0.4 + bestParentScore * 0.2),
        };
    }

    // Fallback to package-based with low confidence
    const pkg = node.data.packageName || '';
    const segments = pkg.split('.');
    return {
        aggregateName: segments.length > 1 ? segments[segments.length - 1] : 'General',
        isRoot: false,
        confidence: 0.2,
    };
};

/**
 * Apply a heuristic to all nodes and return aggregate assignments
 */
export const applyHeuristic = (
    nodes: Node[],
    edges: Edge[],
    heuristic: AggregateHeuristic = combinedHeuristic
): Map<string, AggregateResult> => {
    const results = new Map<string, AggregateResult>();

    nodes.forEach(node => {
        if (node.type === 'entityNode') {
            results.set(node.id, heuristic(node, nodes, edges));
        }
    });

    return results;
};

/**
 * Get aggregate name for a node using specified heuristic
 */
export const getAggregateForNode = (
    node: Node,
    allNodes: Node[],
    edges: Edge[],
    heuristic: AggregateHeuristic = combinedHeuristic
): string => {
    const result = heuristic(node, allNodes, edges);
    return result.aggregateName;
};

// Export all heuristics for testing
export const heuristics = {
    package: packageBasedHeuristic,
    cascade: cascadeBasedHeuristic,
    connectivity: connectivityBasedHeuristic,
    combined: combinedHeuristic,
};

export type HeuristicType = keyof typeof heuristics;
