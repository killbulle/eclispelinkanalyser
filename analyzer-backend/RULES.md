# EclipseLink JPA Analyzer - Extended Rules Documentation

## Overview

This document describes the new analysis rules added to the EclipseLink JPA Analyzer to provide comprehensive coverage of JPA and EclipseLink-specific annotations and patterns.

## New Attribute-Level Rules

### LobRule (`LOB_ANNOTATION`)
**Severity**: WARNING  
**Purpose**: Checks `@Lob` annotation usage and provides performance recommendations for large object fields.

**Checks**:
- Detects `@Lob` fields and recommends lazy loading
- Validates database type compatibility (BLOB/CLOB vs inappropriate types)
- Warns against using `@Lob` fields as `@Id` or `@Version`
- Suggests external storage for very large files (>1MB)

**Example Violations**:
- "Attribute 'content' uses @Lob annotation. Consider using lazy loading for Lob fields to avoid performance issues."
- "Attribute 'pdfData' has database type 'VARCHAR' which may not be optimal for @Lob. Consider using BLOB for byte[] or CLOB for String."

### TemporalRule (`TEMPORAL_ANNOTATION`)
**Severity**: WARNING  
**Purpose**: Validates `@Temporal` annotation usage and ensures proper temporal type handling.

**Checks**:
- Validates Java type compatibility (Date, Calendar, java.time types)
- Ensures `TemporalType` is specified (DATE, TIME, TIMESTAMP)
- Matches Java type with appropriate TemporalType (LocalDate → DATE, LocalDateTime → TIMESTAMP)
- Recommends migration from legacy Date/Calendar to java.time API
- Warns about `@Temporal` on `@Version` fields

**Example Violations**:
- "Attribute 'eventDate' uses @Temporal but has incompatible Java type 'String'."
- "Attribute 'created' uses legacy Date/Calendar with @Temporal. Consider migrating to java.time API."

### VersionRule (`VERSION_ANNOTATION`)
**Severity**: WARNING  
**Purpose**: Checks `@Version` field usage and ensures proper optimistic locking configuration.

**Checks**:
- Verifies entity has a `@Version` field (recommends adding if missing)
- Validates version field type (Long, Integer, Short, Timestamp, Instant)
- Recommends Long for high-concurrency systems
- Ensures version field is not nullable
- Detects multiple `@Version` fields (error)
- Provides guidance on timestamp-based versioning

**Example Violations**:
- "Entity 'Document' does not have a @Version field. Consider adding optimistic locking support."
- "Version field 'version' has type 'String'. @Version should use Long, Integer, Short, Timestamp, or Instant."

## New Relationship-Level Rules

### BatchFetchRule (`BATCH_FETCH`)
**Severity**: INFO  
**Purpose**: Recommends batch fetching annotations (`@BatchFetch`) to optimize N+1 query scenarios.

**Checks**:
- Identifies collection relationships without batch fetching
- Validates batch fetch type (JOIN, EXISTS, IN)
- Detects conflicts between `@JoinFetch` and `@BatchFetch`
- Recommends batch fetching for lazy collections
- Suggests global batch fetching configuration for entities with multiple collections

**Example Violations**:
- "Collection relationship 'tags' does not use batch fetching. Consider adding @BatchFetch to optimize N+1 queries."
- "Relationship 'department' uses @JoinFetch but not @BatchFetch. Combine both for optimal performance."

## New Inheritance Rules

### InheritanceStrategyRule (`INHERITANCE_STRATEGY`)
**Severity**: INFO  
**Purpose**: Analyzes inheritance strategy and provides recommendations for optimal performance.

**Checks**:
- Detects missing inheritance strategy in child entities
- Validates inheritance strategy (JOINED, SINGLE_TABLE, TABLE_PER_CLASS)
- Provides strategy-specific recommendations:
  - JOINED: Ensure foreign key indexes, consider SINGLE_TABLE for simple hierarchies
  - SINGLE_TABLE: Monitor table growth, consider partitioning
  - TABLE_PER_CLASS: Warns about UNION query performance
- Validates MappedSuperclass usage

**Example Violations**:
- "Entity 'Car' extends 'Vehicle' but does not specify inheritance strategy. Add @Inheritance annotation."
- "Entity 'Project' uses JOINED inheritance. Ensure foreign key indexes are created for performance."

