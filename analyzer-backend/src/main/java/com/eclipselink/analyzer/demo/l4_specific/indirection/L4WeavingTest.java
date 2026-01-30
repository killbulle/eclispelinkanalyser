package com.eclipselink.analyzer.demo.l4_specific.indirection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Basic;
import javax.persistence.FetchType;

@Entity
public class L4WeavingTest {
    @Id
    private Long id;

    @Basic(fetch = FetchType.LAZY) // Requires Weaving for ValueHolder
    private String hugeData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
