package com.eclipselink.analyzer.demo.l4_specific.batch;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class L4Detail {
    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
