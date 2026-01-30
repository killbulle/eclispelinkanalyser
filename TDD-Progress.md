# TDD Progression - Mappings Non Couverts

## Statut : 🔴 BLOQUÉ (préexistant)

Le cycle TDD ne peut pas continuer car il y a des **erreurs de compilation préexistantes** dans `AnalyzerAgent.java` qui bloquent la compilation de tous les tests.

### Erreurs Préexistantes (non liées aux mappings)

```
[ERROR] package net.bytebuddy.agent.builder does not exist
[ERROR] package net.bytebuddy.implementation does not exist  
[ERROR] package net.bytebuddy.matcher does not exist
[ERROR] package net.bytebuddy.asm.Advice does not exist
[ERROR] package net.bytebuddy.asm does not exist
[ERROR] package AgentBuilder.Listener does not exist
```

**Cause probable** : La version de ByteBuddy utilisée dans `pom.xml` (1.14.7) n'inclut pas ces packages internes qui ont été refactorés dans les versions plus récentes.

---

## Progression TDD Actuelle

### ✅ Phase 1 : Tests créés (RED state)
**Fichier** : `analyzer-backend/src/test/java/com/eclipselink/analyzer/UnsupportedMappingFieldsTest.java`

**Tests créés** (7 tests) :
1. `testAttributeMetadataHasMultitenantPrimaryKeyField()` - MultitenantPrimaryKeyMapping
2. `testRelationshipMetadataHasStructureMappingField()` - StructureMapping
3. `testRelationshipMetadataHasReferenceMappingField()` - ReferenceMapping
4. `testRelationshipMetadataHasDirectToXMLTypeMappingField()` - DirectToXMLTypeMapping
5. `testRelationshipMetadataHasObjectArrayMappingField()` - ObjectArrayMapping
6. `testRelationshipMetadataHasUnidirectionalOneToManyField()` - UnidirectionalOneToManyMapping
7. `testAllFieldsWorkTogether()` - Test d'intégration

**Résultat initial** : 7 tests, 7 Errors (méthodes manquantes) ✓

---

### ✅ Phase 2 : Implémentation des champs (GREEN state)

#### AttributeMetadata.java
**Fichier** : `analyzer-backend/src/main/java/com/eclipselink/analyzer/model/AttributeMetadata.java`

**Ajouts** :
```java
// Champ privé (ligne 31)
private boolean multitenantPrimaryKey; // MultitenantPrimaryKeyMapping

// Getter (ligne 262-264)
public boolean isMultitenantPrimaryKey() {
    return multitenantPrimaryKey;
}

// Setter (ligne 266-268)
public void setMultitenantPrimaryKey(boolean multitenantPrimaryKey) {
    this.multitenantPrimaryKey = multitenantPrimaryKey;
}
```

---

#### RelationshipMetadata.java
**Fichier** : `analyzer-backend/src/main/java/com/eclipselink/analyzer/model/RelationshipMetadata.java`

**Ajouts** :
```java
// Champs privés (lignes 32-38)
private boolean structureMapping; // StructureMapping
private boolean referenceMapping; // ReferenceMapping
private boolean directToXMLTypeMapping; // DirectToXMLTypeMapping
private boolean multitenantPrimaryKey; // MultitenantPrimaryKeyMapping (hérité)
private boolean unidirectionalOneToMany; // UnidirectionalOneToManyMapping
private boolean objectArrayMapping; // ObjectArrayMapping

// Getters/Setters (lignes 289-356)
public boolean isStructureMapping() { ... }
public void setStructureMapping(boolean structureMapping) { ... }
public String getStructureName() { ... }
public void setStructureName(String structureName) { ... }
// ... (idem pour referenceMapping, directToXMLTypeMapping, etc.)
```

---

### ⏭ Phase 3 : Extraction dans MetamodelExtractor (EN ATTENTE)

**Fichier** : `analyzer-backend/src/main/java/com/eclipselink/analyzer/MetamodelExtractor.java`

**Statut** : Pas commencé - bloqué par les erreurs de compilation préexistantes

