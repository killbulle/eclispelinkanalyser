import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import vm from 'vm';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load unified DDD rules
const sharedRulesPath = path.join(__dirname, 'src/analysis/shared-rules.js');
let sharedRulesContent = fs.readFileSync(sharedRulesPath, 'utf8');
sharedRulesContent = sharedRulesContent.replace(/export default DDDRules;/, '');
const context = { module: { exports: {} }, exports: {}, console };
vm.createContext(context);
vm.runInContext(sharedRulesContent, context);
const DDDRules = context.DDDRules || context.module.exports;

// Load agent-report.json
const reportPath = path.join(__dirname, 'public/agent-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
const nodesList = report.nodes;

console.log('=== UPDATING DDD HEURISTICS WITH CONFIDENCE SCORING ===\n');
console.log(`Processing ${nodesList.length} entities...\n`);

// Prepare all nodes for analysis
const allNodesForRules = nodesList.map(n => ({ data: n }));

// Configuration (can be tuned)
const config = {
    referenceEntity: {
        minTotalRelations: 3,
        incomingOutgoingRatio: 1.5,
        maxAttributes: 5,
        allowCollections: false,
    },
    aggregateRoot: {
        cascadeWeight: 1.0,
        outDegreeWeight: 0.8,
        cascadeThreshold: 1,
        outDegreeThreshold: 3,
        strongOwnershipPenalty: -1.0,
    },
    confidence: {
        highThreshold: 0.8,
        mediumThreshold: 0.5,
        lowThreshold: 0.3,
    }
};

// Statistics
const stats = {
    roles: {},
    aggregateSources: {},
    confidenceLevels: { high: 0, medium: 0, low: 0 },
    cutPoints: []
};

// Update each node
const updatedNodes = nodesList.map(node => {
    const nodeForAnalysis = { data: node };
    
    // Analyze role with confidence
    const roleAnalysis = DDDRules.analyzeRole(nodeForAnalysis, allNodesForRules, config);
    const aggregateInfo = DDDRules.getAggregateNameWithConfidence(nodeForAnalysis, allNodesForRules, config);
    
    // Update node fields
    const updatedNode = { ...node };
    updatedNode.dddRole = roleAnalysis.role;
    updatedNode.aggregateName = aggregateInfo.name;
    
    // Add confidence fields (new)
    updatedNode.dddRoleConfidence = roleAnalysis.confidence;
    updatedNode.aggregateNameConfidence = aggregateInfo.confidence;
    updatedNode.aggregateNameSource = aggregateInfo.source;
    
    // Add metrics for debugging
    updatedNode.metrics = roleAnalysis.details;
    
    // Calculate cut-point score
    const cutPointScore = DDDRules.getCutPointScore(nodeForAnalysis, allNodesForRules, config);
    updatedNode.cutPointScore = cutPointScore.score;
    updatedNode.cutPointNormalized = cutPointScore.normalizedScore;
    
    // Update statistics
    stats.roles[roleAnalysis.role] = (stats.roles[roleAnalysis.role] || 0) + 1;
    stats.aggregateSources[aggregateInfo.source] = (stats.aggregateSources[aggregateInfo.source] || 0) + 1;
    
    if (roleAnalysis.confidence >= config.confidence.highThreshold) {
        stats.confidenceLevels.high++;
    } else if (roleAnalysis.confidence >= config.confidence.mediumThreshold) {
        stats.confidenceLevels.medium++;
    } else {
        stats.confidenceLevels.low++;
    }
    
    if (cutPointScore.score > 1.0) {
        stats.cutPoints.push({
            node: node.name,
            score: cutPointScore.score,
            role: roleAnalysis.role,
            aggregate: aggregateInfo.name
        });
    }
    
    return updatedNode;
});

// Generate summary report
const summaryReport = DDDRules.generateAnalysisReport(updatedNodes, config);

// Create updated report
const updatedReport = {
    ...report,
    nodes: updatedNodes,
    analysisMetadata: {
        timestamp: new Date().toISOString(),
        heuristicVersion: "2.0",
        config: config,
        summary: summaryReport.summary
    }
};

// Save updated report
const outputPath = path.join(__dirname, 'public/agent-report-improved.json');
fs.writeFileSync(outputPath, JSON.stringify(updatedReport, null, 2));
console.log(`\nUpdated report saved to: ${outputPath}`);

// Print summary
console.log('\n=== ANALYSIS SUMMARY ===');
console.log(`Total entities: ${updatedNodes.length}`);
console.log('\nRoles:');
Object.entries(stats.roles).forEach(([role, count]) => {
    console.log(`  ${role}: ${count}`);
});

console.log('\nAggregate Sources:');
Object.entries(stats.aggregateSources).forEach(([source, count]) => {
    console.log(`  ${source}: ${count}`);
});

console.log('\nConfidence Levels:');
console.log(`  High (≥${config.confidence.highThreshold}): ${stats.confidenceLevels.high}`);
console.log(`  Medium (≥${config.confidence.mediumThreshold}): ${stats.confidenceLevels.medium}`);
console.log(`  Low (<${config.confidence.mediumThreshold}): ${stats.confidenceLevels.low}`);

console.log('\nCut-points (score > 1.0):');
if (stats.cutPoints.length === 0) {
    console.log('  None');
} else {
    stats.cutPoints.sort((a, b) => b.score - a.score);
    stats.cutPoints.forEach(cp => {
        console.log(`  ${cp.node}: ${cp.score.toFixed(2)} (${cp.role}, agg:${cp.aggregate})`);
    });
}

// Compare with previous results
console.log('\n=== COMPARISON WITH PREVIOUS ANALYSIS ===');
const previousRoles = {};
nodesList.forEach(node => {
    previousRoles[node.dddRole] = (previousRoles[node.dddRole] || 0) + 1;
});

console.log('Previous roles:');
Object.entries(previousRoles).forEach(([role, count]) => {
    console.log(`  ${role}: ${count}`);
});

console.log('\nChanges:');
updatedNodes.forEach((node, idx) => {
    const original = nodesList[idx];
    if (original.dddRole !== node.dddRole || original.aggregateName !== node.aggregateName) {
        console.log(`  ${node.name}:`);
        console.log(`    Role: ${original.dddRole} → ${node.dddRole} (conf: ${node.dddRoleConfidence.toFixed(2)})`);
        console.log(`    Aggregate: ${original.aggregateName} → ${node.aggregateName} (conf: ${node.aggregateNameConfidence.toFixed(2)})`);
    }
});

console.log('\n=== KEY IMPROVEMENTS ===');
console.log('1. REFERENCE_ENTITY detection: Location should be identified as reference entity');
console.log('2. Confidence scoring: Each classification has a confidence level');
console.log('3. Configurable heuristics: All thresholds and weights can be tuned');
console.log('4. Detailed metrics: Each entity has calculated metrics for transparency');

// Validate DDDSample expectations
console.log('\n=== DDDSAMPLE VALIDATION ===');
const location = updatedNodes.find(n => n.name === 'Location');
const handlingEvent = updatedNodes.find(n => n.name === 'HandlingEvent');
const cargo = updatedNodes.find(n => n.name === 'Cargo');
const voyage = updatedNodes.find(n => n.name === 'Voyage');

console.log(`Location: ${location?.dddRole} (expected: REFERENCE_ENTITY)`);
console.log(`HandlingEvent: ${handlingEvent?.dddRole} (expected: ENTITY or AGGREGATE_ROOT)`);
console.log(`Cargo: ${cargo?.dddRole} (expected: AGGREGATE_ROOT)`);
console.log(`Voyage: ${voyage?.dddRole} (expected: AGGREGATE_ROOT)`);

if (location?.dddRole === 'REFERENCE_ENTITY') {
    console.log('✓ Location correctly identified as REFERENCE_ENTITY');
} else {
    console.log('✗ Location NOT identified as REFERENCE_ENTITY');
}

console.log('\n=== NEXT STEPS ===');
console.log('1. Review the updated report at: public/agent-report-improved.json');
console.log('2. Test the frontend with improved heuristics');
console.log('3. Tune configuration parameters based on domain knowledge');
console.log('4. Integrate confidence scores into UI visualization');