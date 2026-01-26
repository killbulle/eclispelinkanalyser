package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import java.util.ArrayList;
import java.util.List;

public class InheritanceRule implements MappingRule {
    @Override
    public String getId() {
        return "INHERITANCE_ANALYSIS";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Analyzes inheritance mapping strategies and potential issues.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        // Check for abstract entities without explicit inheritance strategy
        if (entity.getType().equals("ABSTRACT_ENTITY")) {
            violations.add(new Violation(getId(), getSeverity(),
                    "Entity '" + entity.getName() + "' is abstract. Ensure proper inheritance strategy (@Inheritance) is defined (SINGLE_TABLE, JOINED, TABLE_PER_CLASS) to avoid mapping issues."));
        }
        
        // Check for entities that might be part of inheritance hierarchy but not marked
        // This is a simple heuristic - in a real analyzer, we would need the full model
        if (entity.getName().toLowerCase().contains("base") || 
            entity.getName().toLowerCase().contains("abstract") ||
            entity.getName().toLowerCase().contains("parent")) {
            violations.add(new Violation(getId(), "WARNING",
                    "Entity '" + entity.getName() + "' has name suggesting it might be a base class. Consider using JPA inheritance mapping if this entity has subclasses."));
        }
        
        return violations;
    }
}