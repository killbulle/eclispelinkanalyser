/**
 * Topological Analysis Module
 * Calculates graph metrics: Edge Weights, Node Stability, Centrality.
 */

import type { Node, Edge } from 'reactflow';

export interface TopologicalMetrics {
    instability: number; // 0 (Stable) to 1 (Unstable)
    afferentCoupling: number; // Ca: Incoming dependencies
    efferentCoupling: number; // Ce: Outgoing dependencies
    degreeCentrality: number;
}

export interface EdgeWeight {
    weight: number; // 0 to 1+
    reasons: string[];
}

/**
 * Calculates weight for a single edge based on JPA properties.
 * Higher weight = Stronger coupling (Composition).
 */
export const calculateEdgeWeight = (edge: Edge): EdgeWeight => {
    let weight = 0.1; // Base existence weight
    const reasons: string[] = ['Base Link'];

    const data = edge.data || {};

    // 1. Lifecycle Coupling (Cascade / OrphanRemoval)
    if (data.cascade) {
        if (data.cascade.includes('ALL')) {
            weight += 0.5;
            reasons.push('Cascade ALL (+0.5)');
        } else if (data.cascade.includes('PERSIST') || data.cascade.includes('MERGE')) {
            weight += 0.2;
            reasons.push('Cascade Partial (+0.2)');
        }
    }

    // Note: orphanRemoval usually implies strong composition
    // (We assume the parser extracts this, if available in edge data)

    // 2. Performance Coupling (Fetch Type)
    if (data.isEager) {
        weight += 0.3;
        reasons.push('EAGER Fetch (+0.3)');
    }

    // 3. Structural Coupling (Mandatory)
    // If owning side and non-nullable (simplified assumption based on typical mapping)
    if (data.owningSide) {
        weight += 0.1;
        reasons.push('Owning Side (+0.1)');
    }

    // 4. Loose Coupling Penalty (ManyToMany)
    if (data.mappingType === 'ManyToMany') {
        weight -= 0.2;
        reasons.push('ManyToMany (-0.2)');
    }

    return { weight: Math.max(0, weight), reasons };
};

/**
 * Calculates topological metrics for all nodes in the graph.
 */
export const analyzeTopology = (nodes: Node[], edges: Edge[]): Map<string, TopologicalMetrics> => {
    const metrics = new Map<string, TopologicalMetrics>();

    // Initialize
    nodes.forEach(node => {
        metrics.set(node.id, {
            instability: 0,
            afferentCoupling: 0,
            efferentCoupling: 0,
            degreeCentrality: 0
        });
    });

    // Calculate Couplings
    edges.forEach(edge => {
        const sourceMetrics = metrics.get(edge.source);
        const targetMetrics = metrics.get(edge.target);

        if (sourceMetrics) {
            sourceMetrics.efferentCoupling++; // Dependency: Source -> Target
        }
        if (targetMetrics) {
            targetMetrics.afferentCoupling++;
        }
    });

    // Calculate Instability & Centrality
    metrics.forEach((m, id) => {
        // Instability I = Ce / (Ce + Ca)
        // I = 0 -> Stable (Independent, heavily used) -> Candidate VO / Core Entity
        // I = 1 -> Unstable (Dependent, not used) -> Candidate Service / Top-level
        const total = m.efferentCoupling + m.afferentCoupling;
        m.instability = total === 0 ? 0.5 : m.efferentCoupling / total;

        // Simple degree centrality
        m.degreeCentrality = total;
    });

    return metrics;
};
