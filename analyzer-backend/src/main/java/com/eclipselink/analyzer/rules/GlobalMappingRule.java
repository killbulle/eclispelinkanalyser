package com.eclipselink.analyzer.rules;

import com.eclipselink.analyzer.model.EntityNode;
import java.util.List;

public interface GlobalMappingRule {
    String getId();
    String getSeverity();
    String getDescription();
    List<MappingRule.Violation> checkAll(List<EntityNode> entities);
}