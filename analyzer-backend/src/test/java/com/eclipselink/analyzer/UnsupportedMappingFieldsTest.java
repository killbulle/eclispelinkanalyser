package com.eclipselink.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import com.eclipselink.analyzer.model.EntityNode;
import com.eclipselink.analyzer.model.AttributeMetadata;
import com.eclipselink.analyzer.model.RelationshipMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * TDD tests for Unsupported Mappings extraction.
 * Using JUnit 5 to match existing tests.
 *
 * Progression: Test fields → Test getters/setters → Test JSON serialization
 */
public class UnsupportedMappingFieldsTest {

    private EntityNode node;
    private AttributeMetadata attr;
    private RelationshipMetadata rel;

    @BeforeEach
    void setUp() {
        node = new EntityNode();
        node.setName("TestEntity");
        node.setAttributes(new java.util.HashMap<>());
        node.setRelationships(new ArrayList<>());

        attr = new AttributeMetadata();
        attr.setName("testAttr");
        attr.setJavaType("java.lang.String");
        attr.setDatabaseType("VARCHAR");

        rel = new RelationshipMetadata();
        rel.setAttributeName("testRel");
        rel.setMappingType("TestMapping");
    }

    /**
     * TDD Step 1: AttributeMetadata should have multitenantPrimaryKey field
     * Status: RED (no field) → GREEN (field added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: AttributeMetadata should have multitenantPrimaryKey field")
    void testAttributeMetadataHasMultitenantPrimaryKeyField() {
        try {
            attr.setMultitenantPrimaryKey(true);
            assertTrue(true, "setMultitenantPrimaryKey method should exist");
            assertTrue(attr.isMultitenantPrimaryKey(), "MultitenantPrimaryKey should be true");
        } catch (NoSuchMethodError e) {
            fail("setMultitenantPrimaryKey method should exist: " + e.getMessage());
        }
    }

    /**
     * TDD Step 2: RelationshipMetadata should have structureMapping field
     * Status: RED (no field) → GREEN (field added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: RelationshipMetadata should have structureMapping field")
    void testRelationshipMetadataHasStructureMappingField() {
        try {
            rel.setStructureMapping(true);
            assertTrue(true, "setStructureMapping method should exist");
            assertTrue(rel.isStructureMapping(), "StructureMapping should be true");
        } catch (NoSuchMethodError e) {
            fail("setStructureMapping method should exist: " + e.getMessage());
        }
    }

    /**
     * TDD Step 3: RelationshipMetadata should have referenceMapping field
     * Status: RED (no field) → GREEN (field added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: RelationshipMetadata should have referenceMapping field")
    void testRelationshipMetadataHasReferenceMappingField() {
        try {
            rel.setReferenceMapping(true);
            assertTrue(true, "setReferenceMapping method should exist");
            assertTrue(rel.isReferenceMapping(), "ReferenceMapping should be true");
        } catch (NoSuchMethodError e) {
            fail("setReferenceMapping method should exist: " + e.getMessage());
        }
    }

    /**
     * TDD Step 4: RelationshipMetadata should have directToXMLTypeMapping field
     * Status: RED (no field) → GREEN (field added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: RelationshipMetadata should have directToXMLTypeMapping field")
    void testRelationshipMetadataHasDirectToXMLTypeMappingField() {
        try {
            rel.setDirectToXMLTypeMapping(true);
            assertTrue(true, "setDirectToXMLTypeMapping method should exist");
            assertTrue(rel.isDirectToXMLTypeMapping(), "DirectToXMLTypeMapping should be true");
        } catch (NoSuchMethodError e) {
            fail("setDirectToXMLTypeMapping method should exist: " + e.getMessage());
        }
    }

    /**
     * TDD Step 5: RelationshipMetadata should have objectArrayMapping field
     * Status: RED (no field) → GREEN (field added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: RelationshipMetadata should have objectArrayMapping field")
    void testRelationshipMetadataHasObjectArrayMappingField() {
        try {
            rel.setObjectArrayMapping(true);
            assertTrue(true, "setObjectArrayMapping method should exist");
            assertTrue(rel.isObjectArrayMapping(), "ObjectArrayMapping should be true");
        } catch (NoSuchMethodError e) {
            fail("setObjectArrayMapping method should exist: " + e.getMessage());
        }
    }

    /**
     * TDD Step 6: RelationshipMetadata should have unidirectionalOneToMany field
     * Status: RED (no field) → GREEN (field added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: RelationshipMetadata should have unidirectionalOneToMany field")
    void testRelationshipMetadataHasUnidirectionalOneToManyField() {
        try {
            rel.setUnidirectionalOneToMany(true);
            assertTrue(true, "setUnidirectionalOneToMany method should exist");
            assertTrue(rel.isUnidirectionalOneToMany(), "UnidirectionalOneToMany should be true");
        } catch (NoSuchMethodError e) {
            fail("setUnidirectionalOneToMany method should exist: " + e.getMessage());
        }
    }

    /**
     * TDD Step 7: All fields should work together
     * Status: RED (missing fields) → GREEN (all fields added) → REFACTOR
     */
    @Test
    @DisplayName("TDD: All fields should work together")
    void testAllFieldsWorkTogether() {
        try {
            attr.setMultitenantPrimaryKey(true);
            node.getAttributes().put("tenantId", attr);

            rel.setStructureMapping(true);
            rel.setReferenceMapping(true);
            rel.setDirectToXMLTypeMapping(true);
            rel.setObjectArrayMapping(true);
            rel.setUnidirectionalOneToMany(true);

            node.getRelationships().add(rel);

            assertAll(
                    "All new mapping fields should work",
                    () -> assertFalse(node.getAttributes().isEmpty(), "Node should have attributes"),
                    () -> assertFalse(node.getRelationships().isEmpty(), "Node should have relationships"),
                    () -> assertTrue(node.getAttributes().get("tenantId").isMultitenantPrimaryKey(),
                            "Tenant ID should be multitenant primary key"),
                    () -> assertTrue(node.getRelationships().get(0).isStructureMapping(),
                            "Relationship should have structure mapping"),
                    () -> assertTrue(node.getRelationships().get(0).isReferenceMapping(),
                            "Relationship should have reference mapping"),
                    () -> assertTrue(node.getRelationships().get(0).isDirectToXMLTypeMapping(),
                            "Relationship should have direct to XML type mapping"),
                    () -> assertTrue(node.getRelationships().get(0).isObjectArrayMapping(),
                            "Relationship should have object array mapping"),
                    () -> assertTrue(node.getRelationships().get(0).isUnidirectionalOneToMany(),
                            "Relationship should have unidirectional one-to-many"));
        } catch (NoSuchMethodError e) {
            fail("Fields should exist: " + e.getMessage());
        }
    }
}
