# DDD Heuristic Rules Documentation

## Overview

This document describes the configurable heuristic rules for Domain-Driven Design (DDD) analysis in the EclipseLink Analyzer. The rules are implemented in `shared/ddd-rules.js` and used by both the Java backend and TypeScript frontend.

## Key Concepts

### 1. REFERENCE_ENTITY
Shared, read-only entities that act as catalogs or code tables (e.g., Location, Category, Country codes).
- **Characteristics**: High connectivity, few attributes, more incoming than outgoing relationships
- **Purpose**: Eliminate false positive cut-point detection for reference entity relationships

### 2. AGGREGATE_ROOT
Entities that control the lifecycle of other entities within the same aggregate.
- **Characteristics**: Cascade relationships, high out-degree, not strongly owned by other entities
- **Purpose**: Identify aggregate boundaries for cut-point detection

### 3. CUT-POINT
Relationships that cross aggregate boundaries from non-root entities (potential design issues).
- **Detection**: Non-root entity → different aggregate (excluding reference entities)
- **Purpose**: Flag potential DDD violations for refactoring

### 4. Confidence Scoring
Quantitative measure (0-1) of classification certainty based on heuristic strength.

## Configuration Parameters

### Reference Entity Detection (`referenceEntity`)
```javascript
minTotalRelations: 3,           // Minimum total incoming+outgoing relationships
incomingOutgoingRatio: 1.5,     // Incoming must be > outgoing * this ratio
maxAttributes: 5,               // Maximum number of attributes  
allowCollections: false,        // Reference entities shouldn't have collections
```

**Usage**: Entities meeting all criteria are classified as REFERENCE_ENTITY with high confidence.

### Aggregate Root Detection (`aggregateRoot`)
```javascript
cascadeWeight: 1.0,             // Weight for cascade relationships
outDegreeWeight: 0.8,           // Weight for high out-degree
cascadeThreshold: 1,            // Minimum cascade count to consider as root
outDegreeThreshold: 3,          // Minimum out-degree to consider as root
strongOwnershipPenalty: -1.0,   // Penalty if entity is strongly owned by another
```

**Usage**: Entities with positive scores (after penalties) and meeting thresholds are classified as AGGREGATE_ROOT.

### Confidence Thresholds (`confidence`)
```javascript
highThreshold: 0.7,             // Confidence ≥ 0.7 = high confidence
mediumThreshold: 0.5,           // Confidence ≥ 0.5 = medium confidence  
lowThreshold: 0.3,              // Confidence < 0.3 = low confidence
```

**Usage**: Determines confidence level display in UI.

### Cut-point Detection (`cutPoint`)
```javascript
referenceEntityAllowed: false,  // Relationships to/from reference entities are NOT cut-points
rootToRootAllowed: true,        // Aggregate roots can reference other roots
entityToRootIsCutPoint: true,   // Non-root entity referencing another aggregate = cut-point
relationshipWeights: {          // Weight factors for different relationship types
    ManyToOne: 0.5,
    OneToOne: 1.0,
    OneToMany: 1.5,
    ManyToMany: 2.0
}
```

**Usage**: Determines which edges are flagged as cut-points in the visualization.

## Algorithm Workflow

1. **Entity Classification**:
   - Check for special types (EMBEDDABLE, ABSTRACT_ENTITY)
   - Calculate metrics (incoming/outgoing relations, attributes, cascades)
   - Compute REFERENCE_ENTITY score first (highest specificity)
   - Compute AGGREGATE_ROOT score (excluding strongly owned entities)
   - Default to ENTITY if neither classification applies

2. **Confidence Calculation**:
   - Based on margin between selected role score and next best score
   - Formula: `confidence = roleScore * (1 + margin)`
   - Normalized to 0-1 range

3. **Aggregate Assignment**:
   - Use Java analyzer's aggregateName if available
   - Otherwise, find owning root via cascade relationships
   - Fallback to package-based grouping

4. **Cut-point Detection**:
   - Check if relationship involves reference entity (exclude if so)
   - Compare source and target aggregates
   - Apply DDD logic based on entity roles
   - Adjust confidence based on relationship type and cascade settings

