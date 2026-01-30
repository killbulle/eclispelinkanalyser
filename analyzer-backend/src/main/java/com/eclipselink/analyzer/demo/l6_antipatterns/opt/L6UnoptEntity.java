package com.eclipselink.analyzer.demo.l6_antipatterns.opt;

import com.eclipselink.analyzer.demo.l6_antipatterns.circular.L6CyclicA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany; // Default is LAZY, but let's make it EAGER to trigger warning if we have rule
import javax.persistence.FetchType;
import java.util.List;

@Entity
public class L6UnoptEntity {
    @Id
    private Long id;

    @OneToMany(fetch = FetchType.EAGER)
    private List<L6CyclicA> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
