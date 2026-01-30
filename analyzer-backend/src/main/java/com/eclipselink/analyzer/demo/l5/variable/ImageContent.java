package com.eclipselink.analyzer.demo.l5.variable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ImageContent implements MediaContent {
    @Id
    private Long id;
    private int width;
    private int height;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
