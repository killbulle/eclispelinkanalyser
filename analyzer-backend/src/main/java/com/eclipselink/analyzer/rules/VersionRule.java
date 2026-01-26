package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VersionRule implements MappingRule {
    @Override
    public String getId() {
        return "VERSION_ANNOTATION";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks @Version annotation usage and ensures proper optimistic locking configuration.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        Map<String, AttributeMetadata> attributes = entity.getAttributes();
        boolean hasVersion = false;
        AttributeMetadata versionAttr = null;
        
        if (attributes != null) {
            for (AttributeMetadata attr : attributes.values()) {
                if (attr.isVersion()) {
                    hasVersion = true;
                    versionAttr = attr;
                    break;
                }
            }
            
            // Check 1: Entity should have a version field for optimistic locking
            if (!hasVersion) {
                violations.add(new Violation(getId(), "INFO",
                    "Entity '" + entity.getName() + "' does not have a @Version field. " +
                    "Consider adding optimistic locking support for concurrent updates."));
                return violations;
            }
            
            // Check 2: Version field type
            if (versionAttr != null) {
                String javaType = versionAttr.getJavaType();
                if (javaType != null) {
                    boolean validType = javaType.equals("java.lang.Long") || javaType.equals("Long") ||
                                       javaType.equals("java.lang.Integer") || javaType.equals("Integer") ||
                                       javaType.equals("java.lang.Short") || javaType.equals("Short") ||
                                       javaType.equals("java.sql.Timestamp") || javaType.equals("java.time.Instant");
                    
                    if (!validType) {
                        violations.add(new Violation(getId(), "ERROR",
                            "Version field '" + versionAttr.getName() + "' has type '" + javaType + 
                            "'. @Version should use Long, Integer, Short, Timestamp, or Instant."));
                    } else {
                        // Recommend Long for large systems
                        if (!javaType.contains("Long") && !javaType.contains("Timestamp") && !javaType.contains("Instant")) {
                            violations.add(new Violation(getId(), "INFO",
                                "Version field '" + versionAttr.getName() + "' uses " + javaType + 
                                ". Consider using Long for systems with high concurrency to avoid overflow."));
                        }
                    }
                }
                
                // Check 3: Version field should not be nullable
                if (versionAttr.isNullable()) {
                    violations.add(new Violation(getId(), "WARNING",
                        "Version field '" + versionAttr.getName() + "' is nullable. " +
                        "@Version fields should be NOT NULL to ensure optimistic locking works correctly."));
                }
                
                // Check 4: Only one version field per entity
                int versionCount = 0;
                for (AttributeMetadata attr : attributes.values()) {
                    if (attr.isVersion()) versionCount++;
                }
                if (versionCount > 1) {
                    violations.add(new Violation(getId(), "ERROR",
                        "Entity '" + entity.getName() + "' has " + versionCount + " @Version fields. " +
                        "Only one @Version field is allowed per entity."));
                }
                
                // Check 5: Version field should not be updatable
                // Note: We don't have updatable flag in AttributeMetadata yet, skip
                
                // Recommendation: Consider timestamp-based versioning for audit purposes
                if (versionAttr.getJavaType() != null && 
                   (versionAttr.getJavaType().contains("Timestamp") || versionAttr.getJavaType().contains("Instant"))) {
                    violations.add(new Violation(getId(), "INFO",
                        "Version field '" + versionAttr.getName() + "' uses timestamp-based versioning. " +
                        "This provides audit information but may have lower concurrency resolution."));
                }
            }
        }
        
        return violations;
    }
}