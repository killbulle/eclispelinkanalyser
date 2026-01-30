package com.eclipselink.analyzer.demo.l5.transform;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.eclipse.persistence.annotations.Transformation;
import org.eclipse.persistence.annotations.ReadTransformer;
import org.eclipse.persistence.annotations.WriteTransformer;
import org.eclipse.persistence.annotations.WriteTransformers;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.mappings.transformers.AttributeTransformer;
import org.eclipse.persistence.mappings.transformers.FieldTransformer;

@Entity
public class GeoCoordinate {
    @Id
    private Long id;

    @Transformation
    @ReadTransformer(transformerClass = CoordinatesTransformer.class)
    // removed columnName to avoid lint error, assuming default or different
    // mechanism
    @WriteTransformers({
            @WriteTransformer(transformerClass = LatitudeTransformer.class, column = @javax.persistence.Column(name = "LAT")),
            @WriteTransformer(transformerClass = LongitudeTransformer.class, column = @javax.persistence.Column(name = "LON"))
    })
    private double[] coordinates;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static class CoordinatesTransformer implements AttributeTransformer {
        public Object buildAttributeValue(Record record, Object object, Session session) {
            return new double[2]; // Dummy
        }

        public void initialize(org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping mapping) {
        }
    }

    public static class LatitudeTransformer implements FieldTransformer {
        public Object buildFieldValue(Object instance, String fieldName, Session session) {
            return 0.0;
        }

        public void initialize(org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping mapping) {
        }
    }

    public static class LongitudeTransformer implements FieldTransformer {
        public Object buildFieldValue(Object instance, String fieldName, Session session) {
            return 0.0;
        }

        public void initialize(org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping mapping) {
        }
    }
}
