package com.eclipselink.analyzer.demo.l6_antipatterns.cartesian;

import com.eclipselink.analyzer.demo.l6_antipatterns.circular.L6CyclicA;
import com.eclipselink.analyzer.demo.l6_antipatterns.circular.L6CyclicB;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.FetchType;
import java.util.List;

@Entity
public class L6CartesianParent {
    @Id
    private Long id;

    @OneToMany(fetch = FetchType.EAGER)
    private List<L6CyclicA> listA;

    @OneToMany(fetch = FetchType.EAGER)
    private List<L6CyclicB> listB;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
