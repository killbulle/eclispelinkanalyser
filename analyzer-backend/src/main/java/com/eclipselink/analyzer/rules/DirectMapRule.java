package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * DirectMapRule - Analyzes EclipseLink DirectMapMapping (Key/Value pairs) usage
 * 
 * Checks:
 * - Detects Map<Basic, Basic> relationships
 * - Recommends @ElementCollection for standard JPA
 * - Warns if @DirectMapMapping is used (proprietary) without specific need
 */
public class DirectMapRule implements MappingRule {

    @Override
    public String getId() {
        return "DIRECT_MAP_MAPPING";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Analyzes Map mappings for basic types and recommends standard JPA @ElementCollection.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();

        if (entity.getRelationships() == null)
            return violations;

        for (RelationshipMetadata rel : entity.getRelationships()) {
            // Check if it's a Direct Map Mapping (Map<Basic, Basic>)
            if (rel.isDirectMapMapping()) {
                String keyType = rel.getMapKeyType();
                String valueType = rel.getMapValueType();

                violations.add(new Violation(getId(), "INFO",
                        "Relationship '" + rel.getAttributeName() + "' uses EclipseLink DirectMapMapping (Map<"
                                + keyType + ", " + valueType + ">). " +
                                "Consider using standard JPA @ElementCollection for better portability if advanced EclipseLink features are not used."));

                if ("java.lang.Object".equals(keyType) || "java.lang.Object".equals(valueType)) {
                    violations.add(new Violation(getId(), "WARNING",
                            "DirectMapMapping '" + rel.getAttributeName() + "' uses generic Object type. " +
                                    "Specify concrete types for better performance and type safety."));
                }
            }
        }

        return violations;
    }
}
