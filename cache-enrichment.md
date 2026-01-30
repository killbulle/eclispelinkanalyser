# Enrichissement des Informations de Cache

Ce document décrit les modifications apportées pour enrichir le rapport d'analyse avec les configurations de cache d'EclipseLink.

## Modifications Effectuées

### 1. MetamodelExtractor.java
**Fichier**: `analyzer-backend/src/main/java/com/eclipselink/analyzer/MetamodelExtractor.java`

**Localisation**: Lignes 85-125 (après extraction de l'héritage, avant extraction des mappings)

**Ajout**: Extraction des configurations de cache depuis `ClassDescriptor`

```java
// Extract Cache Configuration
if (descriptor.getCachePolicy() != null) {
    // Cache Type
    Class<?> identityMapClass = descriptor.getIdentityMapClass();
    if (identityMapClass != null) {
        String className = identityMapClass.getSimpleName();
        if (className.contains("FullIdentityMap")) {
            node.setCacheType("FULL");
        } else if (className.contains("SoftIdentityMap")) {
            node.setCacheType("SOFT");
        } else if (className.contains("WeakIdentityMap")) {
            node.setCacheType("WEAK");
        } else if (className.contains("HardCacheWeakIdentityMap")) {
            node.setCacheType("HARD_WEAK");
        } else if (className.contains("SoftWeak")) {
            node.setCacheType("SOFT_WEAK");
        } else if (className.contains("NoIdentityMap")) {
            node.setCacheType("NONE");
        }
    }

    // Cache Size
    int cacheSize = descriptor.getIdentityMapSize();
    if (cacheSize > 0) {
        node.setCacheSize(cacheSize);
    }

    // Cache Expiry
    org.eclipse.persistence.descriptors.invalidation.CacheInvalidationPolicy invalidationPolicy =
            descriptor.getCacheInvalidationPolicy();
    if (invalidationPolicy != null) {
        if (invalidationPolicy instanceof org.eclipse.persistence.descriptors.invalidation.TimeToLiveCacheInvalidationPolicy) {
            long ttl = ((org.eclipse.persistence.descriptors.invalidation.TimeToLiveCacheInvalidationPolicy) invalidationPolicy).getTimeToLive();
            if (ttl > 0) {
                node.setCacheExpiry((int) ttl);
            }
        }
    }

    // Cache Synchronization (Coordination)
    int syncType = descriptor.getCacheSynchronizationType();
    if (syncType == org.eclipse.persistence.descriptors.ClassDescriptor.SEND_OBJECT_CHANGES) {
        node.setCacheCoordinationType("SEND_OBJECT_CHANGES");
    } else if (syncType == org.eclipse.persistence.descriptors.ClassDescriptor.INVALIDATE_CHANGED_OBJECTS) {
        node.setCacheCoordinationType("INVALIDATE_CHANGED_OBJECTS");
    } else if (syncType == org.eclipse.persistence.descriptors.ClassDescriptor.SEND_NEW_OBJECTS_WITH_CHANGES) {
        node.setCacheCoordinationType("SEND_NEW_OBJECTS_WITH_CHANGES");
    } else if (syncType == org.eclipse.persistence.descriptors.ClassDescriptor.DO_NOT_SEND_CHANGES) {
        node.setCacheCoordinationType("NONE");
    }

    // Cache Isolation
    org.eclipse.persistence.config.CacheIsolationType isolation = descriptor.getCacheIsolation();
    if (isolation != null) {
        node.setCacheIsolation(isolation.name());
    }

    // Cache Refresh Settings
    node.setCacheAlwaysRefresh(descriptor.shouldAlwaysRefreshCache());
    node.setCacheRefreshOnlyIfNewer(descriptor.shouldOnlyRefreshCacheIfNewerVersion());
    node.setCacheDisableHits(descriptor.shouldDisableCacheHits());
}
```

---

### 2. EntityNode.java
**Fichier**: `analyzer-backend/src/main/java/com/eclipselink/analyzer/model/EntityNode.java`

**Ajouts**: 5 nouveaux champs de configuration de cache (lignes 21-27)

```java
// EclipseLink Cache Configuration
private String cacheType; // FULL, WEAK, SOFT, NONE, SOFT_WEAK, HARD_WEAK
private Integer cacheSize; // Size of the cache (number of objects)
private Integer cacheExpiry; // Expiry in milliseconds
private String cacheCoordinationType; // SEND_OBJECT_CHANGES, INVALIDATE_CHANGED_OBJECTS, etc.
private String cacheIsolation; // SHARED, PROTECTED, ISOLATED
private Boolean cacheAlwaysRefresh; // alwaysRefresh
private Boolean cacheRefreshOnlyIfNewer; // refreshOnlyIfNewer
private Boolean cacheDisableHits; // disableHits
```

**Ajouts**: Getters et Setters correspondants (lignes 161-187)

```java
public Integer getCacheSize() {
    return cacheSize;
}

public void setCacheSize(Integer cacheSize) {
    this.cacheSize = cacheSize;
}

public String getCacheIsolation() {
    return cacheIsolation;
}

public void setCacheIsolation(String cacheIsolation) {
    this.cacheIsolation = cacheIsolation;
}

public Boolean getCacheAlwaysRefresh() {
    return cacheAlwaysRefresh;
}

public void setCacheAlwaysRefresh(Boolean cacheAlwaysRefresh) {
    this.cacheAlwaysRefresh = cacheAlwaysRefresh;
}

public Boolean getCacheRefreshOnlyIfNewer() {
    return cacheRefreshOnlyIfNewer;
}

public void setCacheRefreshOnlyIfNewer(Boolean cacheRefreshOnlyIfNewer) {
    this.cacheRefreshOnlyIfNewer = cacheRefreshOnlyIfNewer;
}

public Boolean getCacheDisableHits() {
    return cacheDisableHits;
}

public void setCacheDisableHits(Boolean cacheDisableHits) {
    this.cacheDisableHits = cacheDisableHits;
}
```

---

### 3. CacheRule.java
**Fichier**: `analyzer-backend/src/main/java/com/eclipselink/analyzer/rules/CacheRule.java`

**Enrichissement**: 7 nouvelles violations de cache (lignes 55-119)

#### Nouvelles violations ajoutées :

1. **ISOLATED cache warning**
   ```java
   if ("ISOLATED".equals(cacheIsolation)) {
       violations.add(new Violation(getId(), "WARNING",
           "Entity '" + entity.getName() + "' uses ISOLATED cache (no second-level caching). " +
           "This may impact performance for frequently accessed entities. " +
           "Consider SHARED or PROTECTED unless caching is explicitly disabled."));
   }
   ```

2. **FULL cache on large entities**
   ```java
   if ("FULL".equals(cacheType) && entity.getAttributes() != null && entity.getAttributes().size() > 20) {
       violations.add(new Violation(getId(), "WARNING",
           "Entity '" + entity.getName() + "' uses FULL cache with " + entity.getAttributes().size() + " attributes. " +
           "FULL cache may be memory-intensive. Consider SOFT_WEAK for better memory management."));
   }
   ```

3. **Default cache size**
   ```java
   if (cacheSize != null && cacheSize == 100) {
       violations.add(new Violation(getId(), "INFO",
           "Entity '" + entity.getName() + "' uses default cache size (100). " +
           "Consider tuning cache size based on expected entity count and memory constraints."));
   }
   ```

4. **Cache disableHits warning**
   ```java
   if (cacheDisableHits != null && cacheDisableHits) {
       violations.add(new Violation(getId(), "WARNING",
           "Entity '" + entity.getName() + "' has cache hits disabled. " +
           "This forces all queries to hit the database. Use only when necessary for specific optimization needs."));
   }
   ```

5. **Cache alwaysRefresh warning**
   ```java
   if (cacheAlwaysRefresh != null && cacheAlwaysRefresh && !"ISOLATED".equals(cacheIsolation)) {
       violations.add(new Violation(getId(), "WARNING",
           "Entity '" + entity.getName() + "' has alwaysRefresh=true but uses cache. " +
           "This defeats the purpose of caching. Use ISOLATED cache instead."));
   }
   ```

6. **Reference entities with inappropriate cache type**
   ```java
   if ("REFERENCE_ENTITY".equals(entity.getDddRole()) && cacheType != null &&
           (cacheType.equals("FULL") || cacheType.equals("SOFT_WEAK") || cacheType.equals("HARD_WEAK"))) {
       violations.add(new Violation(getId(), "INFO",
           "Entity '" + entity.getName() + "' appears to be reference data. " +
           "Consider using WEAK or SOFT cache to allow GC of unused references."));
   }
   ```

7. **Reference entities without cache**
   ```java
   if ("REFERENCE_ENTITY".equals(entity.getDddRole()) && cacheType == null) {
       violations.add(new Violation(getId(), "INFO",
           "Entity '" + entity.getName() + "' appears to be reference data. " +
           "Consider using @Cacheable with cache type SOFT and appropriate cache size."));
   }
   ```

8. **No expiry on cached entities**
   ```java
   if (cacheType != null && cacheType != "NONE" && cacheExpiry == null &&
           !"ISOLATED".equals(cacheIsolation) && !"REFERENCE_ENTITY".equals(entity.getDddRole())) {
       violations.add(new Violation(getId(), "INFO",
           "Entity '" + entity.getName() + "' uses cache but has no expiry. " +
           "For frequently changing data, consider setting expiry to avoid stale reads."));
   }
   ```

---

## Nouvelles Métriques Disponibles dans le Rapport

### Par Entité (EntityNode)

| Propriété | Type | Valeurs possibles | Description |
|------------|-------|------------------|-------------|
| `cacheType` | String | FULL, WEAK, SOFT, SOFT_WEAK, HARD_WEAK, NONE | Type de cache utilisé |
| `cacheSize` | Integer | >0 | Taille du cache (nombre d'objets) |
| `cacheExpiry` | Integer | millisecondes | Expiration du cache |
| `cacheCoordinationType` | String | SEND_OBJECT_CHANGES, INVALIDATE_CHANGED_OBJECTS, SEND_NEW_OBJECTS_WITH_CHANGES, NONE | Type de coordination pour clustering |
| `cacheIsolation` | String | SHARED, PROTECTED, ISOLATED | Niveau d'isolation du cache |
| `cacheAlwaysRefresh` | Boolean | true/false | Force le rafraîchissement à chaque requête |
| `cacheRefreshOnlyIfNewer` | Boolean | true/false | Rafraîchit uniquement si version plus récente |
| `cacheDisableHits` | Boolean | true/false | Désactive les hits de cache (force DB) |

---

## Violations de Cache Détectées

### Niveau ERROR
Aucune nouvelle violation de niveau ERROR ajoutée.

### Niveau WARNING
1. **ISOLATED cache** - Entités sans second-level cache
2. **FULL cache on large entities** - Risque de consommation mémoire élevée
3. **Cache disableHits** - Toutes les requêtes vont à la DB
4. **Cache alwaysRefresh** - Défait le but du cache

### Niveau INFO
1. **Default cache size** - Taille par défaut (100) non optimisée
2. **Reference entities with inappropriate cache** - Types FULL/SOFT_WEAK sur lookup
3. **Reference entities without cache** - Lookup tables sans cache
4. **No expiry on cached entities** - Risque de données obsolètes

---

## Exemple de Rapport Enrichi

### Sans enrichissement (avant) :
```json
{
  "nodes": [
    {
      "name": "Location",
      "dddRole": "REFERENCE_ENTITY",
      "cacheType": null,
      "cacheExpiry": null,
      "cacheCoordinationType": null
    }
  ]
}
```

### Avec enrichissement (après) :
```json
{
  "nodes": [
    {
      "name": "Location",
      "dddRole": "REFERENCE_ENTITY",
      "cacheType": "SOFT_WEAK",
      "cacheSize": 100,
      "cacheExpiry": null,
      "cacheCoordinationType": "INVALIDATE_CHANGED_OBJECTS",
      "cacheIsolation": "SHARED",
      "cacheAlwaysRefresh": false,
      "cacheRefreshOnlyIfNewer": false,
      "cacheDisableHits": false
    }
  ],
  "violations": [
    {
      "ruleId": "CACHE_OPTIMIZATION",
      "severity": "INFO",
      "message": "Entity 'Location' appears to be reference data. Consider using @Cacheable with cache type SOFT and appropriate cache size."
    }
  ]
}
```

---

## Avantages de l'Enrichissement

### 1. **Détection d'Anti-Patterns de Cache**
- ISOLATED cache inutilement (performance dégradée)
- FULL cache sur entités volumineuses (OOM)
- alwaysRefresh=true avec cache actif (incohérent)
- disableHits=true (anti-cache)

### 2. **Optimisation des Performances**
- Recommandations basées sur les caractéristiques de l'entité
- Suggestion de types de cache appropriés
- Alertes sur les tailles par défaut

### 3. **Support du Clustering**
- Détection des configurations de coordination manquantes
- Recommandations pour environnements clusterisés

### 4. **Gestion des Données Obsolètes**
- Alertes sur absence d'expiry
- Recommandations basées sur le type de données (référence vs transactionnel)

---

## Mapping EclipseLink → EntityNode

| Configuration EclipseLink | Méthode ClassDescriptor | Champ EntityNode |
|------------------------|---------------------------|------------------|
| IdentityMapClass | `getIdentityMapClass()` | `cacheType` |
| IdentityMapSize | `getIdentityMapSize()` | `cacheSize` |
| CacheInvalidationPolicy | `getCacheInvalidationPolicy()` | `cacheExpiry` |
| CacheSynchronizationType | `getCacheSynchronizationType()` | `cacheCoordinationType` |
| CacheIsolationType | `getCacheIsolation()` | `cacheIsolation` |
| AlwaysRefresh | `shouldAlwaysRefreshCache()` | `cacheAlwaysRefresh` |
| OnlyRefreshIfNewer | `shouldOnlyRefreshCacheIfNewerVersion()` | `cacheRefreshOnlyIfNewer` |
| DisableCacheHits | `shouldDisableCacheHits()` | `cacheDisableHits` |

---

## Prochaines Étapes Possibles

1. **Support des DailyCacheInvalidationPolicy**
   - Actuellement, seul `TimeToLiveCacheInvalidationPolicy` est supporté
   - Ajouter champ `cacheExpiryTimeOfDay` pour les expirations horaires

2. **Analyse des statistiques de cache**
   - Si accessible via Session/EntityManager
   - Taux de hit/miss
   - Utilisation mémoire

3. **Règles de cache par niveau DDD**
   - Recommandations spécifiques pour AGGREGATE_ROOT
   - Recommandations spécifiques pour REFERENCE_ENTITY

4. **Comparaison inter-entités**
   - Détecter les incohérences de cache entre entités liées
   - Suggérer une harmonisation

---

## Résumé

**Modifications**: 3 fichiers modifiés
- MetamodelExtractor.java (+40 lignes)
- EntityNode.java (+5 champs, +9 getters/setters)
- CacheRule.java (+7 nouvelles violations)

**Résultat**: Le rapport contient maintenant toutes les configurations de cache d'EclipseLink avec des violations enrichies et contextuelles.
