package com.eclipselink.analyzer.demo.l2_relationships.onetoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class L2Employee {
    @Id
    private Long id;

    @OneToOne
    private L2Address address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
