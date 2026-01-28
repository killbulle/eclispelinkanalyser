package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * IndirectionPolicyRule - Analyzes EclipseLink indirection policy usage
 * 
 * EclipseLink uses indirection policies for lazy loading:
 * - VALUEHOLDER: Legacy ValueHolderInterface (obsolete, requires explicit
 * coding)
 * - WEAVED: Modern weaving-based indirection (recommended)
 * - TRANSPARENT: Transparent indirection for collections
 * (IndirectList/IndirectSet)
 * - NONE: No indirection (eager loading)
 */
public class IndirectionPolicyRule implements MappingRule {

    @Override
    public String getId() {
        return "INDIRECTION_POLICY";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks EclipseLink indirection policy (ValueHolder, Weaving) for lazy loading best practices.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();

        if (entity.getRelationships() == null)
            return violations;

        for (RelationshipMetadata rel : entity.getRelationships()) {
            String indirectionType = rel.getIndirectionType();

            if (indirectionType == null || indirectionType.isEmpty()) {
                // If lazy but no indirection type specified, info about weaving
                if (rel.isLazy()) {
                    violations.add(new Violation(getId(), "INFO",
                            "Relationship '" + rel.getAttributeName()
                                    + "' is lazy but indirection type is not specified. " +
                                    "Ensure EclipseLink weaving is enabled for proper lazy loading."));
                }
                continue;
            }

            switch (indirectionType.toUpperCase()) {
                case "VALUEHOLDER":
                    // Legacy pattern - recommend migration
                    violations.add(new Violation(getId(), "WARNING",
                            "Relationship '" + rel.getAttributeName()
                                    + "' uses legacy ValueHolderInterface indirection. " +
                                    "Consider migrating to weaving-based or transparent indirection for cleaner code."));
                    break;

                case "WEAVED":
                    // Modern and recommended - no warning
                    break;

                case "TRANSPARENT":
                    // Good for collections
                    if (!isCollectionMapping(rel.getMappingType())) {
                        violations.add(new Violation(getId(), "WARNING",
                                "Relationship '" + rel.getAttributeName()
                                        + "' uses transparent indirection but is not a collection. " +
                                        "Transparent indirection (IndirectList/Set) is designed for OneToMany/ManyToMany."));
                    }
                    break;

                case "NONE":
                    // Eager loading - warn if it's a collection (performance risk)
                    if (isCollectionMapping(rel.getMappingType())) {
                        violations.add(new Violation(getId(), "WARNING",
                                "Collection relationship '" + rel.getAttributeName()
                                        + "' has no indirection (eager loading). " +
                                        "This may cause performance issues with large datasets."));
                    }
                    break;
            }
        }

        return violations;
    }

    private boolean isCollectionMapping(String mappingType) {
        return "OneToMany".equals(mappingType) || "ManyToMany".equals(mappingType);
    }
}
