package com.eclipselink.analyzer.demo.l4_specific.cache;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;

@Entity
@Cache(type = CacheType.SOFT, size = 500, expiry = 60000, coordinationType = CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS)
public class L4CachedProduct {
    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
