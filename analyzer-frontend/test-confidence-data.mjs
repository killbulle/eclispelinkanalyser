import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load report
const reportPath = path.join(__dirname, 'public/agent-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
const nodesList = report.nodes;

console.log('=== CHECKING CONFIDENCE DATA IN REPORT ===\n');

// Check first few nodes for confidence fields
nodesList.slice(0, 3).forEach(node => {
    console.log(`Node: ${node.name}`);
    console.log(`  dddRole: ${node.dddRole}`);
    console.log(`  dddRoleConfidence: ${node.dddRoleConfidence}`);
    console.log(`  aggregateName: ${node.aggregateName}`);
    console.log(`  aggregateNameConfidence: ${node.aggregateNameConfidence}`);
    console.log(`  cutPointScore: ${node.cutPointScore}`);
    console.log(`  cutPointNormalized: ${node.cutPointNormalized}`);
    console.log('');
});

// Count nodes with confidence data
const nodesWithRoleConfidence = nodesList.filter(n => n.dddRoleConfidence !== undefined).length;
const nodesWithAggregateConfidence = nodesList.filter(n => n.aggregateNameConfidence !== undefined).length;
const nodesWithCutPointScore = nodesList.filter(n => n.cutPointScore !== undefined).length;

console.log(`Summary:`);
console.log(`  Total nodes: ${nodesList.length}`);
console.log(`  Nodes with dddRoleConfidence: ${nodesWithRoleConfidence}`);
console.log(`  Nodes with aggregateNameConfidence: ${nodesWithAggregateConfidence}`);
console.log(`  Nodes with cutPointScore: ${nodesWithCutPointScore}`);

// Check if all nodes have confidence data
if (nodesWithRoleConfidence === nodesList.length) {
    console.log('\n✅ All nodes have dddRoleConfidence data');
} else {
    console.log(`\n⚠️  Only ${nodesWithRoleConfidence}/${nodesList.length} nodes have dddRoleConfidence data`);
}

// Check range of confidence scores
const confidences = nodesList.map(n => n.dddRoleConfidence).filter(c => c !== undefined);
if (confidences.length > 0) {
    const min = Math.min(...confidences);
    const max = Math.max(...confidences);
    const avg = confidences.reduce((a, b) => a + b, 0) / confidences.length;
    console.log(`\nConfidence score range: ${min.toFixed(2)} - ${max.toFixed(2)}, Avg: ${avg.toFixed(2)}`);
}