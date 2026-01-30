package com.eclipselink.analyzer.demo.l5.aggregate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ElementCollection;
import java.util.List;

@Entity
public class Portfolio {
    @Id
    private Long id;

    private String name;

    @ElementCollection
    private List<Holding> holdings;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings;
    }
}
