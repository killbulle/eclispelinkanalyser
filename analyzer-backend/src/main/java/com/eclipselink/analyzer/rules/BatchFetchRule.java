package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class BatchFetchRule implements MappingRule {
    @Override
    public String getId() {
        return "BATCH_FETCH";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Recommends batch fetching annotations (@BatchFetch) to optimize N+1 query scenarios.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();
        
        if (relationships != null) {
            int collectionRelationships = 0;
            boolean hasPotentialNPlusOne = false;
            
            for (RelationshipMetadata rel : relationships) {
                String mappingType = rel.getMappingType();
                boolean isCollection = mappingType.equals("OneToMany") || mappingType.equals("ManyToMany");
                
                if (isCollection) {
                    collectionRelationships++;
                    
                    // Check if batch fetch is already configured
                    String batchFetchType = rel.getBatchFetchType();
                    boolean isJoinFetch = rel.isJoinFetch();
                    
                    // Recommendation 1: Collection relationships without batch fetch
                    if (batchFetchType == null || batchFetchType.isEmpty()) {
                        violations.add(new Violation(getId(), getSeverity(),
                            "Collection relationship '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                            "' does not use batch fetching. Consider adding @BatchFetch to optimize N+1 queries."));
                        hasPotentialNPlusOne = true;
                    } else {
                        // Validate batch fetch type
                        if (!batchFetchType.equals("JOIN") && !batchFetchType.equals("EXISTS") && !batchFetchType.equals("IN")) {
                            violations.add(new Violation(getId(), "WARNING",
                                "Batch fetch type '" + batchFetchType + "' on relationship '" + rel.getAttributeName() + 
                                "' is not standard. Use JOIN, EXISTS, or IN."));
                        }
                        
                        // Check if join fetch conflicts with batch fetch
                        if (isJoinFetch && batchFetchType.equals("JOIN")) {
                            violations.add(new Violation(getId(), "INFO",
                                "Relationship '" + rel.getAttributeName() + "' uses both join fetch and batch fetch JOIN. " +
                                "This may cause Cartesian product issues."));
                        }
                    }
                    
                    // Recommendation 2: Lazy collection without batch fetch
                    if (rel.isLazy() && (batchFetchType == null || batchFetchType.isEmpty())) {
                        violations.add(new Violation(getId(), "WARNING",
                            "Lazy collection '" + rel.getAttributeName() + "' does not use batch fetching. " +
                            "This will cause N+1 queries when accessing the collection."));
                    }
                }
                
                // Check ManyToOne relationships for batch fetch opportunities
                if (mappingType.equals("ManyToOne") || mappingType.equals("OneToOne")) {
                    if (rel.isLazy() && (rel.getBatchFetchType() == null || rel.getBatchFetchType().isEmpty())) {
                        // ManyToOne relationships can also benefit from batch fetching
                        violations.add(new Violation(getId(), "INFO",
                            "Lazy relationship '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                            "' could benefit from @BatchFetch when loading multiple parent entities."));
                    }
                }
            }
            
            // Global recommendation for entities with multiple collection relationships
            if (collectionRelationships >= 2 && hasPotentialNPlusOne) {
                violations.add(new Violation(getId(), "INFO",
                    "Entity '" + entity.getName() + "' has " + collectionRelationships + " collection relationships. " +
                    "Consider using global batch fetching configuration in persistence.xml or @BatchFetch on all collections."));
            }
            
            // Check for EclipseLink specific: @JoinFetch vs @BatchFetch
            for (RelationshipMetadata rel : relationships) {
                if (rel.isJoinFetch() && (rel.getBatchFetchType() == null || rel.getBatchFetchType().isEmpty())) {
                    violations.add(new Violation(getId(), "INFO",
                        "Relationship '" + rel.getAttributeName() + "' uses @JoinFetch. " +
                        "Consider combining with @BatchFetch for optimal performance."));
                }
            }
        }
        
        return violations;
    }
}