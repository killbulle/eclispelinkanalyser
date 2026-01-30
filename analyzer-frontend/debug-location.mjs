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

const allNodesForRules = nodesList.map(n => ({ data: n }));

// Find Location node
const locationNode = nodesList.find(n => n.name === 'Location');
const locationForAnalysis = { data: locationNode };

console.log('=== DEBUG LOCATION ANALYSIS ===\n');

// Calculate metrics manually
const data = locationNode;
const nodeName = data.name;
let incomingRelations = 0;
let outgoingRelations = data.relationships ? data.relationships.length : 0;
let hasCollections = false;

// Analyze outgoing relationships
if (data.relationships) {
    for (const rel of data.relationships) {
        if (rel.mappingType && (rel.mappingType.includes('OneToMany') || rel.mappingType.includes('ManyToMany'))) {
            hasCollections = true;
        }
    }
}

// Analyze incoming relationships
for (const other of nodesList) {
    if (other.name === nodeName) continue;
    if (other.relationships) {
        for (const rel of other.relationships) {
            if (rel.targetEntity === nodeName) {
                incomingRelations++;
            }
        }
    }
}

const totalRelations = incomingRelations + outgoingRelations;
const incomingOutgoingRatio = outgoingRelations > 0 ? incomingRelations / outgoingRelations : Infinity;
const attributeCount = Object.keys(data.attributes || {}).length;

console.log('Metrics:');
console.log(`  incomingRelations: ${incomingRelations}`);
console.log(`  outgoingRelations: ${outgoingRelations}`);
console.log(`  totalRelations: ${totalRelations}`);
console.log(`  incomingOutgoingRatio: ${incomingOutgoingRatio}`);
console.log(`  attributeCount: ${attributeCount}`);
console.log(`  hasCollections: ${hasCollections}`);
console.log(`  type: ${data.type}`);

// Check reference entity conditions
const refConfig = DDDRules.defaultConfig.referenceEntity;
console.log('\nReference entity config:');
console.log(`  minTotalRelations: ${refConfig.minTotalRelations}`);
console.log(`  incomingOutgoingRatio: ${refConfig.incomingOutgoingRatio}`);
console.log(`  maxAttributes: ${refConfig.maxAttributes}`);
console.log(`  allowCollections: ${refConfig.allowCollections}`);

const conditions = {
    totalRelations: totalRelations >= refConfig.minTotalRelations,
    incomingOutgoingRatio: incomingOutgoingRatio >= refConfig.incomingOutgoingRatio,
    attributeCount: attributeCount <= refConfig.maxAttributes,
    hasCollections: refConfig.allowCollections ? true : !hasCollections,
    type: data.type === "ENTITY"
};

console.log('\nConditions:');
Object.entries(conditions).forEach(([key, value]) => {
    console.log(`  ${key}: ${value}`);
});

// Use DDDRules to analyze
console.log('\n=== USING DDDRULES ===');
const config = DDDRules.defaultConfig;
const metrics = DDDRules._calculateMetrics(locationForAnalysis, allNodesForRules, config);
console.log('Calculated metrics:', JSON.stringify(metrics, null, 2));

const scores = DDDRules._calculateRoleScores(metrics, config);
console.log('\nRole scores:', JSON.stringify(scores, null, 2));

const role = DDDRules._determineRole(scores, config);
console.log(`Determined role: ${role}`);

const confidence = DDDRules._calculateConfidence(scores, role, config);
console.log(`Confidence: ${confidence}`);

// Full analysis
const analysis = DDDRules.analyzeRole(locationForAnalysis, allNodesForRules, config);
console.log('\nFull analysis:', JSON.stringify(analysis, null, 2));

// Check all nodes relationships to Location
console.log('\n=== INCOMING RELATIONSHIPS TO LOCATION ===');
nodesList.forEach(other => {
    if (other.name === nodeName) return;
    if (other.relationships) {
        other.relationships.forEach(rel => {
            if (rel.targetEntity === nodeName) {
                console.log(`${other.name} -> Location (${rel.mappingType})`);
            }
        });
    }
});