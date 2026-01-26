package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InheritanceStrategyRule implements MappingRule {
    @Override
    public String getId() {
        return "INHERITANCE_STRATEGY";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Analyzes inheritance strategy and provides recommendations for optimal performance.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        
        String parentEntity = entity.getParentEntity();
        String inheritanceStrategy = entity.getInheritanceStrategy();
        String type = entity.getType();
        
        // Check 1: Inheritance hierarchy detection
        if (parentEntity != null && !parentEntity.isEmpty()) {
            // This entity has a parent
            if (inheritanceStrategy == null || inheritanceStrategy.isEmpty()) {
                violations.add(new Violation(getId(), "WARNING",
                    "Entity '" + entity.getName() + "' extends '" + parentEntity + 
                    "' but does not specify inheritance strategy. Add @Inheritance annotation."));
            } else {
                // Validate inheritance strategy
                if (!inheritanceStrategy.equals("JOINED") && !inheritanceStrategy.equals("SINGLE_TABLE") && 
                    !inheritanceStrategy.equals("TABLE_PER_CLASS")) {
                    violations.add(new Violation(getId(), "ERROR",
                        "Entity '" + entity.getName() + "' has invalid inheritance strategy '" + 
                        inheritanceStrategy + "'. Use JOINED, SINGLE_TABLE, or TABLE_PER_CLASS."));
                }
                
                // Strategy-specific recommendations
                Map<String, AttributeMetadata> attributes = entity.getAttributes();
                int attributeCount = (attributes != null) ? attributes.size() : 0;
                
                if (inheritanceStrategy.equals("JOINED")) {
                    // JOINED strategy: good for normalized data, but joins can be expensive
                    violations.add(new Violation(getId(), "INFO",
                        "Entity '" + entity.getName() + "' uses JOINED inheritance. " +
                        "Ensure foreign key indexes are created for performance."));
                    
                    if (attributeCount <= 2) {
                        violations.add(new Violation(getId(), "INFO",
                            "Entity '" + entity.getName() + "' has only " + attributeCount + " attributes. " +
                            "Consider SINGLE_TABLE strategy for simple hierarchies to avoid joins."));
                    }
                } else if (inheritanceStrategy.equals("SINGLE_TABLE")) {
                    // SINGLE_TABLE: good performance but nullable columns
                    violations.add(new Violation(getId(), "INFO",
                        "Entity '" + entity.getName() + "' uses SINGLE_TABLE inheritance. " +
                        "Monitor table growth and consider partitioning for large hierarchies."));
                    
                    if (attributeCount > 10) {
                        violations.add(new Violation(getId(), "WARNING",
                            "Entity '" + entity.getName() + "' has " + attributeCount + " attributes. " +
                            "SINGLE_TABLE may result in many nullable columns. Consider JOINED strategy."));
                    }
                } else if (inheritanceStrategy.equals("TABLE_PER_CLASS")) {
                    // TABLE_PER_CLASS: polymorphic queries use UNION
                    violations.add(new Violation(getId(), "WARNING",
                        "Entity '" + entity.getName() + "' uses TABLE_PER_CLASS inheritance. " +
                        "Polymorphic queries may be slow due to UNION operations. Use with caution."));
                }
            }
        } else {
            // Root entity or non-inheritance entity
            if (inheritanceStrategy != null && !inheritanceStrategy.isEmpty()) {
                // Root entity with inheritance strategy
                if (!type.equals("MAPPED_SUPERCLASS")) {
                    violations.add(new Violation(getId(), "INFO",
                        "Root entity '" + entity.getName() + "' defines inheritance strategy '" + 
                        inheritanceStrategy + "'. Ensure all subclasses use the same strategy."));
                }
            }
        }
        
        // Check 2: MappedSuperclass handling
        if (type.equals("MAPPED_SUPERCLASS")) {
            if (parentEntity != null && !parentEntity.isEmpty()) {
                violations.add(new Violation(getId(), "ERROR",
                    "MappedSuperclass '" + entity.getName() + "' cannot extend another entity. " +
                    "MappedSuperclass is not an entity itself."));
            }
            
            if (inheritanceStrategy != null && !inheritanceStrategy.isEmpty()) {
                violations.add(new Violation(getId(), "WARNING",
                    "MappedSuperclass '" + entity.getName() + "' should not define inheritance strategy. " +
                    "Strategy should be defined on concrete entity subclasses."));
            }
        }
        
        // Check 3: Abstract entity vs concrete entity
        if (type.equals("ABSTRACT_ENTITY")) {
            violations.add(new Violation(getId(), "INFO",
                "Abstract entity '" + entity.getName() + "' cannot be instantiated. " +
                "Ensure all concrete subclasses properly implement the hierarchy."));
        }
        
        return violations;
    }
}