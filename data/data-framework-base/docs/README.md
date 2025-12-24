# 🔥 Strategiz Data Base Module

**Rock solid, brain-dead simple data persistence for all platform entities.**

## 🎯 Ultra Simple Usage

### 1. Create an Entity
```java
@Document
public class WatchlistItem extends BaseEntity {
    private String id;
    private String symbol;
    private String userId;
    
    public WatchlistItem() {}
    
    public WatchlistItem(String symbol, String userId) {
        super(userId); // Initializes audit fields automatically
        this.symbol = symbol;
        this.userId = userId;
    }
    
    @Override
    public String getId() { return id; }
    
    @Override
    public void setId(String id) { this.id = id; }
    
    @Override
    public String getCollectionName() { return "watchlist_items"; }
    
    // Getters and setters...
}
```

### 2. Create a Repository
```java
@Repository
public class WatchlistRepository extends BaseRepository<WatchlistItem> {
    
    public WatchlistRepository(Firestore firestore) {
        super(firestore, WatchlistItem.class);
    }
    
    // Add custom query methods if needed
    public CompletableFuture<List<WatchlistItem>> findByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            // Custom implementation
        });
    }
}
```

### 3. Use in Service
```java
@Service
public class WatchlistService {
    
    private final WatchlistRepository repository;
    
    public WatchlistItem addToWatchlist(String userId, String symbol) {
        WatchlistItem item = new WatchlistItem(symbol, userId);
        return repository.save(item, userId); // ONE method for create/update!
    }
    
    public Optional<WatchlistItem> getWatchlistItem(String id) {
        return repository.findById(id); // Only returns active items
    }
    
    public boolean removeFromWatchlist(String id, String userId) {
        return repository.delete(id, userId); // Soft delete
    }
}
```

## 🚀 That's It!

**Three simple rules:**
1. **Extend `BaseEntity`** for all your entities
2. **Extend `BaseRepository`** for all your repositories  
3. **Use `repository.save(entity, userId)`** for everything

The system automatically handles:
- ✅ Audit fields (createdBy, modifiedBy, timestamps, version)
- ✅ Create vs Update detection
- ✅ ID generation
- ✅ Soft deletes
- ✅ Validation
- ✅ Optimistic locking

## 📊 Firestore Document Structure

Every entity automatically gets this structure:
```json
{
  "id": "item_123",
  "symbol": "AAPL", 
  "userId": "user_456",
  "auditFields": {
    "createdBy": "user_456",
    "modifiedBy": "user_789",
    "createdDate": "2024-01-15T10:30:00.000Z",
    "modifiedDate": "2024-01-16T14:45:30.000Z", 
    "version": 3,
    "isActive": true
  }
}
```

## 🔧 Advanced Usage

### Bulk Operations
```java
List<WatchlistItem> items = Arrays.asList(item1, item2, item3);
List<WatchlistItem> saved = repository.saveAll(items, userId); // Atomic batch operation
```

### Entity Status Checks
```java
if (entity.isActive()) {
    // Entity is active
}

if (entity.isDeleted()) {
    // Entity is soft-deleted
}

String creator = entity.getCreatedBy();
Long version = entity.getVersion();
```

### Restore Deleted Items
```java
repository.restore(itemId, userId); // Restore soft-deleted item
```

### Custom Queries in Repository
```java
public CompletableFuture<List<WatchlistItem>> findBySymbol(String symbol) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            Query query = getCollection()
                .whereEqualTo("symbol", symbol)
                .whereEqualTo("isActive", true);
                
            return query.get().get().getDocuments().stream()
                .map(doc -> {
                    WatchlistItem item = doc.toObject(WatchlistItem.class);
                    item.setId(doc.getId());
                    return item;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    });
}
```

## 🛡️ Error Handling

The system automatically prevents:
- ❌ Saving entities without audit fields
- ❌ Invalid user IDs (null/empty)
- ❌ Corrupted audit data
- ❌ Concurrent modification conflicts (optimistic locking)

## 🔄 Migration from Existing Entities

If you have existing entities without audit fields:

1. **Change inheritance:**
   ```java
   // Before
   public class MyEntity {
   
   // After  
   public class MyEntity extends BaseEntity {
   ```

2. **Remove manual audit fields:**
   ```java
   // Remove these - handled automatically now
   // private String createdBy;
   // private Date createdAt;
   // private String modifiedBy;
   // private Date modifiedAt;
   ```

3. **Update repository:**
   ```java
   // Before
   public class MyRepository {
   
   // After
   public class MyRepository extends BaseRepository<MyEntity> {
   ```

4. **Update service calls:**
   ```java
   // Before
   repository.create(entity);
   repository.update(entity);
   
   // After
   repository.save(entity, userId); // One method!
   ```

## 💡 Why This Design?

**Rock Solid:**
- No way to forget audit fields
- No way to save invalid data
- Automatic conflict prevention
- Built-in soft delete safety

**Brain-Dead Simple:**
- One base class to extend
- One method to save anything
- No audit field management needed
- Works the same everywhere

**Performance Optimized:**
- Async operations with CompletableFuture
- Bulk operations with atomic batches
- Efficient Firestore queries
- Automatic active filtering

## 🏗️ Architecture

```
BaseEntity (audit fields + validation)
    ↓
BaseRepository (CRUD + audit enforcement)
    ↓  
YourRepository (custom queries)
    ↓
YourService (business logic)
    ↓
YourController (REST API)
```

**Every layer is simple, focused, and testable.**