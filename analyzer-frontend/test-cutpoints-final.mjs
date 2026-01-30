import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import vm from 'vm';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load updated DDD rules
const sharedRulesPath = path.join(__dirname, 'src/analysis/shared-rules.js');
let sharedRulesContent = fs.readFileSync(sharedRulesPath, 'utf8');
sharedRulesContent = sharedRulesContent.replace(/export default DDDRules;/, '');
const context = { module: { exports: {} }, exports: {}, console };
vm.createContext(context);
vm.runInContext(sharedRulesContent, context);
const DDDRules = context.DDDRules || context.module.exports;

// Load updated report
const reportPath = path.join(__dirname, 'public/agent-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
const nodesList = report.nodes;

const allNodesForRules = nodesList.map(n => ({ data: n }));
const config = DDDRules.defaultConfig;

console.log('=== FINAL CUT-POINT ANALYSIS ===\n');
console.log(`Entities: ${nodesList.length}`);
console.log(`Roles: ${nodesList.map(n => n.dddRole).join(', ')}\n`);

// Identify cut-point edges
const cutPointEdges = [];
nodesList.forEach(source => {
    source.relationships?.forEach(rel => {
        const target = nodesList.find(t => t.name === rel.targetEntity);
        if (!target) return;
        
        const sourceForAnalysis = { data: source };
        const targetForAnalysis = { data: target };
        
        const analysis = DDDRules.analyzeCutPoint(rel, sourceForAnalysis, targetForAnalysis, allNodesForRules, config);
        
        if (analysis.isCutPoint) {
            cutPointEdges.push({
                source: source.name,
                target: target.name,
                mapping: rel.mappingType,
                confidence: analysis.confidence,
                reason: analysis.reason,
                sourceRole: source.dddRole,
                targetRole: target.dddRole,
                sourceAgg: source.aggregateName,
                targetAgg: target.aggregateName
            });
        }
    });
});

console.log(`Total relationships: ${nodesList.reduce((sum, n) => sum + (n.relationships?.length || 0), 0)}`);
console.log(`Cut-point edges (cyan): ${cutPointEdges.length}\n`);

if (cutPointEdges.length === 0) {
    console.log('No cut-points detected!');
} else {
    cutPointEdges.forEach(edge => {
        console.log(`${edge.source} (${edge.sourceRole}, agg:${edge.sourceAgg})`);
        console.log(`  → ${edge.target} (${edge.targetRole}, agg:${edge.targetAgg})`);
        console.log(`  mapping: ${edge.mapping}, confidence: ${edge.confidence.toFixed(2)}`);
        console.log(`  reason: ${edge.reason}`);
        console.log();
    });
}

// Also list all edges for verification
console.log('\n=== ALL EDGES WITH CUT-POINT STATUS ===');
nodesList.forEach(source => {
    source.relationships?.forEach(rel => {
        const target = nodesList.find(t => t.name === rel.targetEntity);
        if (!target) return;
        
        const sourceForAnalysis = { data: source };
        const targetForAnalysis = { data: target };
        
        const analysis = DDDRules.analyzeCutPoint(rel, sourceForAnalysis, targetForAnalysis, allNodesForRules, config);
        
        console.log(`${source.name} → ${target.name}: ${rel.mappingType} ${analysis.isCutPoint ? '✂️ CYAN' : 'normal'}`);
        console.log(`  source: ${source.dddRole}, agg:${source.aggregateName}`);
        console.log(`  target: ${target.dddRole}, agg:${target.aggregateName}`);
        console.log(`  isCutPoint: ${analysis.isCutPoint}, reason: ${analysis.reason}`);
        console.log();
    });
});

// Check DDD compliance: entities referencing other aggregates
console.log('\n=== DDD COMPLIANCE CHECK ===');
let dddIssues = 0;
nodesList.forEach(source => {
    source.relationships?.forEach(rel => {
        const target = nodesList.find(t => t.name === rel.targetEntity);
        if (!target) return;
        
        if (source.dddRole !== 'AGGREGATE_ROOT' && source.aggregateName !== target.aggregateName) {
            console.log(`⚠️  ${source.name} (${source.dddRole}) references ${target.name} (${target.dddRole}) across aggregate boundary`);
            console.log(`   Aggregate: ${source.aggregateName} → ${target.aggregateName}`);
            console.log(`   Relationship: ${rel.mappingType}`);
            console.log(`   This is a potential DDD violation (non-root referencing another aggregate)`);
            dddIssues++;
        }
    });
});

console.log(`\nTotal DDD compliance issues: ${dddIssues}`);

// Summary
console.log('\n=== SUMMARY ===');
console.log('Expected based on DDDSample documentation:');
console.log('  - Location is REFERENCE_ENTITY (UN/LOCODE catalog)');
console.log('  - Cargo, Voyage, HandlingEvent are aggregates');
console.log('  - Leg, CarrierMovement, RouteSpecification, HandlingActivity, Delivery are internal entities');
console.log('  - Relationships to Location should NOT be cut-points');
console.log('  - Leg → Voyage might be a cut-point (entity referencing another aggregate)');
console.log('\nOur analysis:');
console.log(`  Location role: ${nodesList.find(n => n.name === 'Location')?.dddRole}`);
console.log(`  Cut-point edges: ${cutPointEdges.length}`);
console.log(`  DDD issues: ${dddIssues}`);