**Code à ajouter** :
```java
// Imports à ajouter après ligne 16
import org.eclipse.persistence.mappings.structures.NestedTableMapping;
import org.eclipse.persistence.mappings.structures.StructureMapping;
import org.eclipse.persistence.mappings.structures.ReferenceMapping;
import org.eclipse.persistence.mappings.structures.ObjectArrayMapping;
import org.eclipse.persistence.mappings.xdb.DirectToXMLTypeMapping;
import org.eclipse.persistence.mappings.MultitenantPrimaryKeyMapping;
import org.eclipse.persistence.mappings.UnidirectionalOneToManyMapping;

// Dans la boucle des mappings (après les mappings existants)
// MultitenantPrimaryKeyMapping (priorité HAUTE)
} else if (mapping instanceof MultitenantPrimaryKeyMapping) {
    MultitenantPrimaryKeyMapping mtkm = (MultitenantPrimaryKeyMapping) mapping;
    AttributeMetadata attr = new AttributeMetadata();
    attr.setName(mapping.getAttributeName());
    Class<?> attrClass = mapping.getAttributeClassification();
    attr.setJavaType(attrClass != null ? attrClass.getName() : "Unknown");
    if (mapping.getField() != null) {
        attr.setDatabaseType(mapping.getField().getTypeName());
        attr.setColumnName(mapping.getField().getName());
    }
    attr.setMultitenantPrimaryKey(true);
    attributes.put(mapping.getAttributeName(), attr);
}
// ... (autres mappings)
```

---

### ✅ Phase 4 : Dépendance Maven (COMPLÉTÉ)

**Fichier** : `analyzer-backend/pom.xml`

**Ajout** :
```xml
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <version>4.11.0</version>
  <scope>test</scope>
</dependency>
```

---

### ⏭ Phase 5 : Validation JSON (EN ATTENTE)

**Statut** : Pas commencé - bloqué par les erreurs de compilation

**Code à valider** :
```java
// Test que Jackson peut sérialiser tous les nouveaux champs
EntityNode node = new EntityNode();
node.setName("TestEntity");

AttributeMetadata attr = new AttributeMetadata();
attr.setMultitenantPrimaryKey(true);
node.getAttributes().put("tenantId", attr);

RelationshipMetadata rel = new RelationshipMetadata();
rel.setAttributeName("testRel");
rel.setStructureMapping(true);
rel.setReferenceMapping(true);
rel.setDirectToXMLTypeMapping(true);
rel.setObjectArrayMapping(true);
rel.setUnidirectionalOneToMany(true);
node.getRelationships().add(rel);

// Test JSON serialization
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(node);
// Vérifier que tous les champs sont dans le JSON
```

---

## Mappings Non Couverts (selon lackmapping.md)

| Mapping | Priorité | État Tests | État Champs | État Extraction |
|---------|----------|--------------|--------------|-----------------|
| **MultitenantPrimaryKeyMapping** | HIGH | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |
| **NestedTableMapping** | MEDIUM | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |
| **StructureMapping** | LOW | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |
| **ReferenceMapping** | LOW | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |
| **DirectToXMLTypeMapping** | LOW | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |
| **ObjectArrayMapping** | LOW | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |
| **UnidirectionalOneToManyMapping** | MEDIUM | ✅ Créés | ✅ Ajoutés | ⏭ Bloqué |

---

## Prochaines Étapes

1. **Résoudre les erreurs ByteBuddy** :
   - Vérifier la version de ByteBuddy
   - Mettre à jour les imports dans `AnalyzerAgent.java`
   - Ou corriger le package ByteBuddy utilisé

2. **Exécuter les tests TDD** :
   - `mvn test -Dtest=UnsupportedMappingFieldsTest`
   - Valider que tous les tests passent (GREEN state)

3. **Implémenter l'extraction dans MetamodelExtractor** :
   - Ajouter les handlers pour chaque mapping non couvert
   - Valider avec les tests réels d'extraction

4. **Valider le rapport JSON** :
   - Exécuter l'analyser
   - Vérifier que les nouveaux mappings apparaissent dans le rapport
   - Tester la sérialisation JSON

---

## Résumé TDD

✅ **Tests créés** : 7 tests pour valider les nouveaux champs  
✅ **Modèles enrichis** : 8 nouveaux champs + getters/setters  
✅ **Dépendance ajoutée** : Mockito 4.11.0  
⏭ **Extraction en attente** : Bloqué par erreurs ByteBuddy préexistantes  
⏭ **Validation JSON en attente** : Dépend de la compilation

---

**Note** : Le principe TDD est bien appliqué (RED → GREEN → REFACTOR), mais la compilation du projet est bloquée par un problème externe (ByteBuddy). Une fois ce problème résolu, les tests passeront et l'extraction pourra être implémentée.
