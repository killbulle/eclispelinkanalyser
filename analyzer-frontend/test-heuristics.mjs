import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { createRequire } from 'module';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load shared-rules.js as a module (it's a mix of CommonJS and ES modules)
// Since it has both module.exports and export default, we need to evaluate.
const sharedRulesPath = path.join(__dirname, 'src/analysis/shared-rules.js');
let sharedRulesContent = fs.readFileSync(sharedRulesPath, 'utf8');
// Remove ESM export line for Node evaluation
sharedRulesContent = sharedRulesContent.replace(/export default DDDRules;/, '');
// Use vm module to evaluate
import vm from 'vm';
const context = { module: { exports: {} }, exports: {}, console };
vm.createContext(context);
vm.runInContext(sharedRulesContent, context);
const DDDRules = context.DDDRules || context.module.exports;

// Load agent-report.json
const reportPath = path.join(__dirname, 'public/agent-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));

console.log('Testing improved cut-point heuristics on DDDSample');
console.log('Total entities:', report.nodes.length);

// Convert report nodes to format expected by identifyRole
const allNodes = report.nodes.map(node => ({
    data: node,
    name: node.name,
    className: node.name,
    type: node.type,
    packageName: node.packageName,
    relationships: node.relationships,
    attributes: node.attributes
}));

// Identify roles
console.log('\nEntity Roles:');
const roles = {};
report.nodes.forEach(node => {
    const role = DDDRules.identifyRole({ data: node }, allNodes);
    roles[node.name] = role;
    console.log(`  ${node.name}: ${role}`);
});

// Calculate cut-point scores
console.log('\nCut-point Scores:');
report.nodes.forEach(node => {
    const score = DDDRules.getCutPointScore({ data: node }, allNodes);
    console.log(`  ${node.name}: ${score.toFixed(2)}`);
});

// Identify reference entities
console.log('\nReference Entities:');
Object.entries(roles).forEach(([name, role]) => {
    if (role === 'REFERENCE_ENTITY') {
        console.log(`  ${name}`);
    }
});

// Determine cut-points (score > 1.5?)
console.log('\nPotential Cut-points (score > 1.5):');
report.nodes.forEach(node => {
    const score = DDDRules.getCutPointScore({ data: node }, allNodes);
    if (score > 1.5) {
        console.log(`  ${node.name}: ${score.toFixed(2)}`);
    }
});

// Check aggregate names from shared rules
console.log('\nAggregate Names (from DDDRules.getAggregateName):');
report.nodes.forEach(node => {
    const agg = DDDRules.getAggregateName({ data: node }, allNodes);
    console.log(`  ${node.name}: ${agg}`);
});

// Compare with existing aggregateName from report
console.log('\nComparison with existing aggregateName from Java analyzer:');
report.nodes.forEach(node => {
    const agg = DDDRules.getAggregateName({ data: node }, allNodes);
    console.log(`  ${node.name}: Java=${node.aggregateName}, Shared=${agg}`);
});

// Count cut-points
let cutPointCount = 0;
report.nodes.forEach(node => {
    const score = DDDRules.getCutPointScore({ data: node }, allNodes);
    if (score > 1.5) cutPointCount++;
});
console.log(`\nCut-point count: ${cutPointCount}/${report.nodes.length}`);

// Expected: Location should be REFERENCE_ENTITY, HandlingEvent likely cut-point
console.log('\n=== EXPECTED RESULTS ===');
console.log('1. Location should be REFERENCE_ENTITY (shared entity with many incoming relationships)');
console.log('2. HandlingEvent should be primary cut-point (connects Cargo, Location, Voyage aggregates)');
console.log('3. Cut-point count should be ~1/9 entities (previously 4/9)');