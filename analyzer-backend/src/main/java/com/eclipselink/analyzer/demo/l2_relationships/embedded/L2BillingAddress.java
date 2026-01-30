package com.eclipselink.analyzer.demo.l2_relationships.embedded;

import javax.persistence.Embeddable;

@Embeddable
public class L2BillingAddress {
    private String city;
    private String zip;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
