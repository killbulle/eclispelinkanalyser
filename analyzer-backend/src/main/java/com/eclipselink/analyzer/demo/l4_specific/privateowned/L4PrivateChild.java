package com.eclipselink.analyzer.demo.l4_specific.privateowned;

import javax.persistence.*;

@Entity
public class L4PrivateChild {
    @Id
    @GeneratedValue
    private Long id;

    private String childName;

    @ManyToOne
    private L4PrivateParent parent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
