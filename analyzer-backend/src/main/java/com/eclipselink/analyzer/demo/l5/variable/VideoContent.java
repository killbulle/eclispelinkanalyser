package com.eclipselink.analyzer.demo.l5.variable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class VideoContent implements MediaContent {
    @Id
    private Long id;
    private int duration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
