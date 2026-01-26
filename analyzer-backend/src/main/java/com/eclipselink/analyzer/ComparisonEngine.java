package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComparisonEngine {

    public static class Anomaly {
        public String entityName;
        public String propertyName;
        public String severity; // ERROR, WARNING
        public String message;

        public Anomaly(String entityName, String propertyName, String severity, String message) {
            this.entityName = entityName;
            this.propertyName = propertyName;
            this.severity = severity;
            this.message = message;
        }
    }

    public List<Anomaly> compare(List<EntityNode> entityNodes, Map<String, DDLInspector.TableMetadata> schema) {
        List<Anomaly> anomalies = new ArrayList<>();

        for (EntityNode node : entityNodes) {
            String tableName = node.getName().toUpperCase();
            DDLInspector.TableMetadata table = schema.get(tableName);

            if (table == null) {
                anomalies.add(
                        new Anomaly(node.getName(), null, "ERROR", "Table " + tableName + " not found in database"));
                continue;
            }

            if (node.getAttributes() != null) {
                for (AttributeMetadata attr : node.getAttributes().values()) {
                    String columnName = attr.getColumnName() != null ? attr.getColumnName().toUpperCase()
                            : attr.getName().toUpperCase();
                    if (!table.columns.containsKey(columnName)) {
                        anomalies.add(new Anomaly(node.getName(), attr.getName(), "ERROR",
                                "Column " + columnName + " not found in table " + tableName));
                    }
                }
            }
        }

        return anomalies;
    }
}
