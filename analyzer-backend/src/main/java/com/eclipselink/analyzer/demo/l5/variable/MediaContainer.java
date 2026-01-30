package com.eclipselink.analyzer.demo.l5.variable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.DiscriminatorColumn;
import org.eclipse.persistence.annotations.VariableOneToOne;
import org.eclipse.persistence.annotations.DiscriminatorClass;

@Entity
public class MediaContainer {
    @Id
    private Long id;
    private String name;

    @VariableOneToOne(targetInterface = MediaContent.class, discriminatorColumn = @DiscriminatorColumn(name = "MEDIA_TYPE"), discriminatorClasses = {
            @DiscriminatorClass(discriminator = "IMG", value = ImageContent.class),
            @DiscriminatorClass(discriminator = "VID", value = VideoContent.class)
    })
    private MediaContent media;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MediaContent getMedia() {
        return media;
    }

    public void setMedia(MediaContent media) {
        this.media = media;
    }
}
