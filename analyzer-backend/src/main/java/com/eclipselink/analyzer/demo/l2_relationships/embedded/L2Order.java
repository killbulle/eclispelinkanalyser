package com.eclipselink.analyzer.demo.l2_relationships.embedded;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Embedded;

@Entity
public class L2Order {
    @Id
    private Long id;

    @Embedded
    private L2BillingAddress billingAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
