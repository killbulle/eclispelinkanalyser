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

// Load agent-report.json
const reportPath = path.join(__dirname, 'public/agent-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
const nodesList = report.nodes;

const allNodesForRules = nodesList.map(n => ({ data: n }));

// Find Location node
const locationNode = nodesList.find(n => n.name === 'Location');
const locationForAnalysis = { data: locationNode };

console.log('=== TEST LOCATION FIX ===\n');

const config = DDDRules.defaultConfig;
console.log('Config:', JSON.stringify(config, null, 2));

const analysis = DDDRules.analyzeRole(locationForAnalysis, allNodesForRules, config);
console.log('Location analysis:', JSON.stringify(analysis, null, 2));

// Test all nodes
console.log('\n=== ALL NODES ROLES ===');
nodesList.forEach(node => {
    const nodeForAnalysis = { data: node };
    const analysis = DDDRules.analyzeRole(nodeForAnalysis, allNodesForRules, config);
    console.log(`${node.name}: ${analysis.role} (conf: ${analysis.confidence.toFixed(2)})`);
});

// Generate report
console.log('\n=== GENERATING ANALYSIS REPORT ===');
const fullReport = DDDRules.generateAnalysisReport(nodesList, config);
console.log('Summary:');
console.log(`Total nodes: ${fullReport.summary.totalNodes}`);
console.log('Roles:', JSON.stringify(fullReport.summary.roles, null, 2));
console.log('Aggregates:', JSON.stringify(fullReport.summary.aggregates, null, 2));
console.log('Cut-points:', fullReport.summary.cutPoints.length > 0 ? 
    fullReport.summary.cutPoints.map(cp => `${cp.node} (${cp.score})`).join(', ') : 'None');