### DiscriminatorRule (`DISCRIMINATOR_USAGE`)
**Severity**: WARNING  
**Purpose**: Checks discriminator column and value usage in inheritance hierarchies.

**Checks**:
- Ensures SINGLE_TABLE strategy has discriminator column
- Validates discriminator column name length (<31 chars)
- Checks discriminator value specification for concrete entities
- Validates discriminator value length and character set
- Detects discriminator column usage with TABLE_PER_CLASS (not needed)

**Example Violations**:
- "Entity 'Vehicle' uses SINGLE_TABLE inheritance but has no discriminator column. Add @DiscriminatorColumn."
- "Discriminator value 'VeryLongValueName' is too long (>50 chars). Keep discriminator values short."

## New Optimization Rules

### NPlusOneQueryRule (`N_PLUS_ONE_QUERY`)
**Severity**: WARNING  
**Purpose**: Detects potential N+1 query patterns in entity relationships.

**Checks**:
- Identifies lazy collections without batch fetching
- Detects multiple lazy collections causing multiple N+1 patterns
- Flags eager collections that cause Cartesian product issues
- Warns about deep relationship chains increasing N+1 risk
- Recommends entity graphs or fetch plans for complex graphs

**Example Violations**:
- "Lazy collection 'orders' causes N+1 queries when accessed. Add @BatchFetch or use JOIN FETCH in queries."
- "Entity 'Customer' has 3 lazy collections. Accessing multiple collections will cause multiple N+1 query patterns."

### CartesianProductRule (`CARTESIAN_PRODUCT`)
**Severity**: ERROR  
**Purpose**: Detects potential Cartesian product issues from multiple eager relationships.

**Checks**:
- Flags multiple eager relationships causing Cartesian explosion
- Detects eager collection relationships (especially problematic)
- Identifies multiple `@JoinFetch` annotations causing Cartesian products
- Warns about bidirectional eager relationships causing circular loading
- Recommends changing all eager relationships to LAZY

**Example Violations**:
- "Entity 'Order' has 2 eager relationships: 'customer', 'items'. This will cause Cartesian product explosion."
- "Eager collection 'orderLines' will cause Cartesian product when combined with other relationships."

### IndexRule (`INDEX_OPTIMIZATION`)
**Severity**: INFO  
**Purpose**: Recommends database indexes based on relationships and query patterns.

**Checks**:
- Suggests indexes for foreign key columns (owning side of relationships)
- Recommends indexes for join table columns in ManyToMany relationships
- Identifies unique constraint fields that need unique indexes
- Suggests indexes for temporal fields used in range queries
- Warns against indexing Lob fields
- Recommends composite indexes for common query patterns

**Example Violations**:
- "Relationship 'department' creates a foreign key. Add database index for this column."
- "Temporal attribute 'createdDate' is often used in date range queries. Consider adding an index."

## Usage Examples

### Annotation Model Example
The new `Annotation Model` (annotation-report.json) demonstrates all new annotations:

1. **Document Entity**: `@Lob` fields with lazy loading recommendations
2. **Tag Entity**: `@BatchFetch` with JOIN type
3. **Event Entity**: `@Temporal` with DATE, TIME, TIMESTAMP types
4. **VersionedEntity**: `@Version` with Integer type
5. **Vehicle Inheritance**: `@DiscriminatorColumn` with SINGLE_TABLE strategy
6. **Department/Employee**: Cascade types and orphan removal

### Running the Analysis
All new rules are automatically included in the analysis when running the backend. The rules appear in the violations panel with appropriate severity levels.

## Configuration

No additional configuration is required. The rules are registered in `Main.java` and automatically applied to all entity analyses.

## Severity Levels

- **ERROR**: Critical issues that will cause runtime errors or severe performance problems
- **WARNING**: Issues that may cause performance degradation or unexpected behavior
- **INFO**: Recommendations for optimization and best practices

## Future Extensions

1. **CacheableRule**: Check `@Cacheable` and `@ReadOnly` annotation usage (requires cache metadata fields in EntityNode)
2. **ConverterRule**: Validate `@Converter` and `@Convert` annotation usage
3. **SecondaryTableRule**: Analyze `@SecondaryTable` usage and performance implications
4. **FetchProfileRule**: Recommend EclipseLink fetch profiles for complex queries

---

*Last Updated: January 2025*