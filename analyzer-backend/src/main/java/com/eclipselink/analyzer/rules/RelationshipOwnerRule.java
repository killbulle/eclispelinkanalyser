package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class RelationshipOwnerRule implements MappingRule {
    @Override
    public String getId() {
        return "REL_OWNER_VALIDATION";
    }

    @Override
    public String getSeverity() {
        return "ERROR";
    }

    @Override
    public String getDescription() {
        return "Validates bidirectional relationship ownership side.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        if (entity.getRelationships() != null) {
            for (RelationshipMetadata rel : entity.getRelationships()) {
                // Rule: If it's a OneToMany mapping, it SHOULD normally be the inverse side
                // (mappedBy).
                // If it's the owning side in a OneToMany, it usually means a JoinTable is used,
                // which might be intentional but often is a missing 'mappedBy'.
                if (rel.getMappingType().equals("OneToMany") && rel.isOwningSide()) {
                    violations.add(new Violation(getId(), "WARNING",
                            "Relationship '" + rel.getAttributeName()
                                    + "' is a OneToMany without 'mappedBy'. This will use a Join Table. If not intended, add 'mappedBy'."));
                }

                // Rule: ManyToMany mappings without mappedBy on either side are invalid in JPA
                // (multiple owning sides).
                // However, we can only check one entity at a time here.
                // A better check would be in the ComparisonEngine or a global validator.
            }
        }
        return violations;
    }
}
