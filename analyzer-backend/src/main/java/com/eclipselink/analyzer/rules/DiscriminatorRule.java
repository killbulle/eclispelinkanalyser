package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import java.util.ArrayList;
import java.util.List;

public class DiscriminatorRule implements MappingRule {
    @Override
    public String getId() {
        return "DISCRIMINATOR_USAGE";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks discriminator column and value usage in inheritance hierarchies.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        String inheritanceStrategy = entity.getInheritanceStrategy();
        String discriminatorColumn = entity.getDiscriminatorColumn();
        String discriminatorValue = entity.getDiscriminatorValue();
        String parentEntity = entity.getParentEntity();
        
        // Check 1: Discriminator column usage
        if (inheritanceStrategy != null && !inheritanceStrategy.isEmpty()) {
            if (inheritanceStrategy.equals("SINGLE_TABLE")) {
                // SINGLE_TABLE requires discriminator column
                if (discriminatorColumn == null || discriminatorColumn.isEmpty()) {
                    violations.add(new Violation(getId(), "ERROR",
                        "Entity '" + entity.getName() + "' uses SINGLE_TABLE inheritance but has no discriminator column. " +
                        "Add @DiscriminatorColumn annotation."));
                } else {
                    // Validate discriminator column name
                    if (discriminatorColumn.length() > 31) {
                        violations.add(new Violation(getId(), "WARNING",
                            "Discriminator column '" + discriminatorColumn + "' is too long (>31 chars). " +
                            "Some databases have column name length restrictions."));
                    }
                    
                    if (discriminatorColumn.equals("DTYPE")) {
                        violations.add(new Violation(getId(), "INFO",
                            "Entity '" + entity.getName() + "' uses default discriminator column 'DTYPE'. " +
                            "Consider a more descriptive name if multiple inheritance hierarchies exist."));
                    }
                }
                
                // Discriminator value should be specified for concrete entities
                if (discriminatorValue == null || discriminatorValue.isEmpty()) {
                    if (!entity.getType().equals("ABSTRACT_ENTITY") && !entity.getType().equals("MAPPED_SUPERCLASS")) {
                        violations.add(new Violation(getId(), "WARNING",
                            "Concrete entity '" + entity.getName() + "' has no discriminator value. " +
                            "Add @DiscriminatorValue annotation."));
                    }
                } else {
                    // Validate discriminator value
                    if (discriminatorValue.length() > 50) {
                        violations.add(new Violation(getId(), "WARNING",
                            "Discriminator value '" + discriminatorValue + "' is too long (>50 chars). " +
                            "Keep discriminator values short for efficiency."));
                    }
                    
                    if (discriminatorValue.equals(entity.getName())) {
                        violations.add(new Violation(getId(), "INFO",
                            "Entity '" + entity.getName() + "' uses entity name as discriminator value. " +
                            "This is common but consider shorter values for better performance."));
                    }
                }
            } else if (inheritanceStrategy.equals("JOINED")) {
                // JOINED inheritance may use discriminator column (optional)
                if (discriminatorColumn != null && !discriminatorColumn.isEmpty()) {
                    violations.add(new Violation(getId(), "INFO",
                        "Entity '" + entity.getName() + "' uses discriminator column with JOINED inheritance. " +
                        "Discriminator column is optional for JOINED strategy."));
                }
            } else if (inheritanceStrategy.equals("TABLE_PER_CLASS")) {
                // TABLE_PER_CLASS does not use discriminator column
                if (discriminatorColumn != null && !discriminatorColumn.isEmpty()) {
                    violations.add(new Violation(getId(), "WARNING",
                        "Entity '" + entity.getName() + "' defines discriminator column but uses TABLE_PER_CLASS. " +
                        "Discriminator column is not used with TABLE_PER_CLASS inheritance."));
                }
            }
        }
        
        // Check 2: Discriminator value conflicts
        if (discriminatorValue != null && !discriminatorValue.isEmpty()) {
            // Check for common problematic values
            if (discriminatorValue.equals("null") || discriminatorValue.equals("NULL")) {
                violations.add(new Violation(getId(), "ERROR",
                    "Discriminator value '" + discriminatorValue + "' is a reserved SQL keyword. " +
                    "Choose a different discriminator value."));
            }
            
            if (discriminatorValue.contains(" ") || discriminatorValue.contains("-")) {
                violations.add(new Violation(getId(), "INFO",
                    "Discriminator value '" + discriminatorValue + "' contains special characters. " +
                    "Use alphanumeric characters only for better compatibility."));
            }
        }
        
        // Check 3: Inheritance hierarchy without discriminator
        if (parentEntity != null && !parentEntity.isEmpty()) {
            // Child entity
            if (discriminatorColumn != null && !discriminatorColumn.isEmpty() && 
                (discriminatorValue == null || discriminatorValue.isEmpty())) {
                violations.add(new Violation(getId(), "INFO",
                    "Child entity '" + entity.getName() + "' inherits discriminator column '" + 
                    discriminatorColumn + "' but does not define its own discriminator value. " +
                    "Ensure parent entity defines discriminator values for all subclasses."));
            }
        } else {
            // Root entity
            if (discriminatorColumn != null && !discriminatorColumn.isEmpty() && 
                inheritanceStrategy == null) {
                violations.add(new Violation(getId(), "WARNING",
                    "Root entity '" + entity.getName() + "' defines discriminator column but no inheritance strategy. " +
                    "Discriminator column is only used with inheritance."));
            }
        }
        
        // Check 4: EclipseLink specific discriminator types
        if (discriminatorColumn != null && !discriminatorColumn.isEmpty()) {
            // EclipseLink supports CHAR, INTEGER discriminator types
            // We could add discriminatorType field to EntityNode in the future
            violations.add(new Violation(getId(), "INFO",
                "Entity '" + entity.getName() + "' uses discriminator column '" + discriminatorColumn + "'. " +
                "Consider using CHAR discriminator type (default) for single-character values for better performance."));
        }
        
        return violations;
    }
}