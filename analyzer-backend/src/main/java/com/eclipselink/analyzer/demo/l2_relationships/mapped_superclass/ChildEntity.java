package com.eclipselink.analyzer.demo.l2_relationships.mapped_superclass;

import javax.persistence.Entity;
import javax.persistence.Basic;

@Entity
public class ChildEntity extends BaseEntity {
    @Basic
    private String specificField;
}
