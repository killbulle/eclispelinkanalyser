package com.eclipselink.analyzer.demo.l2_relationships.element;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ElementCollection;
import java.util.List;

@Entity
public class L2ProjectEC {
    @Id
    private Long id;

    @ElementCollection
    private List<String> tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
