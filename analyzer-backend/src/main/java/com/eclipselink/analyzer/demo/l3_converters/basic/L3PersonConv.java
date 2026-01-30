package com.eclipselink.analyzer.demo.l3_converters.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class L3PersonConv {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private L3Gender gender;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
