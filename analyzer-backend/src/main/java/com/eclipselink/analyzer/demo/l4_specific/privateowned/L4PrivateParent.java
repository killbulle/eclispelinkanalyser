package com.eclipselink.analyzer.demo.l4_specific.privateowned;

import javax.persistence.*;
import org.eclipse.persistence.annotations.PrivateOwned;
import java.util.ArrayList;
import java.util.List;

@Entity
public class L4PrivateParent {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @PrivateOwned // EclipseLink specific Orphan Removal
    private List<L4PrivateChild> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
