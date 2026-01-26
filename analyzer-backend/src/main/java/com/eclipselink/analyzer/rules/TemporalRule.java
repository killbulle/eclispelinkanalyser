package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemporalRule implements MappingRule {
    @Override
    public String getId() {
        return "TEMPORAL_ANNOTATION";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks @Temporal annotation usage and ensures proper temporal type handling.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        Map<String, AttributeMetadata> attributes = entity.getAttributes();
        
        if (attributes != null) {
            for (AttributeMetadata attr : attributes.values()) {
                if (attr.isTemporal()) {
                    String javaType = attr.getJavaType();
                    String temporalType = attr.getTemporalType();
                    
                    // Check 1: Java type compatibility with @Temporal
                    if (javaType != null && !javaType.equals("java.util.Date") && 
                        !javaType.equals("java.util.Calendar") && !javaType.equals("java.time.LocalDate") &&
                        !javaType.equals("java.time.LocalDateTime") && !javaType.equals("java.time.Instant")) {
                        violations.add(new Violation(getId(), "ERROR",
                            "Attribute '" + attr.getName() + "' uses @Temporal but has incompatible Java type '" + 
                            javaType + "'. @Temporal should be used with Date, Calendar, or Java 8 time types."));
                    }
                    
                    // Check 2: Temporal type specified
                    if (temporalType == null || temporalType.isEmpty()) {
                        violations.add(new Violation(getId(), "ERROR",
                            "Attribute '" + attr.getName() + "' uses @Temporal without specifying TemporalType. " +
                            "Specify DATE, TIME, or TIMESTAMP."));
                    } else {
                        // Check 3: Temporal type matches Java type
                        if (javaType != null) {
                            if (javaType.contains("LocalDate") && !temporalType.equals("DATE")) {
                                violations.add(new Violation(getId(), "INFO",
                                    "Attribute '" + attr.getName() + "' is LocalDate but TemporalType is '" + 
                                    temporalType + "'. Consider using TemporalType.DATE for LocalDate."));
                            }
                            if (javaType.contains("LocalDateTime") && !temporalType.equals("TIMESTAMP")) {
                                violations.add(new Violation(getId(), "INFO",
                                    "Attribute '" + attr.getName() + "' is LocalDateTime but TemporalType is '" + 
                                    temporalType + "'. Consider using TemporalType.TIMESTAMP for LocalDateTime."));
                            }
                        }
                    }
                    
                    // Recommendation: Use Java 8 time API
                    if (javaType != null && (javaType.equals("java.util.Date") || javaType.equals("java.util.Calendar"))) {
                        violations.add(new Violation(getId(), "INFO",
                            "Attribute '" + attr.getName() + "' uses legacy Date/Calendar with @Temporal. " +
                            "Consider migrating to java.time API (LocalDate, LocalDateTime, Instant) for better type safety."));
                    }
                    
                    // Check 4: @Temporal on @Version fields
                    if (attr.isVersion()) {
                        violations.add(new Violation(getId(), "WARNING",
                            "Attribute '" + attr.getName() + "' uses both @Temporal and @Version. " +
                            "Ensure the temporal type is TIMESTAMP for accurate versioning."));
                    }
                }
            }
        }
        
        return violations;
    }
}