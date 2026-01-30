# Mappings EclipseLink Non Gérés dans MetamodelExtractor

Ce document liste les mappings EclipseLink 2.7 qui ne sont pas explicitement gérés dans `MetamodelExtractor.java` (analyzer-backend/src/main/java/com/eclipselink/analyzer/MetamodelExtractor.java:206).

## Vue d'ensemble

Les mappings actuellement gérés:
- ✅ DirectToFieldMapping
- ✅ TransformationMapping
- ✅ AggregateObjectMapping (Embedded)
- ✅ DirectCollectionMapping (ElementCollection)
- ✅ DirectMapMapping
- ✅ AggregateCollectionMapping (ElementCollection avec embeddables)
- ✅ VariableOneToOneMapping
- ✅ ArrayMapping
- ✅ ForeignReferenceMapping (OneToOne, OneToMany, ManyToMany, ManyToOne)

---

## Mappings Non Gérés

### 1. MultitenantPrimaryKeyMapping

**Classe**: `org.eclipse.persistence.mappings.MultitenantPrimaryKeyMapping`

**Héritage**: `AbstractColumnMapping` → `DatabaseMapping`

**Description**: Mapping utilisé pour la gestion multitenant où une colonne de clé primaire sert de discriminant de tenant.

**Depuis**: EclipseLink 2.4

**Détection EclipseLink**: `mapping.isMultitenantPrimaryKeyMapping()`

**Attributs clés**:
- `MultitenantPrimaryKeyAccessor accessor` - Accessor pour la valeur du tenant
- `isInsertable = true`
- `isUpdatable = false`
- `isOptional = false`

**Impact si non géré**:
- Attributs discriminants de tenant non détectés
- Analyses de sécurité/performance incomplètes pour applications multitenant

**Priorité**: Élevée

**Action requise**:
```java
} else if (mapping.isMultitenantPrimaryKeyMapping()) {
    MultitenantPrimaryKeyMapping mtkm = (MultitenantPrimaryKeyMapping) mapping;
    AttributeMetadata attr = new AttributeMetadata();
    attr.setName(mapping.getAttributeName());
    attr.setJavaType("MultitenantKey");
    attr.setDatabaseType(mtkm.getField().getTypeName());
    attr.setColumnName(mtkm.getField().getName());
    attr.setMultitenantPrimaryKey(true); // Ajouter ce champ dans AttributeMetadata
    attributes.put(mapping.getAttributeName(), attr);
}
```

---

### 2. NestedTableMapping

**Classe**: `org.eclipse.persistence.mappings.structures.NestedTableMapping`

**Héritage**: `CollectionMapping` → `ForeignReferenceMapping` → `DatabaseMapping`

**Description**: Mapping pour les tables imbriquées Oracle (nested tables). Similaire à VARRAY mais stockées dans une table séparée.

**Support base de données**: Oracle 8i+

**Détection EclipseLink**: `mapping.isNestedTableMapping()`

**Attributs clés**:
- `String structureName` - Nom du type ADT Oracle
- `DatabaseField field` - Champ contenant la référence

