package com.eclipselink.analyzer.demo.l2_relationships.onetomany;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class L2EmployeeOM {
    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
