package com.eclipselink.analyzer.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;
import org.eclipse.persistence.annotations.Array;
import org.eclipse.persistence.annotations.Struct;
import java.util.List;

@Entity
@Struct(name = "REAL_ARRAY_STRUCT")
public class RealArrayEntity {
    @Id
    private Long id;

    @Column
    @Array(databaseType = "VARCHAR")
    private List<String> tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
