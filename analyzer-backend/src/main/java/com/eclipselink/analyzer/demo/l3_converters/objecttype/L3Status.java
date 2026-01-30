package com.eclipselink.analyzer.demo.l3_converters.objecttype;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;

@Entity
@ObjectTypeConverter(name = "statusConverter", dataType = java.lang.String.class, objectType = java.lang.String.class, conversionValues = {
        @ConversionValue(dataValue = "A", objectValue = "Active"),
        @ConversionValue(dataValue = "I", objectValue = "Inactive")
})
public class L3Status {
    @Id
    private Long id;

    @Convert("statusConverter")
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
