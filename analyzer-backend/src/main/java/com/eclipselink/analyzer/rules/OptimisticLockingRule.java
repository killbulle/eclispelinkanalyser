package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import java.util.ArrayList;
import java.util.List;

public class OptimisticLockingRule implements MappingRule {
    @Override
    public String getId() {
        return "OPTIMISTIC_LOCKING";
    }

    @Override
    public String getSeverity() {
        return "WARNING";
    }

    @Override
    public String getDescription() {
        return "Checks for missing optimistic locking version field.";
    }

    @Override
    public List<Violation> check(EntityNode entity) {
        List<Violation> violations = new ArrayList<>();

        boolean hasVersion = false;
        for (com.eclipselink.analyzer.model.AttributeMetadata attr : entity.getAttributes().values()) {
            if (attr.isVersion()) {
                hasVersion = true;
                break;
            }
        }

        if (!hasVersion) {
            violations.add(new Violation(getId(), getSeverity(),
                    "Entity '" + entity.getName()
                            + "' does not have a version field for optimistic locking. Consider adding @Version to prevent concurrent modification issues."));
        }

        return violations;
    }
}