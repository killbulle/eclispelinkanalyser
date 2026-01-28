# EclipseLink Analyzer 🔍

**EclipseLink Analyzer** is an advanced visualization and static analysis tool for JPA/EclipseLink projects. It extracts the persistence metamodel to generate an interactive map of your domain entities, detecting mapping anomalies, performance risks, and architectural issues.

## 🚀 Key Features

### 1. **Interactive Domain Graph**
*   **Visualize relationships**: One-to-One, One-to-Many, Many-to-Many, Inheritance.
*   **DDD Layers**: Automatically group entities by aggregates or Bounded Contexts.
*   **Virtual Nodes**: Distinct visualization for `MappedSuperclass` and `Embeddable`.

### 2. **Static Analysis & Diagnostics**
*   **⚠️ Schema Anomalies**: Detects broken mappings, missing IDs, or invalid cascade configurations.
*   **⚡ Performance Risks**: Identifies `EAGER` fetch types, N+1 problems, and large collections.
*   **🔄 Cycle Detection**:
    *   **Red Cycles**: Highlights complex dependency loops (A -> B -> C -> A) in **RED**.
    *   **Bidirectional**: Distinguishes simple bidirectional links (A <-> B) in Orange.

### 3. **Ergonomic Domain Views**
Switch between different perspectives using the unified dropdown:
*   **Overview**: Full graph.
*   **Bounded Contexts**: Group entities by package/module with visual frames.
*   **Aggregates**: Focus on root entities.

## 🛠️ Installation & Usage

### 1. Backend (Java)
Build the analyzer to generate the JSON report from your entities.

```bash
cd analyzer-backend
mvn clean install
# Run the analyzer (adjust main class if necessary)
mvn exec:java -Dexec.mainClass="com.eclipselink.analyzer.Main"
```

### 2. Frontend (React)
Visualize the generated report.

```bash
cd analyzer-frontend
npm install
npm run dev
```
Open `http://localhost:5173` and upload your `.json` report or select a demo scenario.
