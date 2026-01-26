package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class EagerFetchRule implements MappingRule {
    @Override
    public String getId() {
        return "REL_EAGER_FETCH";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks for EAGER fetching on relationships.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        if (entity.getRelationships() != null) {
            for (RelationshipMetadata rel : entity.getRelationships()) {
                if (!rel.isLazy()) {
                    violations.add(new Violation(getId(), getSeverity(),
                            "Relationship '" + rel.getAttributeName() + "' in " + entity.getName()
                                    + " uses EAGER fetch. Use LAZY for better performance."));
                }
            }
        }
        return violations;
    }
}
