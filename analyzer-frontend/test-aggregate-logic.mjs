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

console.log('=== ANALYSE DES RÔLES ET AGGREGATES ===\n');

// Vérifier les rôles et aggregates
nodesList.forEach(node => {
    const role = DDDRules.identifyRole({ data: node }, allNodesForRules);
    const aggName = DDDRules.getAggregateName({ data: node }, allNodesForRules);
    console.log(`${node.name}:`);
    console.log(`  rôle: ${role}`);
    console.log(`  aggregateName (DDDRules): ${aggName}`);
    console.log(`  aggregateName (Java): ${node.aggregateName}`);
    console.log(`  dddRole (Java): ${node.dddRole}`);
    console.log();
});

// Problème: getAggregateName ne retourne que si dddRole === 'AGGREGATE_ROOT'
// Mais HandlingEvent a aggregateName: 'Handling' et dddRole: 'ENTITY'
// Il faut que getAggregateName utilise aussi data.aggregateName

console.log('\n=== RELATIONS PROBLÉMATIQUES ===\n');

// Analyser chaque relation
nodesList.forEach(sourceNode => {
    sourceNode.relationships?.forEach(rel => {
        const targetNode = nodesList.find(t => t.name === rel.targetEntity);
        if (!targetNode) return;
        
        const sourceRole = DDDRules.identifyRole({ data: sourceNode }, allNodesForRules);
        const targetRole = DDDRules.identifyRole({ data: targetNode }, allNodesForRules);
        const sourceAgg = DDDRules.getAggregateName({ data: sourceNode }, allNodesForRules) || sourceNode.aggregateName;
        const targetAgg = DDDRules.getAggregateName({ data: targetNode }, allNodesForRules) || targetNode.aggregateName;
        
        const isCut = DDDRules.isCutPointEdge(rel, { data: sourceNode }, { data: targetNode }, allNodesForRules);
        
        // Logique DDD: 
        // - Si source n'est pas racine et target est dans un autre aggregate → problème
        // - Si source est racine → OK (peut référencer d'autres racines par ID)
        const isProblematic = sourceRole !== 'AGGREGATE_ROOT' && sourceAgg && targetAgg && sourceAgg !== targetAgg;
        
        if (sourceAgg !== targetAgg) {
            console.log(`${sourceNode.name} (${sourceRole}, agg:${sourceAgg}) → ${targetNode.name} (${targetRole}, agg:${targetAgg})`);
            console.log(`  mapping: ${rel.mappingType}, isCutPointEdge: ${isCut}, problématique DDD: ${isProblematic}`);
            console.log();
        }
    });
});

console.log('\n=== CORRECTIONS NÉCESSAIRES ===');
console.log('1. getAggregateName doit utiliser data.aggregateName si disponible');
console.log('2. isCutPointEdge doit vérifier:');
console.log('   - Si source est REFERENCE_ENTITY → false');
console.log('   - Si source n\'est pas AGGREGATE_ROOT ET targetAgg ≠ sourceAgg → true (problème)');
console.log('   - Si source est AGGREGATE_ROOT → false (les racines peuvent se référencer)');