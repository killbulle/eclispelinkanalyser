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
console.log('DDDSample Analysis');
console.log('==================');

// 1. Current frontend logic (using Java aggregateName)
console.log('\n1. Current frontend cut-point detection (Java aggregateName):');
const cutPoints = new Set();
nodesList.forEach(n => {
    if (n.relationships?.some(r => {
        const target = nodesList.find(t => t.name === r.targetEntity);
        return target && target.aggregateName !== n.aggregateName && n.aggregateName && target.aggregateName;
    })) {
        cutPoints.add(n.name);
    }
});
console.log('Cut-points:', Array.from(cutPoints).sort());
console.log(`Count: ${cutPoints.size}/${nodesList.length}`);

// 2. Improved heuristic using DDDRules.isCutPointEdge
console.log('\n2. Improved heuristic (DDDRules.isCutPointEdge):');
const allNodes = nodesList.map(node => ({ data: node }));
const improvedCutPoints = new Set();
nodesList.forEach(n => {
    if (n.relationships?.some(r => {
        const target = nodesList.find(t => t.name === r.targetEntity);
        if (!target) return false;
        // Use DDDRules.isCutPointEdge
        const isCut = DDDRules.isCutPointEdge(r, { data: n }, { data: target }, allNodes);
        return isCut;
    })) {
        improvedCutPoints.add(n.name);
    }
});
console.log('Cut-points:', Array.from(improvedCutPoints).sort());
console.log(`Count: ${improvedCutPoints.size}/${nodesList.length}`);

// 3. Cut-point score threshold > 1.0
console.log('\n3. Cut-point score threshold > 1.0:');
const scoreCutPoints = new Set();
nodesList.forEach(n => {
    const score = DDDRules.getCutPointScore({ data: n }, allNodes);
    if (score > 1.0) {
        scoreCutPoints.add(n.name);
    }
});
console.log('Cut-points:', Array.from(scoreCutPoints).sort());
console.log(`Count: ${scoreCutPoints.size}/${nodesList.length}`);

// 4. Detailed analysis per entity
console.log('\n4. Detailed per-entity analysis:');
nodesList.forEach(n => {
    const targetAggs = new Set();
    n.relationships?.forEach(r => {
        const target = nodesList.find(t => t.name === r.targetEntity);
        if (target && target.aggregateName !== n.aggregateName) {
            targetAggs.add(target.aggregateName);
        }
    });
    const score = DDDRules.getCutPointScore({ data: n }, allNodes);
    const role = DDDRules.identifyRole({ data: n }, allNodes);
    console.log(`  ${n.name}:`);
    console.log(`    aggregateName: ${n.aggregateName}`);
    console.log(`    role: ${role}`);
    console.log(`    cross-aggregate relationships: ${Array.from(targetAggs).join(', ') || 'none'}`);
    console.log(`    cut-point score: ${score.toFixed(2)}`);
    console.log(`    isCutPoint (current): ${cutPoints.has(n.name)}`);
    console.log(`    isCutPoint (improved): ${improvedCutPoints.has(n.name)}`);
});

// 5. Summary
console.log('\n5. Summary:');
console.log(`Total entities: ${nodesList.length}`);
console.log(`Java analyzer aggregate detection:`);
const aggMap = new Map();
nodesList.forEach(n => aggMap.set(n.aggregateName, (aggMap.get(n.aggregateName) || 0) + 1));
aggMap.forEach((count, agg) => console.log(`  ${agg}: ${count} entities`));
console.log(`Official DDDSample aggregates: Cargo, HandlingEvent, Voyage`);
console.log(`Location is a shared reference entity (not an aggregate).`);