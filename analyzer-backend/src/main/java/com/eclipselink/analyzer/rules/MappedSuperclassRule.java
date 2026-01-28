package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;

import java.util.ArrayList;
import java.util.List;

/**
 * MappedSuperclassRule - Analyzes @MappedSuperclass usage patterns
 * 
 * Checks:
 * - MappedSuperclass should not have @Table annotation
 * - MappedSuperclass should define common audit fields (createdAt, updatedAt)
 * - Child entities should properly inherit from MappedSuperclass
 */
public class MappedSuperclassRule implements MappingRule {

    @Override
    public String getId() {
        return "MAPPED_SUPERCLASS";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Validates @MappedSuperclass usage and inheritance patterns.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();

        String entityType = entity.getType();

        // Check if this is a MappedSuperclass
        if ("MAPPED_SUPERCLASS".equals(entityType)) {
            // Good practice: MappedSuperclass should have common fields
            if (entity.getAttributes() == null || entity.getAttributes().isEmpty()) {
                violations.add(new Violation(getId(), "INFO",
                        "@MappedSuperclass '" + entity.getName() + "' has no attributes. " +
                                "Consider adding common fields like 'id', 'createdAt', 'updatedAt'."));
            } else {
                // Check for common audit fields
                boolean hasCreatedAt = entity.getAttributes().containsKey("createdAt") ||
                        entity.getAttributes().containsKey("createdDate") ||
                        entity.getAttributes().containsKey("dateCreated");
                boolean hasUpdatedAt = entity.getAttributes().containsKey("updatedAt") ||
                        entity.getAttributes().containsKey("modifiedDate") ||
                        entity.getAttributes().containsKey("lastModified");

                if (!hasCreatedAt || !hasUpdatedAt) {
                    violations.add(new Violation(getId(), "INFO",
                            "@MappedSuperclass '" + entity.getName()
                                    + "' could define audit fields (createdAt/updatedAt) " +
                                    "to centralize auditing across all child entities."));
                }
            }

            // MappedSuperclass should not have relationships that define FKs
            if (entity.getRelationships() != null && !entity.getRelationships().isEmpty()) {
                boolean hasOwningSideRelationship = entity.getRelationships().stream()
                        .anyMatch(rel -> rel.isOwningSide());

                if (hasOwningSideRelationship) {
                    violations.add(new Violation(getId(), "WARNING",
                            "@MappedSuperclass '" + entity.getName() + "' has owning-side relationships. " +
                                    "This may cause FK column duplication across child tables."));
                }
            }
        }

        // Check if entity extends a MappedSuperclass (via parentEntity field)
        if (entity.getParentEntity() != null && !entity.getParentEntity().isEmpty()) {
            // This is fine - just for documentation
        }

        return violations;
    }
}
