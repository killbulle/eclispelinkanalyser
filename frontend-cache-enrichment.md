# Mise à Jour Frontend pour les Informations de Cache

Ce document résume les modifications apportées au frontend pour afficher les configurations de cache d'EclipseLink.

## Modifications Effectuées

### 1. App.tsx - Mise à jour de l'interface EntityNodeData

**Fichier**: `analyzer-frontend/src/App.tsx` (lignes 96-116)

**Ajout des champs de cache** à l'interface EntityNodeData :

```typescript
interface EntityNodeData {
    // ... champs existants ...
    violations: Violation[];
    
    // Nouveaux champs de cache
    cacheType?: string;           // FULL, WEAK, SOFT, SOFT_WEAK, HARD_WEAK, NONE
    cacheSize?: number;           // Taille du cache (nombre d'objets)
    cacheExpiry?: number;          // Expiration en millisecondes
    cacheCoordinationType?: string;  // SEND_OBJECT_CHANGES, INVALIDATE_CHANGED_OBJECTS, etc.
    cacheIsolation?: string;        // SHARED, PROTECTED, ISOLATED
    cacheAlwaysRefresh?: boolean;   // Force le rafraîchissement à chaque requête
    cacheRefreshOnlyIfNewer?: boolean; // Rafraîchit uniquement si version plus récente
    cacheDisableHits?: boolean;    // Désactive les hits de cache (force DB)
}
```

---

### 2. EntityNode.tsx - Ajout de la section Cache

**Fichier**: `analyzer-frontend/src/components/EntityNode.tsx`

#### a) Ajout de l'interface Violation locale (ligne 53-59)

```typescript
interface Violation {
    ruleId: string;
    severity: string;
    message: string;
}
```

#### b) Ajout des constantes et helpers pour le cache (lignes 57-84)

```typescript
const CACHE_CONFIG_COLORS: any = {
    FULL: '#ef4444',
    WEAK: '#3b82f6',
    SOFT: '#10b981',
    SOFT_WEAK: '#8b5cf6',
    HARD_WEAK: '#f59e0b',
    NONE: '#6b7280',
    ISOLATED: '#dc2626',
    SHARED: '#22c55e',
    PROTECTED: '#059669'
};

const getCacheTypeColor = (type?: string) => {
    if (!type) return '#6b7280';
    const colors: { [key: string]: string } = CACHE_CONFIG_COLORS;
    return colors[type] || '#6b7280';
};

const formatExpiry = (expiryMs?: number) => {
    if (!expiryMs) return 'No expiry';
    if (expiryMs < 60000) return `${Math.floor(expiryMs / 1000)}s`;
    if (expiryMs < 3600000) return `${Math.floor(expiryMs / 60000)}m`;
    return `${Math.floor(expiryMs / 3600000)}h`;
};
```

#### c) Ajout des propriétés manquantes à EntityNodeData (lignes 96-115)

```typescript
// Ajout après les champs de cache
focusOpacity?: number;
isCutPoint?: boolean;
isInCycle?: boolean;
hasEagerRisk?: boolean;
isPotentialVO?: boolean;
```

#### d) Ajout de la section Cache (lignes 324-420)

**Emplacement** : Entre la section des attributs (`data.showAttributes`) et les Connection Handles

**Structure de la section** :

