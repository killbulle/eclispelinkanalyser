package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class CartesianProductRule implements MappingRule {
    @Override
    public String getId() {
        return "CARTESIAN_PRODUCT";
    }

    @Override
    public String getSeverity() {
        return "ERROR";
    }

    @Override
    public String getDescription() {
        return "Detects potential Cartesian product issues from multiple eager relationships.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();
        
        if (relationships != null) {
            int eagerCount = 0;
            int joinFetchCount = 0;
            List<String> eagerRelationships = new ArrayList<>();
            
            for (RelationshipMetadata rel : relationships) {
                if (!rel.isLazy()) {
                    eagerCount++;
                    eagerRelationships.add(rel.getAttributeName());
                    
                    // Eager collection relationships are especially problematic
                    String mappingType = rel.getMappingType();
                    boolean isCollection = mappingType.equals("OneToMany") || mappingType.equals("ManyToMany");
                    
                    if (isCollection) {
                        violations.add(new Violation(getId(), "ERROR",
                            "Eager collection '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                            "' will cause Cartesian product when combined with other relationships. " +
                            "Change to LAZY fetch immediately."));
                    }
                }
                
                if (rel.isJoinFetch()) {
                    joinFetchCount++;
                    
                    // Multiple join fetch can cause Cartesian product
                    if (joinFetchCount > 1) {
                        violations.add(new Violation(getId(), "ERROR",
                            "Multiple @JoinFetch annotations in entity '" + entity.getName() + 
                            "' will cause Cartesian product. Use @BatchFetch instead for multiple relationships."));
                    }
                }
            }
            
            // Critical: Multiple eager relationships
            if (eagerCount >= 2) {
                violations.add(new Violation(getId(), "ERROR",
                    "Entity '" + entity.getName() + "' has " + eagerCount + " eager relationships: " + 
                    String.join(", ", eagerRelationships) + ". " +
                    "This will cause Cartesian product explosion. Change all to LAZY fetch."));
            }
            
            // Check for EclipseLink specific: @JoinFetch with multiple collection relationships
            if (joinFetchCount >= 1) {
                int collectionCount = 0;
                for (RelationshipMetadata rel : relationships) {
                    String mappingType = rel.getMappingType();
                    if (mappingType.equals("OneToMany") || mappingType.equals("ManyToMany")) {
                        collectionCount++;
                    }
                }
                
                if (collectionCount >= 2 && joinFetchCount >= 1) {
                    violations.add(new Violation(getId(), "WARNING",
                        "Entity '" + entity.getName() + "' has " + collectionCount + " collections and uses @JoinFetch. " +
                        "Multiple collections with join fetch cause Cartesian product. Use @BatchFetch instead."));
                }
            }
            
            // Check for bidirectional eager relationships (circular eager loading)
            for (RelationshipMetadata rel : relationships) {
                if (!rel.isLazy() && rel.getMappedBy() != null && !rel.getMappedBy().isEmpty()) {
                    violations.add(new Violation(getId(), "WARNING",
                        "Bidirectional eager relationship '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                        "' can cause circular loading issues. Ensure one side uses LAZY fetch."));
                }
            }
        }
        
        // Heuristic: Entity with many relationships is at risk
        if (relationships != null && relationships.size() >= 5) {
            violations.add(new Violation(getId(), "INFO",
                "Entity '" + entity.getName() + "' has " + relationships.size() + " relationships. " +
                "Complex entity structure increases Cartesian product risk. Consider simplifying the model."));
        }
        
        return violations;
    }
}