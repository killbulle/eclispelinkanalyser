package com.eclipselink.analyzer.rules;

import java.util.ArrayList;
import java.util.List;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;

public class VariableOneToOneRule implements MappingRule {
    @Override
    public String getId() {
        return "VARIABLE_ONE_TO_ONE";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks for VariableOneToOne mappings which lack FK constraints.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();

        if (relationships == null)
            return violations;

        for (RelationshipMetadata rel : relationships) {
            if (rel.isVariableOneToOne()) {
                violations.add(new Violation(
                        "VARIABLE_ONE_TO_ONE",
                        "WARNING",
                        String.format(
                                "Relationship '%s' uses VariableOneToOneMapping. This does not support foreign key constraints in the database and can lead to data integrity issues. Consider TABLE_PER_CLASS or JOINED inheritance.",
                                rel.getAttributeName())));
            }
        }
        return violations;
    }
}
