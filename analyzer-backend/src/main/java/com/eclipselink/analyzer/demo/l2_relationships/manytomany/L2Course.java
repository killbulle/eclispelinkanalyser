package com.eclipselink.analyzer.demo.l2_relationships.manytomany;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.List;

@Entity
public class L2Course {
    @Id
    private Long id;

    @ManyToMany(mappedBy = "courses")
    private List<L2Student> students;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
