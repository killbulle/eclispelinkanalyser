package com.eclipselink.analyzer;

import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.RelationshipMetadata;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.Session;

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

            Package pkg = descriptor.getJavaClass().getPackage();
            node.setPackageName(pkg != null ? pkg.getName() : "");
            node.setType("ENTITY");

            Map<String, AttributeMetadata> attributes = new HashMap<>();
            List<RelationshipMetadata> relationships = new ArrayList<>();

            for (DatabaseMapping mapping : descriptor.getMappings()) {
                if (mapping.isDirectToFieldMapping() && mapping.getField() != null) {
                    AttributeMetadata attr = new AttributeMetadata();
                    attr.setName(mapping.getAttributeName());
                    attr.setJavaType(mapping.getAttributeClassification().getName());
                    attr.setDatabaseType(mapping.getField().getTypeName());
                    attr.setColumnName(mapping.getField().getName());
                    attributes.put(mapping.getAttributeName(), attr);
                } else if (mapping.isForeignReferenceMapping()) {
                    org.eclipse.persistence.mappings.ForeignReferenceMapping frm = (org.eclipse.persistence.mappings.ForeignReferenceMapping) mapping;
                    ClassDescriptor refDesc = mapping.getReferenceDescriptor();

                    RelationshipMetadata rel = new RelationshipMetadata();
                    rel.setAttributeName(mapping.getAttributeName());
                    rel.setTargetEntity(refDesc != null ? refDesc.getJavaClass().getSimpleName() : "Unknown");
                    rel.setLazy(frm.isLazy());
                    rel.setMappingType(mapping.getClass().getSimpleName().replace("Mapping", ""));
                    rel.setCascadePersist(frm.isCascadePersist());

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
                }
            }
            node.setAttributes(attributes);
            node.setRelationships(relationships);
            nodes.add(node);
        }
        return nodes;
    }
}
