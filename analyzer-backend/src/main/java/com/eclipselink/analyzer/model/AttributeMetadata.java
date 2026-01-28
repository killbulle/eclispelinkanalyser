package com.eclipselink.analyzer.model;

public class AttributeMetadata {
    private String name;
    private String javaType;
    private String databaseType;
    private String columnName;
    private boolean isLob;
    private boolean isTemporal;
    private String temporalType; // DATE, TIME, TIMESTAMP
    private boolean isVersion;
    private boolean isEnumerated;
    private boolean isConvert;
    private String converterName;
    private boolean isGeneratedValue;
    private String generationStrategy; // AUTO, IDENTITY, SEQUENCE, TABLE
    private boolean isNullable;
    private boolean isUnique;
    private boolean isId;
    private boolean isTransient;
    private boolean isBasic;
    private boolean isElementCollection;

    // EclipseLink Converter Metadata
    private String converterType; // OBJECT_TYPE, TYPE_CONVERSION, SERIALIZED, CUSTOM
    private boolean isObjectTypeConverter;
    private boolean isTypeConversionConverter;
    private boolean isSerializedObjectConverter;

    private boolean isTransformationMapping;
    private String transformationMethodName;
    private String objectTypeDataType; // e.g. java.lang.String (DB side)
    private String objectTypeObjectType; // e.g. com.my.Enum (Java side)

    public AttributeMetadata() {
    }

    public AttributeMetadata(String name, String javaType, String databaseType, String columnName) {
        this.name = name;
        this.javaType = javaType;
        this.databaseType = databaseType;
        this.columnName = columnName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public boolean isLob() {
        return isLob;
    }

    public void setLob(boolean lob) {
        isLob = lob;
    }

    public boolean isTemporal() {
        return isTemporal;
    }

    public void setTemporal(boolean temporal) {
        isTemporal = temporal;
    }

    public String getTemporalType() {
        return temporalType;
    }

    public void setTemporalType(String temporalType) {
        this.temporalType = temporalType;
    }

    public boolean isVersion() {
        return isVersion;
    }

    public void setVersion(boolean version) {
        isVersion = version;
    }

    public boolean isEnumerated() {
        return isEnumerated;
    }

    public void setEnumerated(boolean enumerated) {
        isEnumerated = enumerated;
    }

    public boolean isConvert() {
        return isConvert;
    }

    public void setConvert(boolean convert) {
        isConvert = convert;
    }

    public String getConverterName() {
        return converterName;
    }

    public void setConverterName(String converterName) {
        this.converterName = converterName;
    }

    public boolean isGeneratedValue() {
        return isGeneratedValue;
    }

    public void setGeneratedValue(boolean generatedValue) {
        isGeneratedValue = generatedValue;
    }

    public String getGenerationStrategy() {
        return generationStrategy;
    }

    public void setGenerationStrategy(String generationStrategy) {
        this.generationStrategy = generationStrategy;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public boolean isId() {
        return isId;
    }

    public void setId(boolean id) {
        isId = id;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public void setTransient(boolean transient_) {
        isTransient = transient_;
    }

    public boolean isBasic() {
        return isBasic;
    }

    public void setBasic(boolean basic) {
        isBasic = basic;
    }

    public boolean isElementCollection() {
        return isElementCollection;
    }

    public void setElementCollection(boolean elementCollection) {
        isElementCollection = elementCollection;
    }

    public String getConverterType() {
        return converterType;
    }

    public void setConverterType(String converterType) {
        this.converterType = converterType;
    }

    public boolean isObjectTypeConverter() {
        return isObjectTypeConverter;
    }

    public void setObjectTypeConverter(boolean objectTypeConverter) {
        isObjectTypeConverter = objectTypeConverter;
    }

    public String getObjectTypeDataType() {
        return objectTypeDataType;
    }

    public void setObjectTypeDataType(String objectTypeDataType) {
        this.objectTypeDataType = objectTypeDataType;
    }

    public String getObjectTypeObjectType() {
        return objectTypeObjectType;
    }

    public void setObjectTypeObjectType(String objectTypeObjectType) {
        this.objectTypeObjectType = objectTypeObjectType;
    }

    public boolean isTypeConversionConverter() {
        return isTypeConversionConverter;
    }

    public void setTypeConversionConverter(boolean typeConversionConverter) {
        isTypeConversionConverter = typeConversionConverter;
    }

    public boolean isSerializedObjectConverter() {
        return isSerializedObjectConverter;
    }

    public void setSerializedObjectConverter(boolean serializedObjectConverter) {
        isSerializedObjectConverter = serializedObjectConverter;
    }

    public boolean isTransformationMapping() {
        return isTransformationMapping;
    }

    public void setTransformationMapping(boolean transformationMapping) {
        isTransformationMapping = transformationMapping;
    }

    public String getTransformationMethodName() {
        return transformationMethodName;
    }

    public void setTransformationMethodName(String transformationMethodName) {
        this.transformationMethodName = transformationMethodName;
    }
}
