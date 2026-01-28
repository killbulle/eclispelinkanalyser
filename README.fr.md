# EclipseLink Analyzer 🔍

**EclipseLink Analyzer** est un outil de visualisation avancée et d'analyse statique pour les projets JPA/EclipseLink. Il extrait le méta-modèle de persistance pour générer une carte interactive de vos entités, détectant automatiquement les anomalies de mapping, les risques de performance et les problèmes architecturaux.

## 🚀 Fonctionnalités Clés

### 1. **Graphe de Domaine Interactif**
*   **Visualisation des relations** : One-to-One, One-to-Many, Many-to-Many, Héritage.
*   **Couches DDD** : Regroupement automatique par Agrégats ou Bounded Contexts.
*   **Noeuds Virtuels** : Visualisation distincte pour `MappedSuperclass` et `Embeddable` (bordures en pointillés).

### 2. **Analyse Statique & Diagnostics**
*   **⚠️ Anomalies de Schéma** : Détecte les mappings cassés, IDs manquants ou cascades invalides.
*   **⚡ Risques de Performance** : Identifie les fetch `EAGER`, les problèmes N+1 et les collections trop larges.
*   **🔄 Détection de Cycles** :
    *   **Cycles Rouges** : Met en évidence les boucles de dépendance complexes (A -> B -> C -> A) en **ROUGE**.
    *   **Bidirectionnels** : Distingue les liens simples (A <-> B) en Orange/Jaune.

### 3. **Vues Ergonomiques**
Basculez entre différentes perspectives via le menu déroulant unifié :
*   **Vue d'ensemble** : Graphe complet.
*   **Bounded Contexts** : Regroupe les entités par package avec des cadres visuels ("Clusters").
*   **Aggregates** : Focus sur les entités racines.

## 🛠️ Installation & Utilisation

### 1. Backend (Java)
Compilez l'analyseur pour générer le rapport JSON à partir de vos entités.

```bash
cd analyzer-backend
mvn clean install
# Lancer l'analyseur
mvn exec:java -Dexec.mainClass="com.eclipselink.analyzer.Main"
```

### 2. Frontend (React)
Visualisez le rapport généré.

```bash
cd analyzer-frontend
npm install
npm run dev
```
Ouvrez `http://localhost:5173` et uploadez votre rapport JSON ou sélectionnez un scénario de démonstration.
