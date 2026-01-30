import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import vm from 'vm';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load shared-rules.js
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

console.log('=== EDGE CUT-POINT ANALYSIS (DDDSample) ===\n');

let cutPointEdges = [];
let totalEdges = 0;

nodesList.forEach(source => {
    source.relationships?.forEach(rel => {
        totalEdges++;
        const target = nodesList.find(t => t.name === rel.targetEntity);
        if (!target) return;
        
        const isCutPointEdge = DDDRules.isCutPointEdge(rel, 
            { data: source }, 
            { data: target }, 
            allNodesForRules);
        
        if (isCutPointEdge) {
            cutPointEdges.push({
                source: source.name,
                target: target.name,
                mappingType: rel.mappingType,
                sourceAggregate: source.aggregateName,
                targetAggregate: target.aggregateName,
                sourceRole: DDDRules.identifyRole({ data: source }, allNodesForRules),
                targetRole: DDDRules.identifyRole({ data: target }, allNodesForRules)
            });
        }
    });
});

console.log(`Total edges: ${totalEdges}`);
console.log(`Cut-point edges (cyan): ${cutPointEdges.length}\n`);

cutPointEdges.forEach(edge => {
    console.log(`${edge.source} (${edge.sourceRole}, agg:${edge.sourceAggregate})`);
    console.log(`  → ${edge.target} (${edge.targetRole}, agg:${edge.targetAggregate})`);
    console.log(`  mapping: ${edge.mappingType}`);
    console.log();
});

// Also list all edges with their cut-point status for verification
console.log('\n=== ALL EDGES WITH CUT-POINT STATUS ===');
nodesList.forEach(source => {
    source.relationships?.forEach(rel => {
        const target = nodesList.find(t => t.name === rel.targetEntity);
        if (!target) return;
        
        const isCutPointEdge = DDDRules.isCutPointEdge(rel, 
            { data: source }, 
            { data: target }, 
            allNodesForRules);
        
        const sourceRole = DDDRules.identifyRole({ data: source }, allNodesForRules);
        const targetRole = DDDRules.identifyRole({ data: target }, allNodesForRules);
        
        console.log(`${source.name} → ${target.name}: ${rel.mappingType} ${isCutPointEdge ? '✂️ CYAN' : 'normal'}`);
        console.log(`  source: ${sourceRole}, agg:${source.aggregateName}`);
        console.log(`  target: ${targetRole}, agg:${target.aggregateName}`);
    });
});