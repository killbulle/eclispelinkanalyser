/**
 * Advanced Domain Analysis Engine
 * Orchestrates Semantic and Topological analysis to produce DDD recommendations.
 */

import type { Node, Edge } from 'reactflow';
import { analyzeSemantics, type SemanticProfileType, Stereotype } from './semantic';
import { analyzeTopology, calculateEdgeWeight } from './topology';
import { heuristics, getAggregateForNode } from '../utils/aggregateHeuristics';

export interface AnalysisConfig {
    semanticProfile: SemanticProfileType;
    enableSemantic: boolean;
    enableTopology: boolean;
}

export interface AnalysisReport {
    timestamp: string;
    config: AnalysisConfig;
    aggregates: AggregateRecommendation[];
    valueObjects: VOCandidate[];
    cuts: CutRecommendation[];
}

export interface AggregateRecommendation {
    id: string; // usually the root name
    root: string;
    members: string[];
    confidence: number;
}

export interface VOCandidate {
    className: string;
    reason: string;
    recommendation: string;
    confidence: number;
}

export interface CutRecommendation {
    source: string;
    target: string;
    reason: string;
    action: string;
    weight: number;
}

/**
 * Runs the full analysis pipeline.
 */
export const runAnalysis = (
    nodes: Node[],
    edges: Edge[],
    config: AnalysisConfig
): AnalysisReport => {
    const report: AnalysisReport = {
        timestamp: new Date().toISOString(),
        config,
        aggregates: [],
        valueObjects: [],
        cuts: []
    };

    // 1. Topological Analysis
    const topoMetrics = analyzeTopology(nodes, edges);

    // 2. Identify Value Objects (Rule 1)
    const potentialVOs = new Set<string>();

    nodes.forEach(node => {
        if (node.type !== 'entityNode') return;

        const metrics = topoMetrics.get(node.id);
        const semantic = config.enableSemantic
            ? analyzeSemantics(node.data.name, config.semanticProfile)
            : { concept: 'Disabled', stereotype: Stereotype.UNKNOWN, confidence: 0, ontologySource: undefined };

        let reasons: string[] = [];
        let confidence = 0;

        // Topology Check: Leaf node (Ce=0) or only points to other VOs (simplified here to Ce=0 for now)
        // Also High Stability (I < 0.2)
        if (config.enableTopology && metrics) {
            if (metrics.efferentCoupling === 0) {
                reasons.push('Leaf Node (Topology)');
                confidence += 0.4;
            }
            if (metrics.instability < 0.1) {
                reasons.push('High Stability (I < 0.1)');
                confidence += 0.2;
            }
        }

        // Semantic Check
        if (config.enableSemantic && semantic.stereotype === Stereotype.VALUE_OBJECT) {
            reasons.push(`Semantic Match: ${semantic.concept} (${semantic.ontologySource || 'Generic'})`);
            confidence += 0.4 * semantic.confidence;
        }

        if (confidence >= 0.5) {
            potentialVOs.add(node.data.name);
            report.valueObjects.push({
                className: node.data.name,
                reason: reasons.join(' + '),
                recommendation: 'Consider @Embeddable or Value Object pattern',
                confidence
            });
        }
    });

    // 3. Identify Aggregates (Rule 2) using existing Heuristics + Enhancements
    // We use the 'combined' heuristic from our previous module as a baseline
    const nodeAggregates = new Map<string, string>(); // NodeId -> AggregateName

    nodes.filter(n => n.type === 'entityNode').forEach(n => {
        const aggName = getAggregateForNode(n, nodes, edges, heuristics.combined);
        nodeAggregates.set(n.id, aggName);
    });

    // Group by aggregate
    const aggGroups = new Map<string, Node[]>();
    nodeAggregates.forEach((aggName, nodeId) => {
        if (!aggGroups.has(aggName)) aggGroups.set(aggName, []);
        const node = nodes.find(n => n.id === nodeId);
        if (node) aggGroups.get(aggName)!.push(node);
    });

    aggGroups.forEach((members, aggName) => {
        // Find Root within cluster
        // Criteria: Highest Centrality + Semantic Root + Not a VO
        let bestRoot = members[0];
        let maxScore = -1;

        members.forEach(member => {
            if (potentialVOs.has(member.data.name)) return; // VOs shouldn't be roots

            let score = 0;
            const metrics = topoMetrics.get(member.id);

            // Topological Score
            if (metrics) {
                score += metrics.degreeCentrality; // Simple definition of centrality
                // Roots are entry points -> High Afferent from OUTSIDE cluster (ignored for simplicity here)
            }

            // Semantic Score
            if (config.enableSemantic) {
                const semantic = analyzeSemantics(member.data.name, config.semanticProfile);
                if (semantic.stereotype === Stereotype.ENTITY) score += 5;
                if (semantic.stereotype === Stereotype.ROLE) score += 3;
            }

            if (score > maxScore) {
                maxScore = score;
                bestRoot = member;
            }
        });

        report.aggregates.push({
            id: `agg-${aggName}`,
            root: bestRoot.data.name,
            members: members.map(m => m.data.name),
            confidence: 0.8 // Placeholder
        });
    });

    // 4. Cut Recommendations (Rule 3)
    edges.forEach(edge => {
        const source = nodes.find(n => n.id === edge.source);
        const target = nodes.find(n => n.id === edge.target);
        if (!source || !target) return;

        const sourceAgg = nodeAggregates.get(source.id);
        const targetAgg = nodeAggregates.get(target.id);

        // Check 1: Cross-Aggregate Boundary
        if (sourceAgg && targetAgg && sourceAgg !== targetAgg) {
            // It's a cut candidate
            const weight = calculateEdgeWeight(edge).weight;

            // If weight is weak, strong recommendation to cut
            if (weight < 0.3) {
                report.cuts.push({
                    source: source.data.name,
                    target: target.data.name,
                    reason: `Cross-Aggregate Boundary (${sourceAgg} -> ${targetAgg}) & Weak Link`,
                    action: 'Replace object reference with ID reference',
                    weight
                });
            }
        }

        // Check 2: Stability Violation (Target is less stable than Source)
        // A stable component (Domain) shouldn't depend on unstable (UI/Service).
        // In Entity world: A Core Entity shouldn't depend on a peripheral/specific one.
        const metricsSource = topoMetrics.get(source.id);
        const metricsTarget = topoMetrics.get(target.id);

        if (config.enableTopology && metricsSource && metricsTarget) {
            // If Source is Stable (I < 0.3) and Target is Unstable (I > 0.7)
            if (metricsSource.instability < 0.3 && metricsTarget.instability > 0.7) {
                report.cuts.push({
                    source: source.data.name,
                    target: target.data.name,
                    reason: `Stability Violation (Source I=${metricsSource.instability.toFixed(2)} depends on Unstable Target I=${metricsTarget.instability.toFixed(2)})`,
                    action: 'Invert dependency or use Event',
                    weight: 0 // Priority
                });
            }
        }
    });

    return report;
};
