/**
 * Shared DDD Heuristic Rules
 * This file is intended to be executed by both the Java backend (via ScriptEngine)
 * and the TypeScript frontend.
 */

var DDDRules = {
    /**
     * Determines the DDD role of a node.
     * @param {Object} node - The EntityNode object
     * @param {Array} allNodes - List of all nodes for context
     * @returns {string} - "AGGREGATE_ROOT", "ENTITY", or "VALUE_OBJECT"
     */
    identifyRole: function (node, allNodes, config) {
        // Access data if it's a ReactFlow node, otherwise use raw object (Java)
        var data = node.data || node;
        config = config || {};

        if (data.type === "EMBEDDABLE") {
            return "VALUE_OBJECT";
        }

        if (data.type === "ABSTRACT_ENTITY" || data.type === "MAPPED_SUPERCLASS") {
            return "ENTITY";
        }

        var nodeName = data.name || data.className;
        var rels = data.relationships || [];

        // 1. Structural Analysis (Topology)
        var isStronglyOwned = false;
        var inDegree = 0;

        for (var i = 0; i < allNodes.length; i++) {
            var other = allNodes[i];
            var otherData = other.data || other;
            var name = otherData.name || otherData.className;
            if (name === nodeName) continue;

            var otherRels = otherData.relationships;
            if (otherRels) {
                for (var j = 0; j < otherRels.length; j++) {
                    var rel = otherRels[j];
                    if (rel.targetEntity === nodeName) {
                        inDegree++;
                        // Strong ownership is generally OneToMany (parent->child) or OneToOne with cascade
                        var isComposition = rel.mappingType === 'OneToMany' || rel.mappingType === 'OneToOne';
                        if (isComposition && (rel.cascadePersist || rel.cascadeRemove || (rel.cascade && (rel.cascade.indexOf('ALL') !== -1 || rel.cascade.indexOf('PERSIST') !== -1)))) {
                            isStronglyOwned = true;
                        }
                    }
                }
            }
        }

        // 2. Weighting System
        var rootScore = 0;
        var outDegree = rels.length;

        // Topology Weights
        if (config.enableTopology !== false) {
            var controlsLifecycle = 0;
            var hasCascade = false;

            for (var k = 0; k < rels.length; k++) {
                var r = rels[k];
                var isStrongCascade = r.cascadeAll || (r.cascade && r.cascade.indexOf('ALL') !== -1) || (r.cascadePersist && r.cascadeRemove);

                if (isStrongCascade) {
                    controlsLifecycle++;
                }
                if (r.cascadePersist || isStrongCascade || (r.cascade && (r.cascade.indexOf('PERSIST') !== -1))) {
                    hasCascade = true;
                }
            }

            rootScore += (inDegree * 0.3);
            rootScore += (controlsLifecycle * 0.6);
            rootScore += (hasCascade ? 0.3 : 0);
            rootScore -= (outDegree * 0.1);

            // Entry Point Bonus: if nobody points to it, it's a very strong candidate for a root
            if (inDegree === 0 && outDegree > 0) {
                rootScore += 0.5;
            }
        }

        // Semantic Weights (if provided in config)
        if (config.enableSemantic !== false && config.semantic) {
            var s = config.semantic;
            if (s.stereotype === 'ENTITY') rootScore += (0.4 * s.confidence);
            else if (s.stereotype === 'RESOURCE') rootScore += (0.3 * s.confidence);
            else if (s.stereotype === 'VALUE_OBJECT') rootScore -= (0.5 * s.confidence);

            if (nodeName.toLowerCase().indexOf('root') !== -1 || nodeName.toLowerCase().indexOf('aggregator') !== -1) {
                rootScore += 0.5;
            }
        }

        // 3. Final Decision
        // Threshold: 0.8 (same as former combinedHeuristic)
        // Root if score is high OR it's an entry point with some control/cascades
        var isRoot = rootScore >= 0.8 || (inDegree === 0 && (controlsLifecycle > 1 || hasCascade)) || (inDegree >= 3 && !isStronglyOwned);

        if (isRoot && !isStronglyOwned) {
            return "AGGREGATE_ROOT";
        }

        return "ENTITY";
    },

    /**
     * Determines the aggregate name for a node by finding its owning root.
     * @param {Object} node - The EntityNode object
     * @param {Array} allNodes - List of all nodes for context
     * @param {Object} config - Analysis config (enableTopology, enableSemantic, etc.)
     * @returns {string} - The name of the aggregate root that owns this entity, or the node's own name if it's a root
     */
    getAggregateName: function (node, allNodes, config) {
        var data = node.data || node;
        var nodeName = data.name || data.className;
        config = config || {};

        // First, determine if this node is itself a root
        var role = this.identifyRole(node, allNodes, config);
        if (role === "AGGREGATE_ROOT") {
            return nodeName;
        }

        // Otherwise, find the root that "owns" this entity via cascade relationship
        var ownerRootName = "General";
        for (var i = 0; i < allNodes.length; i++) {
            var other = allNodes[i];
            var otherData = other.data || other;
            var otherName = otherData.name || otherData.className;
            if (otherName === nodeName) continue;

            var otherRole = this.identifyRole(other, allNodes, config);
            if (otherRole !== "AGGREGATE_ROOT") continue;

            var rels = otherData.relationships || [];
            for (var j = 0; j < rels.length; j++) {
                var rel = rels[j];
                if (rel.targetEntity === nodeName) {
                    // Check for ownership via cascade
                    var hasCascade = rel.cascadeAll ||
                        (rel.cascade && (rel.cascade.indexOf('ALL') !== -1 || rel.cascade.indexOf('PERSIST') !== -1)) ||
                        rel.cascadePersist;
                    if (hasCascade) {
                        ownerRootName = otherName;
                        break;
                    }
                }
            }
            if (ownerRootName !== "General") break;
        }

        return ownerRootName;
    }
};

// Export for Node/TS if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DDDRules;
}

// ESM export for Vite (Java will strip this)
export default DDDRules;

