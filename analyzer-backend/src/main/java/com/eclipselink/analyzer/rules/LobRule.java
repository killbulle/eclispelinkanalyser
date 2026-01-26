package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LobRule implements MappingRule {
    @Override
    public String getId() {
        return "LOB_ANNOTATION";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks @Lob annotation usage and provides performance recommendations.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();
        Map<String, AttributeMetadata> attributes = entity.getAttributes();
        
        if (attributes != null) {
            for (AttributeMetadata attr : attributes.values()) {
                if (attr.isLob()) {
                    String javaType = attr.getJavaType();
                    String databaseType = attr.getDatabaseType();
                    
                    // Recommendation 1: Lob fields should be lazy loaded
                    violations.add(new Violation(getId(), getSeverity(),
                        "Attribute '" + attr.getName() + "' in entity '" + entity.getName() + 
                        "' uses @Lob annotation. Consider using lazy loading for Lob fields to avoid performance issues."));
                    
                    // Recommendation 2: Check database type compatibility
                    if (databaseType != null && !databaseType.equalsIgnoreCase("BLOB") && 
                        !databaseType.equalsIgnoreCase("CLOB") && !databaseType.equalsIgnoreCase("LONGBLOB") &&
                        !databaseType.equalsIgnoreCase("LONGTEXT")) {
                        violations.add(new Violation(getId(), "INFO",
                            "Attribute '" + attr.getName() + "' has database type '" + databaseType + 
                            "' which may not be optimal for @Lob. Consider using BLOB for byte[] or CLOB for String."));
                    }
                    
                    // Recommendation 3: Large objects should be stored externally
                    if (javaType != null && javaType.equals("byte[]")) {
                        violations.add(new Violation(getId(), "INFO",
                            "Attribute '" + attr.getName() + "' is byte[] with @Lob. For very large files (>1MB), " +
                            "consider storing files externally and keeping only file metadata in database."));
                    }
                    
                    // Recommendation 4: Lob fields should not be in frequent queries
                    if (attr.isId() || attr.isVersion()) {
                        violations.add(new Violation(getId(), "ERROR",
                            "Attribute '" + attr.getName() + "' is marked as @Lob but also used as " +
                            (attr.isId() ? "@Id" : "@Version") + ". Lob fields should not be used as identifiers or version fields."));
                    }
                }
            }
        }
        
        return violations;
    }
}