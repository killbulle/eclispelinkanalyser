package com.eclipselink.analyzer.demo.l2_relationships.onetoone;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class L2Address {
    @Id
    private Long id;
    private String street;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
