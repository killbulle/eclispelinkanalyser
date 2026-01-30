package com.eclipselink.analyzer.demo.l2_relationships.onetomany;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class L2Department {
    @Id
    private Long id;

    @OneToMany // Unidirectional for simplicity or add mappedBy if we add L2DeptEmployee
    private List<L2EmployeeOM> employees;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
