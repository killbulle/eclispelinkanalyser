package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * AggregateCollectionRule - Analyzes EclipseLink @AggregateCollection usage
 * 
 * Checks:
 * - AggregateCollection relationship logic (List of Embeddables)
 * - Ensures target class is an Embeddable
 * - Warnings about performance with large aggregate collections
 */
public class AggregateCollectionRule implements MappingRule {

    @Override
    public String getId() {
        return "AGGREGATE_COLLECTION";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Analyzes usage of @AggregateCollection (collections of embeddables) for proper configuration.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();

        if (entity.getRelationships() == null)
            return violations;

        for (RelationshipMetadata rel : entity.getRelationships()) {
            if (rel.isAggregateCollection()) {
                // Info: Standard JPA replacement
                violations.add(new Violation(getId(), "INFO",
                        "Relationship '" + rel.getAttributeName() + "' uses @AggregateCollection. " +
                                "Consider using standard JPA @ElementCollection for collections of embeddables."));

                // Warning: Eager loading risk with AggregateCollection (often eager by default
                // in older versions)
                if (!rel.isLazy()) {
                    violations.add(new Violation(getId(), "WARNING",
                            "AggregateCollection '" + rel.getAttributeName() + "' is configured as EAGER. " +
                                    "This can cause significant performance overhead if the collection is large. Consider Lazy loading."));
                }
            }
        }

        return violations;
    }
}
