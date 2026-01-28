/**
 * DDD Aggregate Heuristics Module
 * 
 * This module provides multiple strategies for detecting DDD aggregates
 * from JPA entity relationship graphs. Each heuristic can be tested independently.
 */

import type { Node, Edge } from 'reactflow';
import type { AnalysisConfig } from '../analysis/engine';
import { analyzeSemantics } from '../analysis/semantic';
// @ts-ignore
import DDDRules from '../analysis/shared-rules';

export interface AggregateResult {
    aggregateName: string;
    isRoot: boolean;
    confidence: number; // 0-1
}

export type AggregateHeuristic = (
    node: Node,
    allNodes: Node[],
    edges: Edge[],
    config?: AnalysisConfig
) => AggregateResult;


const safeNodes = (nodes: Node[]) => Array.isArray(nodes) ? nodes : [];

/**
 * Heuristic 1: Package-based clustering
 * Uses the last segment of the package name as aggregate name
 */
export const packageBasedHeuristic: AggregateHeuristic = (node, _allNodes, _edges, _config) => {
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
 * Heuristic 0: Server-side Ref (Source of Truth)
 * Simply uses the aggregate analysis already performed by the Java Backend.
 */
export const serverSideHeuristic: AggregateHeuristic = (node, _allNodes, _edges, _config) => {
    return {
        aggregateName: node.data.aggregateName || 'General',
        isRoot: node.data.dddRole === 'AGGREGATE_ROOT',
        confidence: 1.0, // This is the official server result
    };
};

/**
 * Heuristic 5: Shared Rules (JS Master)
 * Uses the exact same Javascript logic as the Java Backend.
 */
export const sharedHeuristic: AggregateHeuristic = (node, allNodes, _edges, config) => {
    const _nodes = safeNodes(allNodes);

    // Inject semantic analysis result into config for the shared rules
    const semanticConfig = {
        ...config,
        semantic: config?.enableSemantic !== false ? analyzeSemantics(node.data.name, config?.semanticProfile) : undefined
    };

    // DDDRules is the object from the shared JS file
    const role = DDDRules.identifyRole(node, _nodes, semanticConfig);
    const aggName = DDDRules.getAggregateName(node, _nodes, semanticConfig);

    return {
        aggregateName: aggName,
        isRoot: role === 'AGGREGATE_ROOT',
        confidence: 0.9,
    };
};

/**
 * Apply a heuristic to all nodes and return aggregate assignments
 */
export const applyHeuristic = (
    nodes: Node[],
    edges: Edge[],
    heuristic: AggregateHeuristic = sharedHeuristic,
    config?: AnalysisConfig
): Map<string, AggregateResult> => {
    const results = new Map<string, AggregateResult>();

    nodes.forEach(node => {
        if (node.type === 'entityNode') {
            results.set(node.id, heuristic(node, nodes, edges, config));
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
    heuristic: AggregateHeuristic = sharedHeuristic,
    config?: AnalysisConfig
): string => {
    const result = heuristic(node, allNodes, edges, config);
    return result.aggregateName;
};

// Export all functional heuristics
export const heuristics = {
    shared: sharedHeuristic,
    server: serverSideHeuristic,
    package: packageBasedHeuristic,
};

export type HeuristicType = keyof typeof heuristics;
