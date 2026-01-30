package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.AggregateCollectionMapping;
import org.eclipse.persistence.mappings.DirectMapMapping;
import org.eclipse.persistence.mappings.VariableOneToOneMapping;
import org.eclipse.persistence.mappings.structures.ArrayMapping;
import org.eclipse.persistence.mappings.AggregateObjectMapping;
import org.eclipse.persistence.mappings.ForeignReferenceMapping;
import org.eclipse.persistence.mappings.TransformationMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetamodelExtractor {

    public List<EntityNode> extract(Session session) {
        List<EntityNode> nodes = new ArrayList<>();
        Map<Class, ClassDescriptor> descriptors = session.getDescriptors();

        for (ClassDescriptor descriptor : descriptors.values()) {
            if (descriptor.getJavaClass() == null)
                continue;

            EntityNode node = new EntityNode();
            node.setName(descriptor.getJavaClass().getSimpleName());
            node.setType("ENTITY");

            Package pkg = descriptor.getJavaClass().getPackage();
            node.setPackageName(pkg != null ? pkg.getName() : "");

            // Extract Inheritance Metadata
            if (descriptor.hasInheritance()) {
                org.eclipse.persistence.descriptors.InheritancePolicy policy = descriptor.getInheritancePolicy();
                if (policy.getParentDescriptor() != null) {
                    node.setParentEntity(policy.getParentDescriptor().getJavaClass().getSimpleName());
                }

                // Detect Inheritance Strategy
                // Default to null if no strategy is explicitly detected
                String strategy = null;

                if (descriptor.isAggregateDescriptor()) {
                    // handled by setType potentially, but safe to ignore strategy here
                } else if (policy.isJoinedStrategy()) {
                    strategy = "JOINED";
                } else if (policy.shouldReadSubclasses()) {
                    strategy = "SINGLE_TABLE";
                }

                if (strategy != null) {
                    node.setInheritanceStrategy(strategy);
                }

                if (policy.getClassIndicatorField() != null) {
                    node.setDiscriminatorColumn(policy.getClassIndicatorField().getName());
                }

                // Note: getClassIndicatorValues() and getClassIndicatorValue() are tricky in
                // 2.7.x
                // Skipping explicit discriminator value extraction if protected
            }

            // Flag MappedSuperclasses
            if (descriptor.isDescriptorTypeAggregate() || descriptor.isAggregateDescriptor()) {
                node.setType("EMBEDDABLE");
            } else if (descriptor.getTables().isEmpty()) {
                node.setType("MAPPED_SUPERCLASS");
            }

            if (descriptor.getJavaClass().isInterface()) {
                node.setType("INTERFACE");
            } else if (node.getType().equals("ENTITY")
                    && java.lang.reflect.Modifier.isAbstract(descriptor.getJavaClass().getModifiers())) {
                node.setType("ABSTRACT_ENTITY");
            }

            Map<String, AttributeMetadata> attributes = new HashMap<>();
            List<RelationshipMetadata> relationships = new ArrayList<>();

            for (DatabaseMapping mapping : descriptor.getMappings()) {
                if (mapping.isDirectToFieldMapping() && mapping.getField() != null) {
                    AttributeMetadata attr = new AttributeMetadata();
                    attr.setName(mapping.getAttributeName());
                    Class<?> attrClass = mapping.getAttributeClassification();
                    attr.setJavaType(attrClass != null ? attrClass.getName() : "Unknown");
                    attr.setDatabaseType(mapping.getField().getTypeName());
                    attr.setColumnName(mapping.getField().getName());
                    attributes.put(mapping.getAttributeName(), attr);
                } else if (mapping instanceof TransformationMapping) {
                    TransformationMapping tm = (TransformationMapping) mapping;
                    AttributeMetadata attr = new AttributeMetadata();
                    attr.setName(mapping.getAttributeName());
                    Class<?> attrClass = mapping.getAttributeClassification();
                    attr.setJavaType(attrClass != null ? attrClass.getName() : "Unknown");
                    attr.setDatabaseType("TRANSFORMED");
                    attr.setColumnName("MULTIPLE");
                    attr.setTransformationMapping(true);
                    attr.setTransformationMethodName(tm.getAttributeMethodName());
                    attributes.put(mapping.getAttributeName(), attr);
                } else if (mapping instanceof AggregateObjectMapping) {
                    AggregateObjectMapping aom = (AggregateObjectMapping) mapping;
                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity(aom.getReferenceClassName() != null
                            ? aom.getReferenceClassName().substring(aom.getReferenceClassName().lastIndexOf(".") + 1)
                            : "Unknown");
                    rel.setMappingType("Embedded");
                    relationships.add(rel);
                } else if (mapping instanceof DirectCollectionMapping) {
                    DirectCollectionMapping dcm = (DirectCollectionMapping) mapping;
                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity(
                            dcm.getAttributeClassification() != null ? dcm.getAttributeClassification().getSimpleName()
                                    : "Unknown");
                    rel.setMappingType("ElementCollection");
                    rel.setDirectCollection(true);

                    if (mapping instanceof DirectMapMapping) {
                        rel.setDirectMapMapping(true);
                        rel.setMappingType("DirectMap");
                    }
                    rel.setLazy(dcm.isLazy());
                    relationships.add(rel);
                } else if (mapping instanceof AggregateCollectionMapping) {
                    AggregateCollectionMapping acm = (AggregateCollectionMapping) mapping;
                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity(acm.getReferenceClassName() != null
                            ? acm.getReferenceClassName().substring(acm.getReferenceClassName().lastIndexOf(".") + 1)
                            : "Unknown");
                    rel.setMappingType("ElementCollection");
                    rel.setAggregateCollection(true);
                    rel.setLazy(acm.isLazy());
                    rel.setCascadePersist(acm.isCascadePersist());
                    rel.setCascadeRemove(acm.isCascadeRemove());
                    relationships.add(rel);
                } else if (mapping instanceof VariableOneToOneMapping) {
                    VariableOneToOneMapping varMapping = (VariableOneToOneMapping) mapping;
                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity("Variable");
                    rel.setMappingType("VariableOneToOne");
                    rel.setVariableOneToOne(true);
                    rel.setLazy(varMapping.isLazy());
                    rel.setVariableDiscriminatorColumn(varMapping.getTypeFieldName());
                    relationships.add(rel);
                } else if (mapping instanceof ArrayMapping) {
                    ArrayMapping am = (ArrayMapping) mapping;
                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity("Unknown (Array)");
                    rel.setMappingType("Array");
                    rel.setArrayMapping(true);
                    rel.setArrayStructureName(am.getStructureName());
                    relationships.add(rel);
                } else if (mapping.isForeignReferenceMapping()) {
                    org.eclipse.persistence.mappings.ForeignReferenceMapping frm = (org.eclipse.persistence.mappings.ForeignReferenceMapping) mapping;
                    ClassDescriptor refDesc = mapping.getReferenceDescriptor();

                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity(refDesc != null ? refDesc.getJavaClass().getSimpleName() : "Unknown");
                    rel.setMappingType(mapping.getClass().getSimpleName().replace("Mapping", ""));

                    rel.setPrivateOwned(frm.isPrivateOwned());
                    rel.setCascadePersist(frm.isCascadePersist());

                    if (frm.getIndirectionPolicy() != null) {
                        rel.setIndirectionType(frm.getIndirectionPolicy().getClass().getSimpleName()
                                .replace("IndirectionPolicy", "").toUpperCase());
                    }

                    // Fallback: If indirection is enabled but policy name didn't give a type (e.g.
                    // BasicIndirection)
                    if ((rel.getIndirectionType() == null || rel.getIndirectionType().equals("NO"))
                            && frm.usesIndirection()) {
                        rel.setIndirectionType("VALUEHOLDER");
                    }

                    if (mapping.isOneToOneMapping()) {
                        org.eclipse.persistence.mappings.OneToOneMapping oom = (org.eclipse.persistence.mappings.OneToOneMapping) mapping;
                        rel.setMappedBy(oom.getMappedBy());
                        rel.setOwningSide(oom.getMappedBy() == null);
                    } else if (mapping.isOneToManyMapping()) {
                        org.eclipse.persistence.mappings.OneToManyMapping otm = (org.eclipse.persistence.mappings.OneToManyMapping) mapping;
                        rel.setMappedBy(otm.getMappedBy());
                        rel.setOwningSide(otm.getMappedBy() == null);
                    } else if (mapping.isManyToManyMapping()) {
                        org.eclipse.persistence.mappings.ManyToManyMapping mtm = (org.eclipse.persistence.mappings.ManyToManyMapping) mapping;
                        rel.setMappedBy(mtm.getMappedBy());
                        rel.setOwningSide(mtm.getMappedBy() == null);
                    }

                    relationships.add(rel);
                } else {
                    System.err.println("[TODO-MAPPING] Unhandled mapping type: " + mapping.getClass().getName()
                            + " for attribute: " + mapping.getAttributeName() + " in " + descriptor.getJavaClassName());
                }
            }
            node.setAttributes(attributes);
            node.setRelationships(relationships);
            nodes.add(node);
        }
        return nodes;
    }
}
