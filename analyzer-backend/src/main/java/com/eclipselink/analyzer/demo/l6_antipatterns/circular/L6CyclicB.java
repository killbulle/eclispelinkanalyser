package com.eclipselink.analyzer.demo.l6_antipatterns.circular;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class L6CyclicB {
    @Id
    private Long id;

    @OneToOne
    private L6CyclicA a;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
