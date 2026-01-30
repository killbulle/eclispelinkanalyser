package com.eclipselink.analyzer.model;

public class RelationshipMetadata {
    private String attributeName;
    private String targetEntity;
    private String mappingType; // e.g., OneToOne, OneToMany
    private boolean isOwningSide;
    private String mappedBy;
    private boolean isLazy;
    private boolean cascadePersist;
    private boolean cascadeMerge;
    private boolean cascadeRemove;
    private boolean cascadeRefresh;
    private boolean cascadeDetach;
    private boolean cascadeAll;
    private boolean orphanRemoval;
    private boolean optional;
    private String batchFetchType; // JOIN, EXISTS, IN
    private boolean joinFetch;
    private boolean privateOwned;
    private boolean mutable;
    private boolean readOnly;
    private String indirectionType; // VALUEHOLDER, WEAVED, TRANSPARENT, NONE

    // Phase 1 Mappings
    private boolean isAggregateCollection; // EclipseLink @AggregateCollection
    private boolean isDirectMapMapping; // EclipseLink Key/Value pair (e.g. Map<String, Integer>)
    private boolean isDirectCollection;
    private boolean isVariableOneToOne;
    private boolean isArrayMapping;
    private boolean isNestedTable;
    private boolean structureMapping; // StructureMapping
    private boolean referenceMapping; // ReferenceMapping
    private boolean directToXMLTypeMapping; // DirectToXMLTypeMapping
    private boolean multitenantPrimaryKey; // MultitenantPrimaryKeyMapping
    private boolean unidirectionalOneToMany; // UnidirectionalOneToManyMapping
    private boolean objectArrayMapping; // ObjectArrayMapping

    public boolean isNestedTable() {
        return isNestedTable;
    }

    public void setNestedTable(boolean nestedTable) {
        isNestedTable = nestedTable;
    }

    private String variableDiscriminatorColumn;
    private String arrayStructureName;
    private String mapKeyType; // Type of the map key (if applicable)
    private String mapValueType; // Type of the map value (if applicable)

    public RelationshipMetadata() {
    }

    public RelationshipMetadata(String attributeName, String targetEntity, String mappingType) {
        this.attributeName = attributeName;
        this.targetEntity = targetEntity;
        this.mappingType = mappingType;
    }

    // Getters and Setters
    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getMappingType() {
        return mappingType;
    }

    public void setMappingType(String mappingType) {
        this.mappingType = mappingType;
    }

    public boolean isOwningSide() {
        return isOwningSide;
    }

    public void setOwningSide(boolean owningSide) {
        isOwningSide = owningSide;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public boolean isCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(boolean cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public boolean isCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(boolean cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public boolean isCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(boolean cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    public boolean isCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(boolean cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public boolean isCascadeDetach() {
        return cascadeDetach;
    }

    public void setCascadeDetach(boolean cascadeDetach) {
        this.cascadeDetach = cascadeDetach;
    }

    public boolean isCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(boolean cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public boolean isOrphanRemoval() {
        return orphanRemoval;
    }

    public void setOrphanRemoval(boolean orphanRemoval) {
        this.orphanRemoval = orphanRemoval;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getBatchFetchType() {
        return batchFetchType;
    }

    public void setBatchFetchType(String batchFetchType) {
        this.batchFetchType = batchFetchType;
    }

    public boolean isJoinFetch() {
        return joinFetch;
    }

    public void setJoinFetch(boolean joinFetch) {
        this.joinFetch = joinFetch;
    }

    public boolean isPrivateOwned() {
        return privateOwned;
    }

    public void setPrivateOwned(boolean privateOwned) {
        this.privateOwned = privateOwned;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getIndirectionType() {
        return indirectionType;
    }

    public void setIndirectionType(String indirectionType) {
        this.indirectionType = indirectionType;
    }

    public boolean isAggregateCollection() {
        return isAggregateCollection;
    }

    public void setAggregateCollection(boolean aggregateCollection) {
        isAggregateCollection = aggregateCollection;
    }

    public boolean isDirectMapMapping() {
        return isDirectMapMapping;
    }

    public void setDirectMapMapping(boolean directMapMapping) {
        isDirectMapMapping = directMapMapping;
    }

    public String getMapKeyType() {
        return mapKeyType;
    }

    public void setMapKeyType(String mapKeyType) {
        this.mapKeyType = mapKeyType;
    }

    public String getMapValueType() {
        return mapValueType;
    }

    public void setMapValueType(String mapValueType) {
        this.mapValueType = mapValueType;
    }

    public boolean isDirectCollection() {
        return isDirectCollection;
    }

    public void setDirectCollection(boolean directCollection) {
        isDirectCollection = directCollection;
    }

    public boolean isVariableOneToOne() {
        return isVariableOneToOne;
    }

    public void setVariableOneToOne(boolean variableOneToOne) {
        isVariableOneToOne = variableOneToOne;
    }

    public boolean isArrayMapping() {
        return isArrayMapping;
    }

    public void setArrayMapping(boolean arrayMapping) {
        isArrayMapping = arrayMapping;
    }

    public String getVariableDiscriminatorColumn() {
        return variableDiscriminatorColumn;
    }

    public void setVariableDiscriminatorColumn(String variableDiscriminatorColumn) {
        this.variableDiscriminatorColumn = variableDiscriminatorColumn;
    }

    public String getArrayStructureName() {
        return arrayStructureName;
    }

    public void setArrayStructureName(String arrayStructureName) {
        this.arrayStructureName = arrayStructureName;
    }

    public String getStructureName() {
        return arrayStructureName;
    }

    public void setStructureName(String structureName) {
        this.arrayStructureName = structureName;
    }

    public boolean isStructureMapping() {
        return structureMapping;
    }

    public void setStructureMapping(boolean structureMapping) {
        this.structureMapping = structureMapping;
    }

    public boolean isReferenceMapping() {
        return referenceMapping;
    }

    public void setReferenceMapping(boolean referenceMapping) {
        this.referenceMapping = referenceMapping;
    }

    public boolean isDirectToXMLTypeMapping() {
        return directToXMLTypeMapping;
    }

    public void setDirectToXMLTypeMapping(boolean directToXMLTypeMapping) {
        this.directToXMLTypeMapping = directToXMLTypeMapping;
    }

    public boolean isMultitenantPrimaryKey() {
        return multitenantPrimaryKey;
    }

    public void setMultitenantPrimaryKey(boolean multitenantPrimaryKey) {
        this.multitenantPrimaryKey = multitenantPrimaryKey;
    }

    public boolean isUnidirectionalOneToMany() {
        return unidirectionalOneToMany;
    }

    public void setUnidirectionalOneToMany(boolean unidirectionalOneToMany) {
        this.unidirectionalOneToMany = unidirectionalOneToMany;
    }

    public boolean isObjectArrayMapping() {
        return objectArrayMapping;
    }

    public void setObjectArrayMapping(boolean objectArrayMapping) {
        this.objectArrayMapping = objectArrayMapping;
    }
}
