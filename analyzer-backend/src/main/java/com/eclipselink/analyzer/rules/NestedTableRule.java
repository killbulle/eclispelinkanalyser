package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class NestedTableRule implements MappingRule {
    @Override
    public String getId() {
        return "NESTED_TABLE_MAPPING";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Checks for NestedTable mappings (Oracle specific).";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();

        if (relationships != null) {
            for (RelationshipMetadata rel : relationships) {
                if (rel.isNestedTable()) {
                    violations.add(new Violation(
                            getId(),
                            getSeverity(),
                            "Relationship '" + rel.getAttributeName()
                                    + "' uses NestedTableMapping. This is highly database-specific (Oracle) and limits portability. Consider standard OneToMany."));
                }
            }
        }
        return violations;
    }
}
