package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ObjectTypeConverterRule - Validates EclipseLink @ObjectTypeConverter usage
 * 
 * Checks:
 * - Warns if DataType is not standard (String, Integer)
 * - Ensures conversion values are defined (simulation)
 * - Recommends standard JPA @Converter if mapping is simple
 */
public class ObjectTypeConverterRule implements MappingRule {

    @Override
    public String getId() {
        return "OBJECT_TYPE_CONVERTER";
    }

    @Override
    public String getSeverity() {
        return "INFO";
    }

    @Override
    public String getDescription() {
        return "Analyzes @ObjectTypeConverter usage and suggests standard JPA alternatives where possible.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();

        if (entity.getAttributes() == null)
            return violations;

        for (Map.Entry<String, AttributeMetadata> entry : entity.getAttributes().entrySet()) {
            AttributeMetadata attr = entry.getValue();

            if (attr.isObjectTypeConverter()) {
                String dataType = attr.getObjectTypeDataType();

                // Heuristic: If DataType is String and ObjectType is Enum, suggest standard
                // @Enumerated
                // Note: We'd need to know if the object type is actually an enum, but here we
                // guess/warn generically
                if ("java.lang.String".equals(dataType)) {
                    violations.add(new Violation(getId(), "INFO",
                            "Attribute '" + attr.getName() + "' uses @ObjectTypeConverter with String dataType. " +
                                    "If mapping an Enum, consider standard JPA @Enumerated(EnumType.STRING) for portability."));
                }

                // Warn about matching types
                if (dataType != null && !isStandardDBType(dataType)) {
                    violations.add(new Violation(getId(), "WARNING",
                            "Attribute '" + attr.getName() + "' uses complex type '" + dataType
                                    + "' as converter dataType. " +
                                    "This may cause JDBC binding issues. Standard types (String, Integer) are recommended."));
                }
            }
        }

        return violations;
    }

    private boolean isStandardDBType(String type) {
        return type.contains("String") || type.contains("Integer") || type.contains("Long") ||
                type.contains("BigDecimal") || type.contains("Date") || type.contains("Timestamp");
    }
}