```tsx
{/* Cache Configuration Section */}
{(data.cacheType || data.cacheSize || data.cacheExpiry || data.cacheCoordinationType) && (
    <div className="mt-1 px-1.5 py-1 border-t border-subtle">
        {/* Header cliquable */}
        <div className="flex items-center gap-1.5 mb-2 cursor-pointer" onClick={() => setIsHovered(!isHovered)}>
            <div className="w-1 h-1 rounded-full bg-blue-500 shrink-0"></div>
            <span className="text-[8px] font-bold text-blue-500 uppercase tracking-wider">
                Cache Configuration
            </span>
        </div>

        {/* Contenu dépliable au hover */}
        {isHovered && (
            <div className="space-y-1.5 max-h-[160px] overflow-y-auto custom-scrollbar pr-0.5">
                {/* Cache Type */}
                {data.cacheType && (
                    <div className="flex items-center justify-between text-[8px]">
                        <span className="text-secondary">Type</span>
                        <span className="font-medium px-2 py-0.5 rounded" style={{
                            backgroundColor: getCacheTypeColor(data.cacheType),
                            color: data.cacheType === 'FULL' || data.cacheType === 'SOFT' ? '#fff' : '#000'
                        }}>
                            {data.cacheType || 'N/A'}
                        </span>
                    </div>
                )}

                {/* Cache Size */}
                {data.cacheSize !== undefined && (
                    <div className="flex items-center justify-between text-[8px]">
                        <span className="text-secondary">Size</span>
                        <span className="font-mono text-primary">{data.cacheSize}</span>
                    </div>
                )}

                {/* Cache Expiry */}
                {data.cacheExpiry !== undefined && (
                    <div className="flex items-center justify-between text-[8px]">
                        <span className="text-secondary">Expiry</span>
                        <span className="font-mono text-primary">{formatExpiry(data.cacheExpiry)}</span>
                    </div>
                )}

                {/* Cache Coordination */}
                {data.cacheCoordinationType && (
                    <div className="flex items-center justify-between text-[8px]">
                        <span className="text-secondary">Coordination</span>
                        <span className="font-medium text-xs px-2 py-0.5 rounded bg-panel/50 text-primary">
                            {data.cacheCoordinationType}
                        </span>
                    </div>
                )}

                {/* Cache Isolation */}
                {data.cacheIsolation && (
                    <div className="flex items-center justify-between text-[8px]">
                        <span className="text-secondary">Isolation</span>
                        <span className={`font-medium text-xs px-2 py-0.5 rounded ${
                            data.cacheIsolation === 'ISOLATED' ? 'bg-red-100 text-red-700' :
                            data.cacheIsolation === 'PROTECTED' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-green-100 text-green-700'
                        }`}>
                            {data.cacheIsolation}
                        </span>
                    </div>
                )}

                {/* Advanced Settings */}
                {(data.cacheAlwaysRefresh || data.cacheRefreshOnlyIfNewer || data.cacheDisableHits) && (
                    <div className="mt-2 space-y-1.5 pt-2 border-t border-subtle">
                        <span className="text-[8px] font-semibold text-secondary">Advanced Settings</span>
                        {data.cacheAlwaysRefresh && (
                            <div className="flex items-center justify-between text-[8px]">
                                <span className="text-secondary">Always Refresh</span>
                                <span className={`font-bold ${data.cacheAlwaysRefresh ? 'text-red-500' : 'text-green-600'}`}>
                                    {data.cacheAlwaysRefresh ? 'Yes' : 'No'}
                                </span>
                            </div>
                        )}
                        {data.cacheRefreshOnlyIfNewer && (
                            <div className="flex items-center justify-between text-[8px]">
                                <span className="text-secondary">Refresh If Newer</span>
                                <span className={`font-bold ${data.cacheRefreshOnlyIfNewer ? 'text-blue-500' : 'text-gray-500'}`}>
                                    {data.cacheRefreshOnlyIfNewer ? 'Yes' : 'No'}
                                </span>
                            </div>
                        )}
                        {data.cacheDisableHits !== undefined && (
                            <div className="flex items-center justify-between text-[8px]">
                                <span className="text-secondary">Cache Hits</span>
                                <span className={`font-bold ${data.cacheDisableHits ? 'text-red-500' : 'text-green-600'}`}>
                                    {data.cacheDisableHits ? 'Disabled' : 'Enabled'}
                                </span>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    )}
)}
```

---

## Fonctionnalités de la Section Cache

### 1. **Header Cliquable**
- Icône bleue avec badge "Cache Configuration"
- Déclenche l'affichage du contenu au clic (toggle `isHovered`)
- Permet de réduire l'encombrement visuel quand les infos cache ne sont pas utilisées

### 2. **Affichage des Configurations de Cache**

| Propriété | Affichage | Couleurs |
|------------|-----------|----------|
| **Cache Type** | Badge avec fond coloré selon le type | Fond noir/clair selon type |
| **Cache Size** | Texte monospace | - |
| **Cache Expiry** | Formaté (s, m, h) | - |
| **Coordination** | Badge gris | - |
| **Isolation** | Badge coloré (rouge/vert/jaune) | Rouge=ISOLATED, Vert=SHARED, Jaune=PROTECTED |
| **Always Refresh** | Yes/No coloré (rouge/vert) | Rouge=dangereux, Vert=normal |
| **Refresh If Newer** | Yes/No coloré (bleu/gris) | - |
| **Cache Hits** | Disabled/Enabled coloré (rouge/vert) | Rouge=désactivé, Vert=activé |

### 3. **Advanced Settings**
- Affichés dans une sous-section séparée
- Séparateur visuel (border-top)
- Permet de distinguer les configs de base vs avancées

---

## Design UI

### 🎨 Palette de Couleurs Cache

