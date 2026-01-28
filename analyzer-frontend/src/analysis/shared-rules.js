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
    identifyRole: function (node, allNodes) {
        // Access data if it's a ReactFlow node, otherwise use raw object (Java)
        var data = node.data || node;

        if (data.type === "EMBEDDABLE") {
            return "VALUE_OBJECT";
        }

        if (data.type === "ABSTRACT_ENTITY" || data.type === "MAPPED_SUPERCLASS") {
            return "ENTITY";
        }

        // Strong ownership check
        var isStronglyOwned = false;
        var nodeName = data.name || data.className;

        for (var i = 0; i < allNodes.length; i++) {
            var other = allNodes[i];
            var otherData = other.data || other;
            if ((otherData.name || otherData.className) === nodeName) continue;

            var rels = otherData.relationships;
            if (rels) {
                for (var j = 0; j < rels.length; j++) {
                    var rel = rels[j];
                    if (rel.targetEntity === nodeName) {
                        if (rel.cascadePersist || rel.cascadeRemove) {
                            isStronglyOwned = true;
                            break;
                        }
                    }
                }
            }
            if (isStronglyOwned) break;
        }

        // Root indicators
        var outDegree = data.relationships ? data.relationships.length : 0;
        var cascadeCount = 0;
        if (data.relationships) {
            for (var k = 0; k < data.relationships.length; k++) {
                if (data.relationships[k].cascadePersist) cascadeCount++;
            }
        }

        if (!isStronglyOwned && (cascadeCount > 0 || outDegree > 3)) {
            return "AGGREGATE_ROOT";
        }

        return "ENTITY";
    },

    /**
     * Propagates aggregate names from roots to owned entities.
     * This replaces the recursive logic to be more JS-friendly if needed, 
     * but we'll stick to the logic that Java expects.
     */
    getAggregateName: function (node, nodeMap) {
        var data = node.data || node;
        if (data.dddRole === "AGGREGATE_ROOT") {
            return data.name || data.className;
        }
        return null;
    }
};

// Export for Node/TS if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DDDRules;
}

// ESM export for Vite (Java will strip this)
export default DDDRules;

