# Repository Pattern Guide

This document describes the standard repository pattern used across all data modules in the Strategiz platform.

## Architecture Overview

The repository layer follows a CRUD-separated pattern using Spring Data Firestore:

```
data-{module}/
├── entity/
│   └── {Entity}.java          # @Document annotated entity
├── repository/
│   ├── {Entity}SpringDataRepository.java      # Spring Data interface (auto-implemented)
│   ├── Create{Entity}Repository.java          # CRUD interface
│   ├── Create{Entity}RepositoryImpl.java      # CRUD implementation
│   ├── Read{Entity}Repository.java            # CRUD interface  
│   ├── Read{Entity}RepositoryImpl.java        # CRUD implementation
│   ├── Update{Entity}Repository.java          # CRUD interface
│   ├── Update{Entity}RepositoryImpl.java      # CRUD implementation
│   ├── Delete{Entity}Repository.java          # CRUD interface
│   └── Delete{Entity}RepositoryImpl.java      # CRUD implementation
```

## Implementation Steps

### 1. Add Spring Data Firestore Dependency

In your data module's `pom.xml`:

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-data-firestore</artifactId>
</dependency>
```

### 2. Annotate Entity

```java
@Document(collectionName = "your-collection-name")
@JsonIgnoreProperties(ignoreUnknown = true)
public class YourEntity extends BaseEntity {
    
    @DocumentId
    @JsonProperty("id")
    private String id;
    
    // Other fields...
}
```

### 3. Create Spring Data Repository

```java
@Repository
public interface YourEntitySpringDataRepository extends FirestoreRepository<YourEntity, String> {
    
    // Spring Data auto-implements these based on method names
    List<YourEntity> findByUserId(String userId);
    Optional<YourEntity> findByIdAndUserId(String id, String userId);
    // Add other query methods as needed
}
```

### 4. Create CRUD Repositories

#### Interface Pattern
```java
public interface CreateYourEntityRepository {
    YourEntity create(YourEntity entity);
    YourEntity createWithUserId(YourEntity entity, String userId);
}
```

#### Implementation Pattern
```java
@Repository
public class CreateYourEntityRepositoryImpl implements CreateYourEntityRepository {
    
    private final YourEntitySpringDataRepository springDataRepository;
    
    @Autowired
    public CreateYourEntityRepositoryImpl(YourEntitySpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }
    
    @Override
    public YourEntity create(YourEntity entity) {
        // Business logic (ID generation, defaults, etc.)
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(UUID.randomUUID().toString());
        }
        
        // Use Spring Data repository
        return springDataRepository.save(entity);
    }
    
    @Override
    public YourEntity createWithUserId(YourEntity entity, String userId) {
        entity.setUserId(userId);
        
        // Initialize audit fields
        if (!entity._hasAudit()) {
            entity._initAudit(userId);
        }
        
        return create(entity);
    }
}
```

### 5. Follow CRUD Separation

- **CreateRepository**: Only create operations
- **ReadRepository**: Only read/query operations (includes filtering soft-deleted)
- **UpdateRepository**: Only update operations (includes audit field updates)
- **DeleteRepository**: Both soft and hard delete operations

### 6. Service Layer Integration

Services should use the CRUD repositories directly:

```java
@Service
public class YourEntityService {
    
    private final CreateYourEntityRepository createRepository;
    private final ReadYourEntityRepository readRepository;
    // etc.
    
    @Autowired
    public YourEntityService(
            CreateYourEntityRepository createRepository,
            ReadYourEntityRepository readRepository) {
        this.createRepository = createRepository;
        this.readRepository = readRepository;
    }
}
```

## Key Principles

1. **Single Responsibility**: Each CRUD repository handles only one type of operation
2. **Spring Data Magic**: Let Spring auto-implement the data access layer
3. **Soft Delete Aware**: Always filter out soft-deleted entities in Read operations
4. **Audit Fields**: Use BaseEntity's audit system for tracking changes
5. **User Ownership**: Enforce user-based access control where applicable

## Benefits

- **Clean Separation**: Clear responsibility boundaries
- **Spring Data Power**: Automatic query implementation based on method names
- **Testability**: Easy to mock individual CRUD operations
- **Consistency**: Standardized pattern across all data modules
- **Performance**: Leverages Firestore's native querying capabilities

## Collection Naming

Collections are automatically created based on the `@Document(collectionName = "...")` annotation. Use plural, lowercase names (e.g., "strategies", "users", "portfolios").