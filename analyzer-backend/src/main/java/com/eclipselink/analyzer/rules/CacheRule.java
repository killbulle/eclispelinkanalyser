package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class CacheRule implements MappingRule {
    @Override
    public String getId() {
        return "CACHE_OPTIMIZATION";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Recommends EclipseLink caching annotations (@Cacheable, @ReadOnly) for performance optimization.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        if (entity.getRelationships() != null) {
            int relationshipCount = entity.getRelationships().size();
            boolean hasCollections = false;
            boolean hasManyToOne = false;
            
            // Analyze relationship types
            for (RelationshipMetadata rel : entity.getRelationships()) {
                String mappingType = rel.getMappingType();
                if (mappingType.equals("OneToMany") || mappingType.equals("ManyToMany")) {
                    hasCollections = true;
                }
                if (mappingType.equals("ManyToOne") || mappingType.equals("OneToOne")) {
                    hasManyToOne = true;
                }
            }
            
            // Heuristic 1: Entities with many relationships are good cache candidates
            if (relationshipCount >= 3) {
                violations.add(new Violation(getId(), getSeverity(),
                    "Entity '" + entity.getName() + "' has " + relationshipCount + " relationships and is a good candidate for @Cacheable annotation. " +
                    "Consider adding @Cacheable to improve read performance for frequently accessed entities."));
            }
            
            // Heuristic 2: Entities with collection relationships benefit from caching
            if (hasCollections && relationshipCount >= 2) {
                violations.add(new Violation(getId(), getSeverity(),
                    "Entity '" + entity.getName() + "' has collection relationships and is frequently accessed. " +
                    "Consider using @Cacheable with appropriate cache size and expiry settings."));
            }
            
            // Heuristic 3: Entities with no relationships might be immutable/read-only
            if (relationshipCount == 0 && entity.getAttributes() != null && entity.getAttributes().size() <= 5) {
                // Simple entity with few attributes and no relationships
                violations.add(new Violation(getId(), getSeverity(),
                    "Entity '" + entity.getName() + "' appears to be immutable or read-heavy. " +
                    "Consider adding @ReadOnly annotation if the entity is never modified after creation."));
            }
            
            // Heuristic 4: Entities with only ManyToOne relationships (likely reference data)
            if (hasManyToOne && !hasCollections && relationshipCount >= 2) {
                violations.add(new Violation(getId(), getSeverity(),
                    "Entity '" + entity.getName() + "' has reference data characteristics. " +
                    "Consider @Cacheable with cache type SOFT or WEAK for memory-sensitive environments."));
            }
        }
        
        return violations;
    }
}