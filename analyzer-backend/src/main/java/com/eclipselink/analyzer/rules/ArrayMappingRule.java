package com.eclipselink.analyzer.rules;

import java.util.ArrayList;
import java.util.List;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;

public class ArrayMappingRule implements MappingRule {
    @Override
    public String getId() {
        return "ARRAY_MAPPING";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Checks for Database Array mappings (Oracle/PG).";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        List<RelationshipMetadata> relationships = entity.getRelationships();

        if (relationships == null)
            return violations;

        for (RelationshipMetadata rel : relationships) {
            if (rel.isArrayMapping()) {
                violations.add(new Violation(
                        "ARRAY_MAPPING",
                        "INFO",
                        String.format(
                                "Relationship '%s' uses Database Array Mapping (e.g. VARRAY/ARRAY). Be aware this is highly database vendor specific (Oracle/PostgreSQL) and may limit portability.",
                                rel.getAttributeName())));
            }
        }
        return violations;
    }
}
