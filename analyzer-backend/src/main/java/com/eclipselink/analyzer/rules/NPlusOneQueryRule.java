package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class NPlusOneQueryRule implements MappingRule {
    @Override
    public String getId() {
        return "N_PLUS_ONE_QUERY";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Detects potential N+1 query patterns in entity relationships.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();
        
        if (relationships != null) {
            int lazyCollectionCount = 0;
            int eagerCollectionCount = 0;
            int totalRelationships = relationships.size();
            
            for (RelationshipMetadata rel : relationships) {
                String mappingType = rel.getMappingType();
                boolean isCollection = mappingType.equals("OneToMany") || mappingType.equals("ManyToMany");
                
                if (isCollection) {
                    if (rel.isLazy()) {
                        lazyCollectionCount++;
                        
                        // Lazy collection without batch fetch is N+1 risk
                        String batchFetchType = rel.getBatchFetchType();
                        if (batchFetchType == null || batchFetchType.isEmpty()) {
                            violations.add(new Violation(getId(), getSeverity(),
                                "Lazy collection '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                                "' causes N+1 queries when accessed. Add @BatchFetch or use JOIN FETCH in queries."));
                        }
                        
                        // Check if relationship is frequently accessed (heuristic: mappedBy indicates bidirectional)
                        if (rel.getMappedBy() != null && !rel.getMappedBy().isEmpty()) {
                            violations.add(new Violation(getId(), "INFO",
                                "Bidirectional lazy collection '" + rel.getAttributeName() + "' is accessed from both sides. " +
                                "Ensure both sides use consistent fetch strategies to avoid N+1 queries."));
                        }
                    } else {
                        eagerCollectionCount++;
                        
                        // Eager collection can cause Cartesian product or large result sets
                        violations.add(new Violation(getId(), "WARNING",
                            "Eager collection '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                            "' may cause performance issues. Consider LAZY fetch with @BatchFetch."));
                    }
                } else {
                    // OneToOne or ManyToOne
                    if (!rel.isLazy()) {
                        // Eager single-ended relationship can cause unnecessary joins
                        violations.add(new Violation(getId(), "INFO",
                            "Eager relationship '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                            "' may cause unnecessary joins. Consider LAZY fetch unless always needed."));
                    }
                }
            }
            
            // Global N+1 risk assessment
            if (lazyCollectionCount >= 2) {
                violations.add(new Violation(getId(), "WARNING",
                    "Entity '" + entity.getName() + "' has " + lazyCollectionCount + " lazy collections. " +
                    "Accessing multiple collections will cause multiple N+1 query patterns. " +
                    "Consider using global batch fetching or redesigning data access."));
            }
            
            if (eagerCollectionCount >= 2) {
                violations.add(new Violation(getId(), "ERROR",
                    "Entity '" + entity.getName() + "' has " + eagerCollectionCount + " eager collections. " +
                    "This will cause Cartesian product issues and excessive memory usage. " +
                    "Change collections to LAZY fetch immediately."));
            }
            
            // N+1 risk for deep relationship chains
            if (totalRelationships >= 4) {
                violations.add(new Violation(getId(), "INFO",
                    "Entity '" + entity.getName() + "' has " + totalRelationships + " relationships. " +
                    "Complex entity graphs increase N+1 query risk. Consider using entity graphs or fetch plans."));
            }
        }
        
        // Check for EclipseLink specific optimizations
        boolean hasJoinFetch = false;
        boolean hasBatchFetch = false;
        
        if (relationships != null) {
            for (RelationshipMetadata rel : relationships) {
                if (rel.isJoinFetch()) hasJoinFetch = true;
                if (rel.getBatchFetchType() != null && !rel.getBatchFetchType().isEmpty()) hasBatchFetch = true;
            }
            
            if (hasJoinFetch && !hasBatchFetch) {
                violations.add(new Violation(getId(), "INFO",
                    "Entity '" + entity.getName() + "' uses @JoinFetch but not @BatchFetch. " +
                    "Combine both for optimal performance in complex queries."));
            }
        }
        
        return violations;
    }
}