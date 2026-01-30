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

const models = [
    { name: 'DDDSample', file: 'agent-report.json' },
    { name: 'E-commerce Complex', file: 'complex-scenario-report.json' },
    { name: 'OFBiz (113 entities)', file: 'ofbiz-report.json' },
    { name: 'Complex Inheritance', file: 'complex-inheritance-report.json' },
    { name: 'Cyclic', file: 'cyclic-report.json' }
];

console.log('=== TESTING DDD HEURISTICS ON MULTIPLE DOMAIN MODELS ===\n');

for (const model of models) {
    const reportPath = path.join(__dirname, 'public', model.file);
    if (!fs.existsSync(reportPath)) {
        console.log(`${model.name}: File not found (${model.file})`);
        continue;
    }
    
    try {
        const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
        const nodesList = report.nodes || [];
        const allNodesForRules = nodesList.map(n => ({ data: n }));
        const config = DDDRules.defaultConfig;
        
        // Generate analysis report using shared rules
        const analysisReport = DDDRules.generateAnalysisReport(nodesList, config);
        
        console.log(`📊 ${model.name}`);
        console.log(`   Entities: ${nodesList.length}`);
        console.log(`   Relationships: ${nodesList.reduce((sum, n) => sum + ((n.relationships || []).length), 0)}`);
        
        // Role distribution
        const roles = analysisReport.summary.roles;
        console.log('   Roles:');
        Object.entries(roles).forEach(([role, count]) => {
            console.log(`     ${role}: ${count}`);
        });
        
        // Cut-point analysis
        const cutPoints = analysisReport.summary.cutPoints || [];
        console.log(`   Cut-points (score > 1.0): ${cutPoints.length}`);
        if (cutPoints.length > 0) {
            console.log('   Top cut-points:');
            cutPoints.slice(0, 3).forEach(cp => {
                console.log(`     ${cp.node}: score ${cp.score.toFixed(2)} (${cp.role}, agg:${cp.aggregate})`);
            });
            if (cutPoints.length > 3) {
                console.log(`     ... and ${cutPoints.length - 3} more`);
            }
        }
        
        // Reference entities detected
        const refEntities = nodesList.filter(n => n.dddRole === 'REFERENCE_ENTITY' || 
            (analysisReport.nodes.find(an => an.name === n.name)?.role === 'REFERENCE_ENTITY'));
        if (refEntities.length > 0) {
            console.log(`   Reference entities: ${refEntities.map(e => e.name).join(', ')}`);
        }
        
        // Confidence statistics
        const confidences = analysisReport.nodes.map(n => n.roleConfidence || 0);
        const avgConfidence = confidences.length > 0 ? 
            confidences.reduce((a, b) => a + b, 0) / confidences.length : 0;
        const highConfidence = confidences.filter(c => c >= 0.7).length;
        const mediumConfidence = confidences.filter(c => c >= 0.5 && c < 0.7).length;
        const lowConfidence = confidences.filter(c => c < 0.5).length;
        
        console.log(`   Confidence scores:`);
        console.log(`     Avg: ${avgConfidence.toFixed(2)}, High (≥0.7): ${highConfidence}, Medium (≥0.5): ${mediumConfidence}, Low (<0.5): ${lowConfidence}`);
        
        console.log();
        
    } catch (error) {
        console.log(`${model.name}: Error - ${error.message}`);
        console.log();
    }
}

console.log('=== HEURISTIC PERFORMANCE SUMMARY ===\n');
console.log('The unified DDD heuristic rules with confidence scoring should:');
console.log('1. Correctly identify REFERENCE_ENTITIES (high connectivity, few attributes)');
console.log('2. Distinguish AGGREGATE_ROOTS (cascade relationships, high out-degree)');
console.log('3. Only flag non-root → other aggregate relationships as cut-points');
console.log('4. Exclude reference entity relationships from cut-point detection');
console.log('5. Provide meaningful confidence scores for classification decisions');
console.log('\nCheck each model for reasonable results based on domain understanding.');