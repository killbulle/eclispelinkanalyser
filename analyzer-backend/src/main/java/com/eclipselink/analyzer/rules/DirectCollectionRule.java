package com.eclipselink.analyzer.rules;

import java.util.ArrayList;
import java.util.List;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;

public class DirectCollectionRule implements MappingRule {
    @Override
    public String getId() {
        return "DIRECT_COLLECTION";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Checks for EclipseLink DirectCollection mappings.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();

        if (relationships == null)
            return violations;

        for (RelationshipMetadata rel : relationships) {
            if (rel.isDirectCollection()) {
                violations.add(new Violation(
                        "DIRECT_COLLECTION",
                        "INFO",
                        String.format(
                                "Relationship '%s' uses EclipseLink DirectCollectionMapping. Consider using standard JPA @ElementCollection for better portability.",
                                rel.getAttributeName())));
            }
        }
        return violations;
    }
}
