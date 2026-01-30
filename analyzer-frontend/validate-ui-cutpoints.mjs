import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import vm from 'vm';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load shared DDD rules
const sharedRulesPath = path.join(__dirname, 'src/analysis/shared-rules.js');
let sharedRulesContent = fs.readFileSync(sharedRulesPath, 'utf8');
sharedRulesContent = sharedRulesContent.replace(/export default DDDRules;/, '');
const context = { module: { exports: {} }, exports: {}, console };
vm.createContext(context);
vm.runInContext(sharedRulesContent, context);
const DDDRules = context.DDDRules || context.module.exports;

// Load report
const reportPath = path.join(__dirname, 'public/agent-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
const nodesList = report.nodes;
const allNodesForRules = nodesList.map(n => ({ data: n }));
const config = DDDRules.defaultConfig;

console.log('=== UI CUT-POINT VISUALIZATION VALIDATION ===\n');
console.log('Expected visualization based on improved heuristics:');
console.log('1. Location edges: NORMAL (green, solid) - reference entity');
console.log('2. Leg → Voyage: CYAN (dashed) - cut-point');
console.log('3. HandlingEvent → Cargo/Voyage: NORMAL - root-to-root allowed');
console.log('4. Cargo → Location: NORMAL - root referencing reference entity');
console.log('5. CarrierMovement → Location: NORMAL - entity referencing reference entity');
console.log('');

// Simulate UI edge generation
const cutPointEdges = [];
const normalEdges = [];

nodesList.forEach(source => {
    source.relationships?.forEach(rel => {
        const target = nodesList.find(t => t.name === rel.targetEntity);
        if (!target) return;
        
        const sourceForAnalysis = { data: source };
        const targetForAnalysis = { data: target };
        
        const analysis = DDDRules.analyzeCutPoint(rel, sourceForAnalysis, targetForAnalysis, allNodesForRules, config);
        
        const edgeInfo = {
            source: source.name,
            target: target.name,
            mapping: rel.mappingType,
            isCutPoint: analysis.isCutPoint,
            confidence: analysis.confidence,
            reason: analysis.reason,
            sourceRole: source.dddRole,
            targetRole: target.dddRole,
            sourceAgg: source.aggregateName,
            targetAgg: target.aggregateName
        };
        
        if (analysis.isCutPoint) {
            cutPointEdges.push(edgeInfo);
        } else {
            normalEdges.push(edgeInfo);
        }
    });
});

console.log(`Total edges: ${cutPointEdges.length + normalEdges.length}`);
console.log(`Cut-point edges (should be cyan): ${cutPointEdges.length}`);
console.log(`Normal edges (should be green): ${normalEdges.length}\n`);

// Check critical expectations
console.log('=== CRITICAL VALIDATIONS ===');

// 1. Location edges should NOT be cut-points
const locationEdges = normalEdges.filter(e => e.target === 'Location' || e.source === 'Location');
const locationCutPoints = cutPointEdges.filter(e => e.target === 'Location' || e.source === 'Location');
console.log(`1. Location edges: ${locationEdges.length} normal, ${locationCutPoints.length} cut-points`);
if (locationCutPoints.length === 0) {
    console.log('   ✓ PASS: No Location edges marked as cut-points');
} else {
    console.log('   ✗ FAIL: Location edges incorrectly marked as cut-points');
    locationCutPoints.forEach(e => {
        console.log(`     ${e.source} → ${e.target}: ${e.reason}`);
    });
}

// 2. Leg → Voyage should be cut-point
const legToVoyage = cutPointEdges.find(e => e.source === 'Leg' && e.target === 'Voyage');
console.log(`2. Leg → Voyage: ${legToVoyage ? 'cut-point' : 'normal'}`);
if (legToVoyage) {
    console.log(`   ✓ PASS: Leg → Voyage correctly marked as cut-point`);
    console.log(`     Confidence: ${legToVoyage.confidence.toFixed(2)}, Reason: ${legToVoyage.reason}`);
} else {
    console.log('   ✗ FAIL: Leg → Voyage should be cut-point but is not');
}

// 3. HandlingEvent → Cargo/Voyage should NOT be cut-points
const handlingEventEdges = normalEdges.filter(e => e.source === 'HandlingEvent');
const handlingEventCutPoints = cutPointEdges.filter(e => e.source === 'HandlingEvent');
console.log(`3. HandlingEvent edges: ${handlingEventEdges.length} normal, ${handlingEventCutPoints.length} cut-points`);
if (handlingEventCutPoints.length === 0) {
    console.log('   ✓ PASS: HandlingEvent edges correctly not marked as cut-points (root-to-root allowed)');
} else {
    console.log('   ✗ FAIL: HandlingEvent edges incorrectly marked as cut-points');
}

// 4. Cargo → Location should NOT be cut-point
const cargoToLocation = normalEdges.find(e => e.source === 'Cargo' && e.target === 'Location');
console.log(`4. Cargo → Location: ${cargoToLocation ? 'normal' : 'cut-point'}`);
if (cargoToLocation) {
    console.log('   ✓ PASS: Cargo → Location correctly not marked as cut-point (root referencing reference entity)');
} else {
    console.log('   ✗ FAIL: Cargo → Location should not be cut-point but is');
}

console.log('\n=== CUT-POINT EDGES DETAIL ===');
if (cutPointEdges.length === 0) {
    console.log('No cut-point edges detected');
} else {
    cutPointEdges.forEach(edge => {
        console.log(`${edge.source} → ${edge.target}: ${edge.mapping}`);
        console.log(`  Confidence: ${edge.confidence.toFixed(2)}`);
        console.log(`  Reason: ${edge.reason}`);
        console.log(`  Source: ${edge.sourceRole} in ${edge.sourceAgg}`);
        console.log(`  Target: ${edge.targetRole} in ${edge.targetAgg}`);
        console.log('');
    });
}

console.log('\n=== UI RENDERING SUMMARY ===');
console.log('Based on the analysis, the UI should render:');
console.log(`- ${cutPointEdges.length} cyan dashed edges (cut-points)`);
console.log(`- ${normalEdges.length} green solid edges (normal relationships)`);
console.log('\nIf the UI matches this, the improved heuristics are working correctly!');