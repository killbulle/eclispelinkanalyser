package com.eclipselink.analyzer.demo.l3_converters.serialized;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class L3Config {
    @Id
    private Long id;

    // Default JPA mapping for Serializable is serialization/blob
    private MyConfigData data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static class MyConfigData implements Serializable {
        String key;
        String val;
    }
}
