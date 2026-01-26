package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class ForeignKeyIndexRule implements MappingRule {
    @Override
    public String getId() {
        return "FK_INDEX";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks for potential missing foreign key indexes.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        if (entity.getRelationships() != null) {
            for (RelationshipMetadata rel : entity.getRelationships()) {
                if (rel.isOwningSide()) {
                    String mappingType = rel.getMappingType();
                    if (mappingType.equals("OneToOne") || mappingType.equals("ManyToOne") || 
                        mappingType.equals("OneToMany") || mappingType.equals("ManyToMany")) {
                        violations.add(new Violation(getId(), getSeverity(),
                                "Relationship '" + rel.getAttributeName() + "' in " + entity.getName() + 
                                " uses foreign key column(s). Ensure database indexes are created on foreign key columns for better query performance."));
                    }
                }
            }
        }
        
        return violations;
    }
}