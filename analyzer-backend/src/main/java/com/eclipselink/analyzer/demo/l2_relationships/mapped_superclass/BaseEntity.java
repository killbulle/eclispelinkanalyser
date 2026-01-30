package com.eclipselink.analyzer.demo.l2_relationships.mapped_superclass;

import javax.persistence.MappedSuperclass;
import javax.persistence.Id;
import javax.persistence.Basic;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    protected Long id;

    @Basic
    protected String createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
