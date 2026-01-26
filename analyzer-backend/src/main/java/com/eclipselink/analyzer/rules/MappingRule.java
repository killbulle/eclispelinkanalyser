package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import java.util.List;

public interface MappingRule {
    String getId();

    String getSeverity(); // INFO, WARNING, ERROR

    String getDescription();

    List<Violation> check(EntityNode entity);

    class Violation {
        public String ruleId;
        public String severity;
        public String message;

        public Violation(String ruleId, String severity, String message) {
            this.ruleId = ruleId;
            this.severity = severity;
            this.message = message;
        }
    }
}
