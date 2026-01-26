package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexRule implements MappingRule {
    @Override
    public String getId() {
        return "INDEX_OPTIMIZATION";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Recommends database indexes based on relationships and query patterns.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        Map<String, AttributeMetadata> attributes = entity.getAttributes();
        List<RelationshipMetadata> relationships = entity.getRelationships();
        
        // Check 1: Foreign key indexes for relationships
        if (relationships != null) {
            for (RelationshipMetadata rel : relationships) {
                String mappingType = rel.getMappingType();
                boolean isOwningSide = rel.isOwningSide();
                
                // Owning side of relationships typically have foreign keys
                if (isOwningSide && (mappingType.equals("ManyToOne") || mappingType.equals("OneToOne"))) {
                    violations.add(new Violation(getId(), "INFO",
                        "Relationship '" + rel.getAttributeName() + "' in entity '" + entity.getName() + 
                        "' creates a foreign key. Add database index for this column."));
                }
                
                // Join tables for ManyToMany relationships
                if (mappingType.equals("ManyToMany") && isOwningSide) {
                    violations.add(new Violation(getId(), "INFO",
                        "ManyToMany relationship '" + rel.getAttributeName() + "' uses join table. " +
                        "Add indexes on both foreign key columns in the join table."));
                }
                
                // Bidirectional relationships may need indexes on both sides
                if (rel.getMappedBy() != null && !rel.getMappedBy().isEmpty()) {
                    violations.add(new Violation(getId(), "INFO",
                        "Bidirectional relationship '" + rel.getAttributeName() + "' has inverse side mapped by '" + 
                        rel.getMappedBy() + "'. Ensure both sides have appropriate indexes."));
                }
            }
        }
        
        // Check 2: Indexes for frequently queried attributes
        if (attributes != null) {
            for (AttributeMetadata attr : attributes.values()) {
                // Primary key already indexed
                if (attr.isId()) {
                    continue;
                }
                
                // Unique constraints imply indexes
                if (attr.isUnique()) {
                    violations.add(new Violation(getId(), "INFO",
                        "Unique attribute '" + attr.getName() + "' in entity '" + entity.getName() + 
                        "' should have a unique index."));
                }
                
                // Version field for optimistic locking
                if (attr.isVersion()) {
                    violations.add(new Violation(getId(), "INFO",
                        "Version attribute '" + attr.getName() + "' is used in WHERE clauses for optimistic locking. " +
                        "Consider adding an index if version queries are frequent."));
                }
                
                // Temporal fields often used in range queries
                if (attr.isTemporal()) {
                    violations.add(new Violation(getId(), "INFO",
                        "Temporal attribute '" + attr.getName() + "' is often used in date range queries. " +
                        "Consider adding an index for time-based queries."));
                }
                
                // Boolean fields with low selectivity
                if (attr.getJavaType() != null && attr.getJavaType().equals("boolean")) {
                    violations.add(new Violation(getId(), "INFO",
                        "Boolean attribute '" + attr.getName() + "' has low cardinality. " +
                        "Index may not be beneficial unless combined with other columns."));
                }
                
                // Lob fields should not be indexed
                if (attr.isLob()) {
                    violations.add(new Violation(getId(), "WARNING",
                        "Lob attribute '" + attr.getName() + "' should not be indexed. " +
                        "Database indexes on large objects are inefficient."));
                }
            }
            
            // Check for composite index opportunities
            int queryableAttributes = 0;
            for (AttributeMetadata attr : attributes.values()) {
                if (!attr.isLob() && !attr.isTransient() && !attr.isId()) {
                    queryableAttributes++;
                }
            }
            
            if (queryableAttributes >= 5) {
                violations.add(new Violation(getId(), "INFO",
                    "Entity '" + entity.getName() + "' has " + queryableAttributes + " queryable attributes. " +
                    "Consider composite indexes for common query patterns."));
            }
        }
        
        // Check 3: Inheritance strategy impact on indexing
        String inheritanceStrategy = entity.getInheritanceStrategy();
        if (inheritanceStrategy != null && !inheritanceStrategy.isEmpty()) {
            if (inheritanceStrategy.equals("JOINED")) {
                violations.add(new Violation(getId(), "INFO",
                    "JOINED inheritance used. Ensure foreign key columns in subclass tables are indexed."));
            } else if (inheritanceStrategy.equals("SINGLE_TABLE")) {
                violations.add(new Violation(getId(), "INFO",
                    "SINGLE_TABLE inheritance used. Discriminator column '" + 
                    (entity.getDiscriminatorColumn() != null ? entity.getDiscriminatorColumn() : "DTYPE") + 
                    "' should be indexed for efficient subclass filtering."));
            }
        }
        
        // Check 4: EclipseLink specific index annotations
        boolean hasIndexAnnotation = false; // We could add a field for @Index annotation in future
        
        if (!hasIndexAnnotation && relationships != null && relationships.size() >= 3) {
            violations.add(new Violation(getId(), "INFO",
                "Entity '" + entity.getName() + "' has multiple relationships. " +
                "Consider using EclipseLink @Index annotation on foreign key columns."));
        }
        
        return violations;
    }
}