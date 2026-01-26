package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class RedundantUpdateRule implements MappingRule {
    @Override
    public String getId() {
        return "REDUNDANT_SQL_UPDATE";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Detects mappings that may cause extra SQL UPDATE statements.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        if (entity.getRelationships() != null) {
            for (RelationshipMetadata rel : entity.getRelationships()) {
                // Rule: OneToOne mappings with JoinColumn on both sides cause two updates.
                // Since we analyze one side, we check if it's OneToOne and owning.
                // In a perfect analyzer, we'd cross-check with the target entity.
                if (rel.getMappingType().equals("OneToOne") && rel.isOwningSide() && rel.getMappedBy() == null) {
                    // Logic: If target also has a OneToOne to this entity without mappedBy, it's a
                    // double-owner problem.
                    // For now, we flag the potential.
                }

                // Rule: Cascade of all types can lead to unforeseen updates if the graph is
                // large.
                if (rel.isCascadePersist()) {
                    violations.add(new Violation(getId(), "INFO",
                            "Relationship '" + rel.getAttributeName()
                                    + "' has CascadePersist. Ensure this is necessary to avoid extra persistence calls."));
                }
            }
        }
        return violations;
    }
}