## Tuning for Different Domain Types

### E-commerce Domains
```javascript
// More relaxed reference entity detection for product catalogs
referenceEntity: {
    minTotalRelations: 2,
    incomingOutgoingRatio: 1.2,
    maxAttributes: 8,
    allowCollections: true  // Product categories may have subcategories
}
```

### Enterprise Systems (OFBiz-style)
```javascript
// Stricter thresholds for complex enterprise domains
referenceEntity: {
    minTotalRelations: 4,
    incomingOutgoingRatio: 2.0,
    maxAttributes: 4,
    allowCollections: false
},
aggregateRoot: {
    cascadeWeight: 1.2,      // Emphasize cascade ownership
    outDegreeThreshold: 4    // Higher threshold for complex domains
}
```

### Microservices Migration
```javascript
// Sensitive cut-point detection for service boundary identification
cutPoint: {
    referenceEntityAllowed: false,
    rootToRootAllowed: false,  // Even root-to-root may indicate coupling
    entityToRootIsCutPoint: true,
    relationshipWeights: {
        ManyToOne: 1.0,       // Higher weights for all cross-aggregate relations
        OneToOne: 1.5,
        OneToMany: 2.0,
        ManyToMany: 3.0
    }
}
```

## Validation Results

### DDDSample Domain (Reference Implementation)
- **Location**: Correctly identified as REFERENCE_ENTITY (100% confidence)
- **Cut-points**: Only Leg → Voyage flagged (correct DDD violation)
- **False positives eliminated**: 3 Location edges no longer flagged
- **Confidence distribution**: 6/9 entities high confidence, 3/9 medium confidence

### OFBiz (113 entities)
- **Reference entities**: Enumeration, Uom, SecurityGroup, Tenant (correctly identified)
- **Cut-points**: 1 entity flagged (UserPreference)
- **Performance**: Handles large domains efficiently

## Integration Points

### Java Backend (`DDDAnalyzer.java`)
- Loads shared JS rules via ScriptEngine
- Falls back to native implementation with same heuristics
- Maintains backward compatibility

### TypeScript Frontend (`App.tsx`)
- Uses same shared rules for consistent analysis
- Displays confidence scores in UI
- Visualizes cut-points with cyan dashed edges

### Configuration Overrides
```javascript
// Example: Custom configuration for a specific domain
const customConfig = {
    referenceEntity: {
        minTotalRelations: 2,
        maxAttributes: 6
    },
    confidence: {
        highThreshold: 0.8
    }
};

const analysis = DDDRules.analyzeRole(node, allNodes, customConfig);
```

## Best Practices

1. **Start with defaults**: Use default parameters for initial analysis
2. **Validate with known domains**: Test against DDDSample or other reference implementations
3. **Adjust incrementally**: Tune one parameter at a time and observe impact
4. **Consider domain characteristics**: Different domains may require different thresholds
5. **Use confidence scores**: Low confidence indicates ambiguous cases needing manual review

## Troubleshooting

### Issue: Too many cut-points
- **Solution**: Increase `referenceEntity.incomingOutgoingRatio` to be more selective
- **Solution**: Check if entities should be classified as REFERENCE_ENTITY

### Issue: Too few aggregate roots
- **Solution**: Decrease `aggregateRoot.outDegreeThreshold`
- **Solution**: Increase `aggregateRoot.cascadeWeight`

### Issue: Low confidence scores
- **Solution**: Entities may be genuinely ambiguous (manual review needed)
- **Solution**: Adjust scoring weights to emphasize domain-specific characteristics

## Future Enhancements

1. **Machine learning tuning**: Automatically adjust parameters based on labeled datasets
2. **Domain-specific presets**: Pre-configured settings for common domain types
3. **Interactive tuning UI**: Visual parameter adjustment with real-time feedback
4. **Historical analysis**: Track classification changes over refactoring iterations

---

*Last Updated: January 30, 2026*  
*Version: 1.0.0*