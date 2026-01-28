package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

/**
 * CacheRule - Recommends EclipseLink caching annotations for performance
 * optimization.
 * 
 * Analyzes:
 * - @Cacheable usage
 * - @ReadOnly for immutable entities
 * - Cache coordination type (SEND_OBJECT_CHANGES, INVALIDATE_CHANGED_OBJECTS,
 * etc.)
 * - Cache expiry settings
 */
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

        // Check for cache configuration
        String cacheType = entity.getCacheType();
        String cacheCoordination = entity.getCacheCoordinationType();
        Integer cacheExpiry = entity.getCacheExpiry();

        // Warning: Clustered environments need cache coordination
        if (cacheType != null && !cacheType.isEmpty() &&
                (cacheCoordination == null || cacheCoordination.isEmpty())) {
            violations.add(new Violation(getId(), "WARNING",
                    "Entity '" + entity.getName() + "' uses @Cache but has no coordination type. " +
                            "In clustered environments, set coordinationType (e.g., INVALIDATE_CHANGED_OBJECTS) to avoid stale data."));
        }

        // Warning: Very long expiry on cached data
        if (cacheType != null && cacheExpiry != null && cacheExpiry > 3600000) { // > 1 hour
            violations.add(new Violation(getId(), "INFO",
                    "Entity '" + entity.getName() + "' has a cache expiry of " + (cacheExpiry / 1000) + " seconds. " +
                            "For frequently changing data, consider a shorter expiry to reduce stale reads."));
        }

        if (entity.getRelationships() != null) {
            int relationshipCount = entity.getRelationships().size();
            boolean hasCollections = false;
            boolean hasManyToOne = false;

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
            if (relationshipCount >= 3 && cacheType == null) {
                violations.add(new Violation(getId(), getSeverity(),
                        "Entity '" + entity.getName() + "' has " + relationshipCount
                                + " relationships and is a good candidate for @Cacheable annotation."));
            }

            // Heuristic 2: Entities with collection relationships benefit from caching
            if (hasCollections && relationshipCount >= 2 && cacheType == null) {
                violations.add(new Violation(getId(), getSeverity(),
                        "Entity '" + entity.getName()
                                + "' has collection relationships. Consider using @Cacheable with appropriate cache size."));
            }

            // Heuristic 3: Entities with no relationships might be immutable
            if (relationshipCount == 0 && entity.getAttributes() != null && entity.getAttributes().size() <= 5) {
                violations.add(new Violation(getId(), getSeverity(),
                        "Entity '" + entity.getName() + "' appears immutable. Consider adding @ReadOnly annotation."));
            }

            // Heuristic 4: Reference data pattern
            if (hasManyToOne && !hasCollections && relationshipCount >= 2 && cacheType == null) {
                violations.add(new Violation(getId(), getSeverity(),
                        "Entity '" + entity.getName()
                                + "' has reference data characteristics. Consider @Cacheable with cache type SOFT."));
            }
        }

        return violations;
    }
}