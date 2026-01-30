/**
 * Unified DDD Heuristic Rules with Configurable Parameters and Confidence Scoring
 * 
 * This file is executed by both Java backend (via ScriptEngine) and TypeScript frontend.
 * All heuristics are parameterized for easy tuning.
 * 
 * Key Concepts:
 * - REFERENCE_ENTITY: Shared, read-only entities with high connectivity and few attributes (e.g., Location, Category)
 * - AGGREGATE_ROOT: Entities that control lifecycle of other entities via cascade relationships
 * - ENTITY: Regular domain entities
 * - VALUE_OBJECT: Immutable, embedded objects
 * - CUT-POINT: Relationships that cross aggregate boundaries from non-root entities (potential design issues)
 */

var DDDRules = {
    /**
     * Default configuration for heuristic parameters
     * All thresholds and weights can be tuned based on domain knowledge
     */
    defaultConfig: {
        // REFERENCE_ENTITY detection
        referenceEntity: {
            minTotalRelations: 3,           // Minimum total incoming+outgoing relationships
            incomingOutgoingRatio: 1.5,     // Incoming must be > outgoing * this ratio
            maxAttributes: 5,               // Maximum number of attributes
            allowCollections: false,        // Reference entities shouldn't have collection relationships
        },

        // AGGREGATE_ROOT detection  
        aggregateRoot: {
            cascadeWeight: 1.0,             // Weight for cascade relationships
            outDegreeWeight: 0.8,           // Weight for high out-degree
            cascadeThreshold: 1,            // Minimum cascade count to consider as root
            outDegreeThreshold: 3,          // Minimum out-degree to consider as root
            strongOwnershipPenalty: -1.0,   // Penalty if entity is strongly owned by another
        },

        // Confidence scoring
        confidence: {
            highThreshold: 0.7,             // Confidence ≥ 0.7 = high confidence
            mediumThreshold: 0.5,           // Confidence ≥ 0.5 = medium confidence
            lowThreshold: 0.3,              // Confidence < 0.3 = low confidence
        },

        // Cut-point detection
        cutPoint: {
            referenceEntityAllowed: false,  // Relationships to/from reference entities are NOT cut-points
            rootToRootAllowed: true,        // Aggregate roots can reference other roots
            entityToRootIsCutPoint: true,   // Non-root entity referencing another aggregate = cut-point
            relationshipWeights: {          // Weight factors for different relationship types
                ManyToOne: 0.5,
                OneToOne: 1.0,
                OneToMany: 1.5,
                ManyToMany: 2.0
            }
        }
    },

    /**
     * Merge user config with defaults
     */
    _getConfig: function (config) {
        var result = JSON.parse(JSON.stringify(this.defaultConfig));
        if (config) {
            // Deep merge (simple implementation for our structure)
            for (var key in config) {
                if (config.hasOwnProperty(key)) {
                    if (typeof config[key] === 'object' && !Array.isArray(config[key]) && config[key] !== null) {
                        // Ensure result[key] is an object if we want to deep merge
                        if (!result[key] || typeof result[key] !== 'object') {
                            result[key] = {};
                        }
                        for (var subKey in config[key]) {
                            if (config[key].hasOwnProperty(subKey)) {
                                result[key][subKey] = config[key][subKey];
                            }
                        }
                    } else {
                        result[key] = config[key];
                    }
                }
            }
        }
        return result;
    },

    /**
     * Analyze entity and return detailed role analysis with confidence scores
     * @param {Object} node - Entity node
     * @param {Array} allNodes - All nodes for context
     * @param {Object} userConfig - Optional configuration overrides
     * @returns {Object} - { role: string, confidence: number, scores: Object, details: Object }
     */
    analyzeRole: function (node, allNodes, userConfig) {
        var config = this._getConfig(userConfig);
        var data = node.data || node;
        var nodeName = data.name || data.className;

        // Quick classification for special types
        if (data.type === "EMBEDDABLE") {
            return {
                role: "VALUE_OBJECT",
                confidence: 0.95,
                scores: { valueObject: 0.95 },
                details: { reason: "Marked as EMBEDDABLE type" }
            };
        }

        if (data.type === "ABSTRACT_ENTITY" || data.type === "MAPPED_SUPERCLASS") {
            return {
                role: "ENTITY",
                confidence: 0.9,
                scores: { entity: 0.9 },
                details: { reason: "Abstract or mapped superclass" }
            };
        }

        // Calculate metrics
        var metrics = this._calculateMetrics(node, allNodes, config);
        var scores = this._calculateRoleScores(metrics, config);

        // Determine role based on scores
        var role = this._determineRole(scores, config);
        var confidence = this._calculateConfidence(scores, role, config);

        return {
            role: role,
            confidence: confidence,
            scores: scores,
            metrics: metrics,
            details: {
                totalRelations: metrics.totalRelations,
                incomingOutgoingRatio: metrics.incomingOutgoingRatio,
                attributeCount: metrics.attributeCount,
                hasCollections: metrics.hasCollections,
                isStronglyOwned: metrics.isStronglyOwned,
                cascadeCount: metrics.cascadeCount,
                outDegree: metrics.outDegree,
                isEmbedded: metrics.isEmbedded,
                embeddedBy: metrics.embeddedBy
            }
        };
    },

    /**
     * Calculate all metrics for a node
     */
    _calculateMetrics: function (node, allNodes, config) {
        var data = node.data || node;
        var nodeName = data.name || data.className;

        var incomingRelations = 0;
        var outgoingRelations = data.relationships ? data.relationships.length : 0;
        var hasCollections = false;
        var cascadeCount = 0;
        var isStronglyOwned = false;
        var isEmbedded = false;
        var embeddedBy = null;

        // Analyze outgoing relationships
        if (data.relationships) {
            for (var r = 0; r < data.relationships.length; r++) {
                var rel = data.relationships[r];
                if (rel.mappingType) {
                    if (rel.mappingType.includes('OneToMany') || rel.mappingType.includes('ManyToMany')) {
                        hasCollections = true;
                    }
                }
                if (rel.cascadePersist) {
                    cascadeCount++;
                }
            }
        }

        // Analyze incoming relationships and ownership
        for (var i = 0; i < allNodes.length; i++) {
            var other = allNodes[i];
            var otherData = other.data || other;
            if ((otherData.name || otherData.className) === nodeName) continue;

            var rels = otherData.relationships;
            if (rels) {
                for (var j = 0; j < rels.length; j++) {
                    var rel = rels[j];
                    if (rel.targetEntity === nodeName) {
                        incomingRelations++;
                        // Check for strong ownership
                        if (rel.cascadePersist || rel.cascadeRemove) {
                            isStronglyOwned = true;
                        }
                        // Check if entity is embedded (owned by another via Embedded mapping)
                        if (rel.mappingType && (rel.mappingType === "Embedded" || rel.mappingType.includes("Embedded"))) {
                            isEmbedded = true;
                            embeddedBy = otherData.name || otherData.className;
                        }
                    }
                }
            }
        }

        var totalRelations = incomingRelations + outgoingRelations;
        var incomingOutgoingRatio = outgoingRelations > 0 ? incomingRelations / outgoingRelations : Infinity;
        var attributeCount = Object.keys(data.attributes || {}).length;

        return {
            incomingRelations: incomingRelations,
            outgoingRelations: outgoingRelations,
            totalRelations: totalRelations,
            incomingOutgoingRatio: incomingOutgoingRatio,
            attributeCount: attributeCount,
            hasCollections: hasCollections,
            cascadeCount: cascadeCount,
            outDegree: outgoingRelations,
            isStronglyOwned: isStronglyOwned,
            isEmbedded: isEmbedded,
            embeddedBy: embeddedBy
        };
    },

    /**
     * Calculate role scores based on metrics
     */
    _calculateRoleScores: function (metrics, config) {
        var scores = {
            referenceEntity: 0,
            aggregateRoot: 0,
            entity: 0.5, // Default baseline
            valueObject: 0
        };

        // REFERENCE_ENTITY scoring
        var refConfig = config.referenceEntity;
        if (metrics.totalRelations >= refConfig.minTotalRelations &&
            metrics.incomingOutgoingRatio >= refConfig.incomingOutgoingRatio &&
            metrics.attributeCount <= refConfig.maxAttributes &&
            !metrics.hasCollections) {

            // Calculate normalized score (0-1)
            var connectivityScore = Math.min(metrics.totalRelations / 10, 1.0);
            var ratioScore = Math.min(metrics.incomingOutgoingRatio / 3, 1.0);
            var attributeScore = 1.0 - (metrics.attributeCount / refConfig.maxAttributes);

            // Bonus for pure reference entity (no outgoing relationships)
            var outgoingBonus = metrics.outgoingRelations === 0 ? 0.3 : 0;
            // Penalty if entity has cascade operations (reference entities shouldn't)
            var cascadePenalty = metrics.cascadeCount > 0 ? -0.2 : 0;

            scores.referenceEntity = Math.max(0, Math.min(1,
                (connectivityScore * 0.3 + ratioScore * 0.5 + attributeScore * 0.2) +
                outgoingBonus + cascadePenalty
            ));
        }

        // EMBEDDED entity scoring - likely a VALUE_OBJECT
        if (metrics.isEmbedded) {
            scores.valueObject = Math.max(scores.valueObject, 0.8);
            scores.aggregateRoot = -1; // Strong penalty
            scores.entity = 0.3; // Lower entity score
        }

        // AGGREGATE_ROOT scoring
        var rootConfig = config.aggregateRoot;
        if (!metrics.isStronglyOwned) {
            var cascadeScore = Math.min(metrics.cascadeCount / 3, 1.0) * rootConfig.cascadeWeight;
            var outDegreeScore = Math.min(metrics.outDegree / 8, 1.0) * rootConfig.outDegreeWeight;

            scores.aggregateRoot = cascadeScore + outDegreeScore;

            // Bonus if meets thresholds
            if (metrics.cascadeCount >= rootConfig.cascadeThreshold ||
                metrics.outDegree >= rootConfig.outDegreeThreshold) {
                scores.aggregateRoot += 0.3;
            }
        } else {
            scores.aggregateRoot = rootConfig.strongOwnershipPenalty;
        }

        // Normalize scores to 0-1 range
        for (var key in scores) {
            if (scores.hasOwnProperty(key)) {
                scores[key] = Math.max(0, Math.min(1, scores[key]));
            }
        }

        return scores;
    },

    /**
     * Determine role based on scores
     */
    _determineRole: function (scores, config) {
        // Check REFERENCE_ENTITY first (highest specificity)
        if (scores.referenceEntity >= config.confidence.highThreshold) {
            return "REFERENCE_ENTITY";
        }

        // Check VALUE_OBJECT
        if (scores.valueObject >= config.confidence.highThreshold) {
            return "VALUE_OBJECT";
        }

        // Check AGGREGATE_ROOT
        if (scores.aggregateRoot >= config.confidence.mediumThreshold) {
            return "AGGREGATE_ROOT";
        }

        // Default to ENTITY
        return "ENTITY";
    },

    /**
     * Calculate overall confidence for the determined role
     */
    _calculateConfidence: function (scores, role, config) {
        // Map role to score key (score keys use camelCase: referenceEntity, aggregateRoot, entity, valueObject)
        var scoreKey = role === "REFERENCE_ENTITY" ? "referenceEntity" :
            role === "AGGREGATE_ROOT" ? "aggregateRoot" :
                role === "ENTITY" ? "entity" :
                    role === "VALUE_OBJECT" ? "valueObject" : role.toLowerCase();

        var roleScore = scores[scoreKey] || 0;
        var nextBestScore = 0;

        // Find next best score among other roles
        for (var key in scores) {
            if (scores.hasOwnProperty(key) && key !== scoreKey) {
                nextBestScore = Math.max(nextBestScore, scores[key]);
            }
        }

        // Confidence is based on margin between role score and next best
        var margin = roleScore - nextBestScore;
        var confidence = roleScore * (1 + margin);

        return Math.max(0, Math.min(1, confidence));
    },

    /**
     * Backward-compatible identifyRole method
     */
    identifyRole: function (node, allNodes, config) {
        var analysis = this.analyzeRole(node, allNodes, config);
        return analysis.role;
    },

    /**
     * Get aggregate name with confidence
     */
    getAggregateNameWithConfidence: function (node, allNodes, config) {
        var data = node.data || node;
        config = this._getConfig(config);

        // If aggregateName is already set by Java analyzer, use it with high confidence
        if (data.aggregateName) {
            var roleAnalysis = this.analyzeRole(node, allNodes, config);
            var confidence = roleAnalysis.role === "AGGREGATE_ROOT" ?
                roleAnalysis.confidence : Math.max(0.3, roleAnalysis.confidence - 0.2);

            return {
                name: data.aggregateName,
                confidence: confidence,
                source: "javaAnalyzer"
            };
        }

        // Determine if node is a root
        var roleAnalysis = this.analyzeRole(node, allNodes, config);
        if (roleAnalysis.role === "AGGREGATE_ROOT") {
            return {
                name: data.name || data.className,
                confidence: roleAnalysis.confidence,
                source: "heuristic"
            };
        }

        // Find owning root via cascade relationships
        var nodeName = data.name || data.className;
        var bestCandidate = { name: "General", confidence: 0.1, source: "default" };

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
                    var hasCascade = rel.cascadePersist || rel.cascadeRemove || rel.cascadeAll ||
                        (rel.cascade && (rel.cascade.indexOf('ALL') !== -1 || rel.cascade.indexOf('PERSIST') !== -1));

                    if (hasCascade) {
                        var candidateConfidence = 0.7; // Base confidence for cascade ownership
                        if (rel.cascadeAll || (rel.cascadePersist && rel.cascadeRemove)) {
                            candidateConfidence = 0.9;
                        }

                        if (candidateConfidence > bestCandidate.confidence) {
                            bestCandidate = {
                                name: otherName,
                                confidence: candidateConfidence,
                                source: "cascadeRelationship"
                            };
                        }
                    }
                }
            }
        }

        return bestCandidate;
    },

    /**
     * Backward-compatible getAggregateName
     */
    getAggregateName: function (node, allNodes, config) {
        var result = this.getAggregateNameWithConfidence(node, allNodes, config);
        return result.name;
    },

    /**
     * Enhanced cut-point detection with confidence
     */
    analyzeCutPoint: function (edge, sourceNode, targetNode, allNodes, config) {
        config = this._getConfig(config);
        var cutConfig = config.cutPoint;

        var sourceAnalysis = this.analyzeRole(sourceNode, allNodes, config);
        var targetAnalysis = this.analyzeRole(targetNode, allNodes, config);

        // 1. Reference entity check
        if (!cutConfig.referenceEntityAllowed &&
            (sourceAnalysis.role === "REFERENCE_ENTITY" ||
                targetAnalysis.role === "REFERENCE_ENTITY")) {
            return {
                isCutPoint: false,
                confidence: 0.9,
                reason: "Involves reference entity",
                details: {
                    sourceRole: sourceAnalysis.role,
                    targetRole: targetAnalysis.role
                }
            };
        }

        // 2. Get aggregate names
        var sourceAgg = this.getAggregateNameWithConfidence(sourceNode, allNodes, config);
        var targetAgg = this.getAggregateNameWithConfidence(targetNode, allNodes, config);

        // 3. Same aggregate check
        if (sourceAgg.name === targetAgg.name && sourceAgg.name !== "General") {
            return {
                isCutPoint: false,
                confidence: Math.min(sourceAgg.confidence, targetAgg.confidence),
                reason: "Same aggregate: " + sourceAgg.name,
                details: {
                    sourceAggregate: sourceAgg,
                    targetAggregate: targetAgg
                }
            };
        }

        // 4. Different aggregates: apply DDD logic
        var sourceRole = sourceAnalysis.role;
        var isCutPoint = false;
        var reason = "";
        var confidence = Math.min(
            sourceAnalysis.confidence,
            targetAnalysis.confidence,
            sourceAgg.confidence,
            targetAgg.confidence
        );

        if (sourceRole === "AGGREGATE_ROOT" && cutConfig.rootToRootAllowed) {
            isCutPoint = false;
            reason = "Aggregate root referencing another aggregate (allowed)";
            confidence *= 0.9; // Slightly lower confidence for root-to-root
        } else if (sourceRole !== "AGGREGATE_ROOT" && cutConfig.entityToRootIsCutPoint) {
            isCutPoint = true;
            reason = "Non-root entity referencing another aggregate (potential issue)";
            confidence *= 1.1; // Higher confidence for problematic cases
        } else {
            isCutPoint = false;
            reason = "Edge case: " + sourceRole + " → " + targetAnalysis.role;
            confidence *= 0.7;
        }

        // Adjust confidence based on relationship type
        var relWeight = 1.0;
        if (edge.mappingType) {
            for (var relType in cutConfig.relationshipWeights) {
                if (edge.mappingType.includes(relType)) {
                    relWeight = cutConfig.relationshipWeights[relType];
                    break;
                }
            }
        }

        if (edge.cascadePersist || edge.cascadeRemove) {
            relWeight *= 1.5; // Cascade relationships are more significant
        }

        confidence = Math.max(0, Math.min(1, confidence * relWeight));

        return {
            isCutPoint: isCutPoint,
            confidence: confidence,
            reason: reason,
            details: {
                sourceRole: sourceAnalysis.role,
                targetRole: targetAnalysis.role,
                sourceAggregate: sourceAgg,
                targetAggregate: targetAgg,
                relationshipType: edge.mappingType,
                hasCascade: edge.cascadePersist || edge.cascadeRemove,
                weightFactor: relWeight
            }
        };
    },

    /**
     * Backward-compatible isCutPointEdge
     */
    isCutPointEdge: function (edge, sourceNode, targetNode, allNodes, config) {
        var analysis = this.analyzeCutPoint(edge, sourceNode, targetNode, allNodes, config);
        return analysis.isCutPoint;
    },

    /**
     * Calculate comprehensive cut-point score for an entity
     */
    getCutPointScore: function (node, allNodes, config) {
        var data = node.data || node;
        config = this._getConfig(config);

        if (!data.relationships) {
            return { score: 0, details: { reason: "No relationships" } };
        }

        var totalScore = 0;
        var connections = new Map();
        var cutPointAnalyses = [];

        for (var i = 0; i < data.relationships.length; i++) {
            var rel = data.relationships[i];
            var targetNode = allNodes.find(function (n) {
                var nd = n.data || n;
                return nd.name === rel.targetEntity || nd.className === rel.targetEntity;
            });

            if (!targetNode) continue;

            var analysis = this.analyzeCutPoint(rel, node, targetNode, allNodes, config);
            cutPointAnalyses.push(analysis);

            if (analysis.isCutPoint) {
                var weight = analysis.details.weightFactor || 1.0;
                var targetAgg = analysis.details.targetAggregate.name;
                connections.set(targetAgg, (connections.get(targetAgg) || 0) + weight * analysis.confidence);
            }
        }

        // Sum weighted scores from different aggregates
        connections.forEach(function (weight) {
            totalScore += weight;
        });

        // Normalize score (0-5 range for backward compatibility)
        var normalizedScore = Math.min(5, totalScore);

        return {
            score: normalizedScore,
            normalizedScore: normalizedScore / 5, // 0-1 range
            connections: Array.from(connections.entries()),
            analyses: cutPointAnalyses,
            details: {
                totalRelationships: data.relationships.length,
                cutPointRelationships: cutPointAnalyses.filter(a => a.isCutPoint).length,
                differentAggregates: connections.size
            }
        };
    },

    /**
     * Generate summary report for all nodes
     */
    generateAnalysisReport: function (nodes, config) {
        config = this._getConfig(config);
        var allNodes = nodes.map(function (n) { return { data: n }; });

        var report = {
            timestamp: new Date().toISOString(),
            config: config,
            summary: {
                totalNodes: nodes.length,
                roles: {},
                aggregates: {},
                cutPoints: []
            },
            nodes: []
        };

        // Analyze each node
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var nodeForAnalysis = { data: node };

            var roleAnalysis = this.analyzeRole(nodeForAnalysis, allNodes, config);
            var aggregateInfo = this.getAggregateNameWithConfidence(nodeForAnalysis, allNodes, config);
            var cutPointScore = this.getCutPointScore(nodeForAnalysis, allNodes, config);

            // Update summary counts
            report.summary.roles[roleAnalysis.role] = (report.summary.roles[roleAnalysis.role] || 0) + 1;
            report.summary.aggregates[aggregateInfo.name] = (report.summary.aggregates[aggregateInfo.name] || 0) + 1;

            if (cutPointScore.score > 1.0) {
                report.summary.cutPoints.push({
                    node: node.name || node.className,
                    score: cutPointScore.score,
                    role: roleAnalysis.role,
                    aggregate: aggregateInfo.name
                });
            }

            report.nodes.push({
                name: node.name || node.className,
                role: roleAnalysis.role,
                roleConfidence: roleAnalysis.confidence,
                aggregate: aggregateInfo.name,
                aggregateConfidence: aggregateInfo.confidence,
                aggregateSource: aggregateInfo.source,
                cutPointScore: cutPointScore.score,
                cutPointNormalized: cutPointScore.normalizedScore,
                metrics: roleAnalysis.details
            });
        }

        // Sort cut-points by score
        report.summary.cutPoints.sort(function (a, b) {
            return b.score - a.score;
        });

        return report;
    }
};

// Export for Node/TS if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DDDRules;
}

// ESM export for Vite (Java will strip this)
export default DDDRules;