package com.eclipselink.analyzer.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;

public class ConverterRule implements MappingRule {
    @Override
    public String getId() {
        return "CONVERTER_RULE";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks for legacy or problematic EclipseLink converters.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        Map<String, AttributeMetadata> attributes = entity.getAttributes();

        if (attributes == null)
            return violations;

        for (AttributeMetadata attr : attributes.values()) {
            if (attr.isSerializedObjectConverter()) {
                violations.add(new Violation(
                        "SERIALIZED_OBJECT_CONVERTER",
                        "WARNING",
                        String.format(
                                "Attribute '%s' uses @SerializedObjectConverter. Storing serialized Java objects in the database is brittle, hard to query, and causes versioning issues. Prefer JSON or structured data.",
                                attr.getName())));
            }
            if (attr.isTypeConversionConverter()) {
                violations.add(new Violation(
                        "TYPE_CONVERSION_CONVERTER",
                        "INFO",
                        String.format(
                                "Attribute '%s' uses legacy @TypeConverter. Ensure the conversion is compatible across database migrations.",
                                attr.getName())));
            }
        }
        return violations;
    }
}
