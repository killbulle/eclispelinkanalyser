package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;

public class LargeCollectionRule implements MappingRule {
    @Override
    public String getId() {
        return "LARGE_COLLECTION";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks for collections that could grow large without pagination.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        if (entity.getRelationships() != null) {
            for (RelationshipMetadata rel : entity.getRelationships()) {
                String mappingType = rel.getMappingType();
                if (mappingType.equals("OneToMany") || mappingType.equals("ManyToMany")) {
                    violations.add(new Violation(getId(), getSeverity(),
                            "Relationship '" + rel.getAttributeName() + "' in " + entity.getName() + 
                            " is a collection mapping that could return many objects. Consider implementing pagination (@BatchFetch, @FetchJoin, or query limits) to avoid loading too many objects into memory."));
                }
            }
        }
        
        return violations;
    }
}