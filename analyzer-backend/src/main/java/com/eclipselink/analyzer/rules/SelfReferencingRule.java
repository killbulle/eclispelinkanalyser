package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class SelfReferencingRule implements MappingRule {
    @Override
    public String getId() {
        return "SELF_REFERENCE";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Checks for self-referencing relationships which can cause circular dependencies.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        if (entity.getRelationships() != null) {
            for (RelationshipMetadata rel : entity.getRelationships()) {
                if (rel.getTargetEntity().equals(entity.getName())) {
                    violations.add(new Violation(getId(), getSeverity(),
                            "Relationship '" + rel.getAttributeName() + "' in " + entity.getName() + 
                            " references the same entity type. This can create circular object graphs. Ensure proper handling for serialization and graph traversal to avoid infinite loops."));
                }
            }
        }
        
        return violations;
    }
}