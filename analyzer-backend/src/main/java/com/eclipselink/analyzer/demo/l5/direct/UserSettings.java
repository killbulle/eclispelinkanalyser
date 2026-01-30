package com.eclipselink.analyzer.demo.l5.direct;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.eclipse.persistence.annotations.BasicCollection;
import java.util.Collection;

@Entity
public class UserSettings {
    @Id
    private Long id;

    private String username;

    @BasicCollection
    private Collection<String> tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }
}
