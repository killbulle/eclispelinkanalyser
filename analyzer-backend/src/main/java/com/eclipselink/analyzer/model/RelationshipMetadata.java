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
}