**Impact si non géré**:
- Mapping Oracle-specific non détecté
- Règle `NestedTableRule` ne peut pas fonctionner (le flag `isNestedTable` n'est jamais setté)

**Priorité**: Moyenne (Oracle-specific)

**État partiel**: Un flag `isNestedTable` existe dans `RelationshipMetadata` mais n'est jamais setté

**Action requise**:
```java
} else if (mapping.isNestedTableMapping()) {
    NestedTableMapping ntm = (NestedTableMapping) mapping;
    RelationshipMetadata rel = new RelationshipMetadata();
    rel.setAttributeName(mapping.getAttributeName());
    rel.setTargetEntity("NestedTable");
    rel.setMappingType("NestedTable");
    rel.setNestedTable(true); // Flag existe déjà
    rel.setStructureName(ntm.getStructureName()); // Optionnel
    relationships.add(rel);
}
```

---

### 3. StructureMapping

**Classe**: `org.eclipse.persistence.mappings.structures.StructureMapping`

**Héritage**: `AbstractCompositeObjectMapping` → `AggregateObjectMapping` → `DatabaseMapping`

**Description**: Mapping pour les structures SQL Oracle (types utilisateur). Similaire à AggregateObjectMapping mais pour types objet-relationnels.

**Support base de données**: Oracle 8i+

**Détection EclipseLink**: `mapping.isStructureMapping()`

**Attributs clés**:
- `String structureName` - Nom du type de données défini par l'utilisateur

**Impact si non géré**:
- Attribut serait traité comme `AggregateObjectMapping` standard
- Perte d'information sur la nature Oracle-specific

**Priorité**: Faible (serait attrapé par `AggregateObjectMapping` check)

**Action requise**:
```java
// Ajouter dans le bloc AggregateObjectMapping
} else if (mapping instanceof AggregateObjectMapping) {
    AggregateObjectMapping aom = (AggregateObjectMapping) mapping;
    RelationshipMetadata rel = new RelationshipMetadata();
    rel.setAttributeName(mapping.getAttributeName());
    rel.setTargetEntity(aom.getReferenceClassName() != null
            ? aom.getReferenceClassName().substring(aom.getReferenceClassName().lastIndexOf(".") + 1)
            : "Unknown");
    rel.setMappingType("Embedded");
    
    // Distinguer les StructureMapping Oracle
    if (mapping.isStructureMapping()) {
        rel.setStructureMapping(true);
        rel.setStructureName(((StructureMapping) mapping).getStructureName());
    }
    
    relationships.add(rel);
}
```

---

### 4. ReferenceMapping

**Classe**: `org.eclipse.persistence.mappings.structures.ReferenceMapping`

**Héritage**: `ObjectReferenceMapping` → `ForeignReferenceMapping` → `DatabaseMapping`

**Description**: Mapping pour les types REF Oracle qui référencent d'autres structures dans un modèle objet-relationnel.

**Support base de données**: Oracle 8i+

**Détection EclipseLink**: `mapping.isReferenceMapping()`

**Attributs clés**:
- `DatabaseField field` - Champ unique contenant le REF

**Impact si non géré**:
- Serait attrapé par le bloc `isForeignReferenceMapping()`
- Perte d'information sur la nature Oracle-specific (REF vs foreign key)

**Priorité**: Faible (serait attrapé par `ForeignReferenceMapping` check)

**Action requise**:
```java
// Dans le bloc isForeignReferenceMapping()
RelationshipMetadata rel = new RelationshipMetadata();
rel.setAttributeName(mapping.getAttributeName());
rel.setTargetEntity(refDesc != null ? refDesc.getJavaClass().getSimpleName() : "Unknown");
rel.setMappingType(mapping.getClass().getSimpleName().replace("Mapping", ""));

// Distinguer les ReferenceMapping Oracle
if (mapping.isReferenceMapping()) {
    rel.setReferenceMapping(true);
}

// ... reste du code
```

---

### 5. DirectToXMLTypeMapping

**Classe**: `org.eclipse.persistence.mappings.xdb.DirectToXMLTypeMapping`

**Héritage**: `DirectToFieldMapping` → `DatabaseMapping`

**Description**: Mapping pour stocker des documents XML (DOM ou String) dans des champs Oracle XMLType.

**Support base de données**: Oracle 9i XDB+

**Détection EclipseLink**: `mapping.isDirectToXMLTypeMapping()`

**Attributs clés**:
- `boolean shouldReadWholeDocument` - Si le DOM doit être initialisé complet à la lecture
- `XMLTransformer xmlTransformer` - Pour convertir DOM en String
- `XMLComparer xmlComparer` - Pour détecter les modifications XML
- `XMLParser xmlParser` - Pour convertir String en DOM

**Impact si non géré**:
- Serait attrapé par le bloc `isDirectToFieldMapping()`
- Perte d'information sur le fait que c'est du XML Oracle-specific
- Impossible de détecter les problèmes potentiels avec les gros documents XML

**Priorité**: Faible (serait attrapé par `DirectToFieldMapping` check)

**Action requise**:
```java
// Dans le bloc isDirectToFieldMapping
if (mapping.isDirectToFieldMapping() && mapping.getField() != null) {
    AttributeMetadata attr = new AttributeMetadata();
    attr.setName(mapping.getAttributeName());
    Class<?> attrClass = mapping.getAttributeClassification();
    attr.setJavaType(attrClass != null ? attrClass.getName() : "Unknown");
    attr.setDatabaseType(mapping.getField().getTypeName());
    attr.setColumnName(mapping.getField().getName());
    
    // Distinguer DirectToXMLTypeMapping
    if (mapping.isDirectToXMLTypeMapping()) {
        attr.setXmlTypeMapping(true);
    }
    
    attributes.put(mapping.getAttributeName(), attr);
}
```

---

### 6. ObjectArrayMapping

**Classe**: `org.eclipse.persistence.mappings.structures.ObjectArrayMapping`

**Héritage**: `AbstractCompositeCollectionMapping` → `AbstractCompositeCollectionMapping` → `DatabaseMapping`

**Description**: Mapping pour les arrays Oracle de types de données complexes (pas de primitives).

**Support base de données**: Oracle 8i+

**Détection EclipseLink**: Pas de méthode `isObjectArrayMapping()` directe dans `DatabaseMapping`

**Attributs clés**:
- `String structureName` - Nom du type ADT Oracle

**Impact si non géré**:
- Serait attrapé par `AggregateCollectionMapping` check (car extends AbstractCompositeCollectionMapping)
- Perte d'information sur la nature Oracle-specific (VARRAY vs ElementCollection standard)

**Priorité**: Faible

**Action requise**:
```java
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
    
    // Distinguer ObjectArrayMapping (nécessite instanceof)
    if (mapping instanceof ObjectArrayMapping) {
        rel.setObjectArrayMapping(true);
        rel.setStructureName(((ObjectArrayMapping) mapping).getStructureName());
    }
    
    relationships.add(rel);
}
```

---

### 7. UnidirectionalOneToManyMapping

**Classe**: `org.eclipse.persistence.mappings.UnidirectionalOneToManyMapping`

**Héritage**: `OneToManyMapping` → `CollectionMapping` → `ForeignReferenceMapping` → `DatabaseMapping`

**Description**: Mapping OneToMany SANS référence inverse (pas de mappedBy côté ManyToOne).

**Depuis**: EclipseLink 1.1

**Détection EclipseLink**: Pas de méthode `isUnidirectionalOneToManyMapping()` directe

**Attributs clés**:
- `boolean shouldIncrementTargetLockValueOnAddOrRemoveTarget` - Si la version optimistic du target doit être incrémentée

**Impact si non géré**:
- Serait attrapé par le bloc `isOneToManyMapping()`
- Perte d'information sur la nature unidirectionnelle (important pour N+1 queries et jointures)

**Priorité**: Moyenne

**Action requise**:
```java
// Dans le bloc isOneToManyMapping
} else if (mapping.isOneToManyMapping()) {
    org.eclipse.persistence.mappings.OneToManyMapping otm = (org.eclipse.persistence.mappings.OneToManyMapping) mapping;
    rel.setMappedBy(otm.getMappedBy());
    rel.setOwningSide(otm.getMappedBy() == null);
    
    // Distinguer UnidirectionalOneToManyMapping
    if (mapping instanceof UnidirectionalOneToManyMapping) {
        rel.setUnidirectional(true);
        UnidirectionalOneToManyMapping uotm = (UnidirectionalOneToManyMapping) mapping;
        rel.setShouldIncrementTargetLockValueOnAddOrRemoveTarget(
            uotm.shouldIncrementTargetLockValueOnAddOrRemoveTarget());
    }
}
```

---

## Modifications Modèles Requises

Pour supporter ces mappings, les classes modèle suivantes doivent être étendues:

### AttributeMetadata.java
Ajouter:
- `private boolean xmlTypeMapping;`
- `private boolean multitenantPrimaryKey;`

### RelationshipMetadata.java
Ajouter:
- `private boolean referenceMapping;` (déjà détecté par ForeignReferenceMapping mais flag utile)
- `private boolean structureMapping;`
- `private String structureName;` (existe déjà pour ArrayMapping, peut être réutilisé)
- `private boolean objectArrayMapping;`
- `private boolean unidirectional;`
- `private boolean shouldIncrementTargetLockValueOnAddOrRemoveTarget;`

---

## Ordre de Priorité de Correction

1. **MultitenantPrimaryKeyMapping** - Élevée (plus commun)
2. **NestedTableMapping** - Moyenne (corrige la règle NestedTableRule)
3. **UnidirectionalOneToManyMapping** - Moyenne (impact sur performance)
4. **ObjectArrayMapping** - Faible (Oracle-specific)
5. **StructureMapping** - Faible (serait partiellement géré)
6. **ReferenceMapping** - Faible (serait partiellement géré)
7. **DirectToXMLTypeMapping** - Faible (serait partiellement géré)

---

## Notes

- Les mappings Oracle-specific (NestedTableMapping, StructureMapping, ReferenceMapping, ObjectArrayMapping, DirectToXMLTypeMapping) sont rarement utilisés en pratique mais devraient être supportés pour une couverture complète.
- La plupart de ces mappings seraient attrapés par les checks actuels mais avec perte d'information spécifique.
- Le mapping MultitenantPrimaryKeyMapping est le plus important à supporter car c'est une fonctionnalité moderne d'EclipseLink qui peut impacter la sécurité et les performances.
