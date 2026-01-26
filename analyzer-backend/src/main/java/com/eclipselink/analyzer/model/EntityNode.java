package com.eclipselink.analyzer.model;

import java.util.List;
import java.util.Map;

public class EntityNode {
    private String name;
    private String packageName;
    private String type; // ENTITY, ABSTRACT_ENTITY, MAPPED_SUPERCLASS, EMBEDDABLE
    private String parentEntity; // Name of parent entity (for inheritance)
    private String inheritanceStrategy; // JOINED, SINGLE_TABLE, TABLE_PER_CLASS
    private String discriminatorColumn; // Name of discriminator column
    private String discriminatorValue; // Discriminator value for this entity
    private Map<String, AttributeMetadata> attributes; // Name -> Metadata
    private List<RelationshipMetadata> relationships;
    private String dddRole; // AGGREGATE_ROOT, ENTITY, VALUE_OBJECT
    private String aggregateName; // Name of the aggregate this entity belongs to
    private List<String> violations; // List of violation descriptions

    public EntityNode() {
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations;
    }

    public EntityNode(String name, String packageName, String type) {
        this.name = name;
        this.packageName = packageName;
        this.type = type;
    }

    // Getters and Setters
    public String getDddRole() {
        return dddRole;
    }

    public void setDddRole(String dddRole) {
        this.dddRole = dddRole;
    }

    public String getAggregateName() {
        return aggregateName;
    }

    public void setAggregateName(String aggregateName) {
        this.aggregateName = aggregateName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, AttributeMetadata> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, AttributeMetadata> attributes) {
        this.attributes = attributes;
    }

    public List<RelationshipMetadata> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipMetadata> relationships) {
        this.relationships = relationships;
    }

    public String getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(String parentEntity) {
        this.parentEntity = parentEntity;
    }

    public String getInheritanceStrategy() {
        return inheritanceStrategy;
    }

    public void setInheritanceStrategy(String inheritanceStrategy) {
        this.inheritanceStrategy = inheritanceStrategy;
    }

    public String getDiscriminatorColumn() {
        return discriminatorColumn;
    }

    public void setDiscriminatorColumn(String discriminatorColumn) {
        this.discriminatorColumn = discriminatorColumn;
    }

    public String getDiscriminatorValue() {
        return discriminatorValue;
    }

    public void setDiscriminatorValue(String discriminatorValue) {
        this.discriminatorValue = discriminatorValue;
    }
}