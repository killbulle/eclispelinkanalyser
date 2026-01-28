package com.eclipselink.analyzer.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;

public class TransformationMappingRule implements MappingRule {
    @Override
    public String getId() {
        return "TRANSFORMATION_MAPPING";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Checks for EclipseLink Transformation Mappings.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        Map<String, AttributeMetadata> attributes = entity.getAttributes();

        if (attributes == null)
            return violations;

        for (AttributeMetadata attr : attributes.values()) {
            if (attr.isTransformationMapping()) {
                violations.add(new Violation(
                        "TRANSFORMATION_MAPPING",
                        "INFO",
                        String.format(
                                "Attribute '%s' uses @Transformation. This is a complex mapping type. Consider standard @PostLoad/@PrePersist or a generic @Converter if possible.",
                                attr.getName())));
            }
        }
        return violations;
    }
}
