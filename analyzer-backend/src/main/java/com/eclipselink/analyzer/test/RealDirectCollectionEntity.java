package com.eclipselink.analyzer.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.eclipse.persistence.annotations.BasicCollection;
import java.util.Collection;

@Entity
public class RealDirectCollectionEntity {
    @Id
    private Long id;

    @BasicCollection
    private Collection<String> phones;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Collection<String> getPhones() {
        return phones;
    }

    public void setPhones(Collection<String> phones) {
        this.phones = phones;
    }
}