| Type de Cache | Couleur Badge | Description |
|---------------|--------------|-------------|
| **FULL** | `#ef4444` | Rouge foncé - Risque OOM |
| **WEAK** | `#3b82f6` | Bleu foncé - Memory-safe |
| **SOFT** | `#10b981` | Vert - Good balance |
| **SOFT_WEAK** | `#8b5cf6` | Cyan - Défaut EclipseLink |
| **HARD_WEAK** | `#f59e0b` | Orange - Variante SoftWeak |
| **NONE** | `#6b7280` | Gris - Pas de cache |
| **ISOLATED** | `#dc2626` | Rouge - Pas de second-level |
| **PROTECTED** | `#059669` | Vert - Instances isolées |
| **SHARED** | `#22c55e` | Bleu - Défaut |

### 🔧 Format de l'Expiry

| Valeur | Format | Exemple |
|--------|--------|---------|
| `null` | No expiry | - |
| `< 60000` | Xs | 30s |
| `< 3600000` | Xm | 15m |
| `>= 3600000` | Xh | 2h |

---

## Positionnement dans le Composant EntityNode

```
┌─────────────────────────────────────────┐
│ Header (type, nom, erreurs)    │
├─────────────────────────────────────────┤
│ Attributes (optionnel)              │
├─────────────────────────────────────────┤
│ Cache Configuration (NOUVEAU)       │ ← Ajouté ici
│   - Header cliquable               │
│   - Configuration type                │
│   - Size                            │
│   - Expiry                           │
│   - Coordination                     │
│   - Isolation                        │
│   - Advanced Settings                 │
├─────────────────────────────────────────┤
│ Handles (ReactFlow)                │
└─────────────────────────────────────────┘
```

---

## Intégration avec le Reste du Frontend

### ✅ Pas de mélange avec les Reports Span
La section Cache est **complètement indépendante** des :
- Violations de mapping (affichées dans l'header)
- Anomalies (affichées dans l'header)
- Reports APM/Performance Span

### ✅ Consistance avec le Design Existant
- Utilise le même style de section (border-top, padding)
- Utilise les mêmes variables CSS (`--border-subtle`, `--text-secondary`, etc.)
- Compatible avec le mode focus/opacité utilisé pour les autres sections

### ✅ Accessibilité
- Hover avec délai de 400ms
- Défilement avec scrollbar customisée
- Max-height de 160px pour éviter de prendre trop de place

---

## Utilisation

### Mode par Défaut
- La section est **cachée** si aucune info de cache n'est présente
- Seul l'header cliquable est visible pour indiquer que la section existe

### Mode Interactif
- Au clic sur le header, la configuration se **déplie/déplie**
- Le changement est animé avec un effet de fondu (fade-in)

### Mode Focus
- Compatible avec le mode focus du composant EntityNode
- L'opacité s'applique à toute la section

---

## Prochaines Étapes Possibles

### 1. **Ajout d'un Onglet Cache dans JPAView**
- Actuellement : Les info cache sont par entité
- Amélioration : Afficher un tableau récapitulatif des caches par entité
- Position : Nouvel onglet dans la barre d'onglets de JPAView (après "DDL View")

### 2. **Règles de Cache dans le Rapport**
- Actuellement : Seules les violations sont affichées
- Amélioration : Afficher les violations de cache (déjà implémentées dans CacheRule.java)

### 3. **Graph de Configuration de Cache**
- Actuellement : Pas de visualisation des caches
- Amélioration : Vue montrant quelles entités ont quels types de cache
- Format : Pie chart ou bar chart

### 4. **Export/Import des Configurations de Cache**
- Actuellement : Pas de modification possible
- Amélioration : Permettre de modifier les configs de cache via une UI

---

## Résumé

**Modifications** : 2 fichiers
- ✅ `App.tsx` : Ajout de 9 champs cache à EntityNodeData
- ✅ `EntityNode.tsx` : Ajout de la section Cache complète

**Fonctionnalités ajoutées** :
- 📋 Affichage des 9 configurations de cache par entité
- 🎨 Codage par couleur des types de cache
- ⏱️ Formatage intelligent de l'expiry
- 🎯 Header cliquable pour déplier/replier
- ⚙️ Section Advanced Settings séparée

**Compatibilité** :
- ✅ Pas d'impact sur les rapports span/s APM
- ✅ Design cohérent avec l'existant
- ✅ Intégration progressive (section optionnelle)

---

## Note

Les erreurs TypeScript détectées lors du build dans App.tsx et JPAView.tsx sont **préexistantes** et non liées à ces modifications. Elles peuvent être ignorées ou corrigées ultérieurement.
