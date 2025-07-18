# Data Watchlist Module

The data-watchlist module provides repository interfaces and entity definitions for the market watchlist subcollection in Firebase Firestore. This module handles CRUD operations for user watchlist items stored as a subcollection under user documents.

## Architecture

This module follows the Strategiz data layer architecture:
- **Entity Layer**: `entity/` package with `*Entity.java` naming convention
- **Repository Layer**: Repository interfaces defining data access contracts
- **Base Entity**: All entities extend `BaseEntity` for consistent audit field management

## Collection Structure

### Subcollection: `users/{userId}/watchlist`
The watchlist subcollection contains documents representing financial assets that users are monitoring:

```
users/
└── {userId}/
    └── watchlist/                    # Subcollection
        └── {watchlistItemId}/         # Document ID (auto-generated)
            ├── symbol                 # Trading symbol (BTC, AAPL, etc.)
            ├── name                   # Full asset name
            ├── type                   # Asset type (STOCK, CRYPTO, ETF, etc.)
            ├── exchange               # Exchange/market identifier
            ├── addedAt                # When added to watchlist
            ├── priority               # Display priority
            ├── notes                  # User notes
            └── [audit fields]         # From BaseEntity
```

## Entity

### WatchlistItemEntity
**File**: `entity/WatchlistItemEntity.java`
**Collection**: `users/{userId}/watchlist`
**Purpose**: Represents a financial asset in the user's market watchlist

#### Key Fields
- `itemId` (String): Unique identifier for the watchlist item (document ID)
- `symbol` (String): Trading symbol (e.g., "BTC", "AAPL", "GOOGL")
- `name` (String): Full name of the asset (e.g., "Bitcoin", "Apple Inc.")
- `type` (String): Asset type - STOCK, CRYPTO, ETF, FOREX, COMMODITY
- `exchange` (String): Exchange or market (e.g., "NASDAQ", "NYSE", "BINANCE")
- `addedAt` (Instant): Timestamp when added to watchlist
- `priority` (Integer): Display order priority (lower number = higher priority)
- `notes` (String): Optional user notes about the asset
- Inherits audit fields from BaseEntity: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

#### Validation
- Symbol and name are required fields (`@NotBlank`)
- Type must be one of the defined asset types
- Priority defaults to 0 if not specified

#### Usage
```java
WatchlistItemEntity item = new WatchlistItemEntity();
item.setSymbol("BTC");
item.setName("Bitcoin");
item.setType("CRYPTO");
item.setExchange("BINANCE");
item.setAddedAt(Instant.now());
item.setPriority(1);
```

## Repository Interface

### WatchlistItemRepository
**File**: `repository/WatchlistItemRepository.java`
**Purpose**: Data access contract for watchlist operations

#### Core CRUD Operations
```java
// Add item to watchlist
WatchlistItemEntity addToWatchlist(String userId, WatchlistItemEntity item);

// Get all watchlist items for user
List<WatchlistItemEntity> findByUserId(String userId);

// Get specific watchlist item
Optional<WatchlistItemEntity> findByUserIdAndItemId(String userId, String itemId);

// Update watchlist item
WatchlistItemEntity updateWatchlistItem(String userId, String itemId, 
                                       WatchlistItemEntity item);

// Remove from watchlist
void removeFromWatchlist(String userId, String itemId);
```

#### Search and Filter Operations
```java
// Find by symbol
Optional<WatchlistItemEntity> findByUserIdAndSymbol(String userId, String symbol);

// Get items by type
List<WatchlistItemEntity> findByUserIdAndType(String userId, String type);

// Get items by exchange
List<WatchlistItemEntity> findByUserIdAndExchange(String userId, String exchange);

// Search by symbol or name
List<WatchlistItemEntity> searchBySymbolOrName(String userId, String query);
```

#### Ordering and Priority Operations
```java
// Get ordered by priority
List<WatchlistItemEntity> findByUserIdOrderByPriority(String userId);

// Get ordered by date added
List<WatchlistItemEntity> findByUserIdOrderByAddedAt(String userId, boolean ascending);

// Update item priority
void updatePriority(String userId, String itemId, int priority);

// Reorder priorities
void reorderPriorities(String userId, Map<String, Integer> itemPriorities);
```

#### Utility Operations
```java
// Check if symbol exists in watchlist
boolean existsByUserIdAndSymbol(String userId, String symbol);

// Count watchlist items
long countByUserId(String userId);

// Count by type
long countByUserIdAndType(String userId, String type);

// Get recently added items
List<WatchlistItemEntity> findRecentlyAdded(String userId, int limit);
```

#### Bulk Operations
```java
// Add multiple items
List<WatchlistItemEntity> addMultipleToWatchlist(String userId, 
                                                 List<WatchlistItemEntity> items);

// Remove multiple items
void removeMultipleFromWatchlist(String userId, List<String> itemIds);

// Clear entire watchlist
void clearWatchlist(String userId);
```

## Usage Examples

### Adding to Watchlist
```java
@Autowired
private WatchlistItemRepository watchlistRepository;

// Add a cryptocurrency to watchlist
WatchlistItemEntity bitcoin = new WatchlistItemEntity();
bitcoin.setSymbol("BTC");
bitcoin.setName("Bitcoin");
bitcoin.setType("CRYPTO");
bitcoin.setExchange("BINANCE");
bitcoin.setAddedAt(Instant.now());
bitcoin.setPriority(1);
bitcoin.setNotes("Long-term hold");

WatchlistItemEntity saved = watchlistRepository.addToWatchlist("user123", bitcoin);
```

### Retrieving Watchlist
```java
// Get entire watchlist ordered by priority
List<WatchlistItemEntity> watchlist = 
    watchlistRepository.findByUserIdOrderByPriority("user123");

// Get only cryptocurrency items
List<WatchlistItemEntity> cryptoItems = 
    watchlistRepository.findByUserIdAndType("user123", "CRYPTO");

// Search for specific symbol
Optional<WatchlistItemEntity> apple = 
    watchlistRepository.findByUserIdAndSymbol("user123", "AAPL");
```

### Managing Priorities
```java
// Update single item priority
watchlistRepository.updatePriority("user123", "itemId456", 2);

// Reorder multiple items
Map<String, Integer> newOrder = new HashMap<>();
newOrder.put("itemId123", 1);
newOrder.put("itemId456", 2);
newOrder.put("itemId789", 3);
watchlistRepository.reorderPriorities("user123", newOrder);
```

### Bulk Operations
```java
// Add multiple stocks at once
List<WatchlistItemEntity> techStocks = Arrays.asList(
    new WatchlistItemEntity("AAPL", "Apple Inc.", "STOCK", "NASDAQ"),
    new WatchlistItemEntity("GOOGL", "Alphabet Inc.", "STOCK", "NASDAQ"),
    new WatchlistItemEntity("MSFT", "Microsoft Corp.", "STOCK", "NASDAQ")
);
watchlistRepository.addMultipleToWatchlist("user123", techStocks);

// Remove multiple items
List<String> itemsToRemove = Arrays.asList("itemId123", "itemId456");
watchlistRepository.removeMultipleFromWatchlist("user123", itemsToRemove);
```

## Implementation Notes

### Firebase Firestore Structure
- Stored as subcollection under `users/{userId}/watchlist`
- Each watchlist item is a separate document
- Document IDs are auto-generated by Firebase
- Supports real-time updates through Firestore listeners

### Indexing Strategy
- Composite index on userId + symbol for unique symbol lookups
- Index on userId + type for filtering by asset type
- Index on userId + priority for ordered retrieval
- Index on userId + addedAt for chronological queries

### Validation Rules
- Symbol must be uppercase (enforced at service layer)
- Type must match predefined enum values
- Maximum notes length enforced (e.g., 500 characters)
- Duplicate symbols prevented per user

### Performance Considerations
- Watchlist size should be limited (e.g., 100 items per user)
- Consider pagination for large watchlists
- Use batch operations for bulk updates
- Cache frequently accessed watchlists

## Asset Types

The module supports the following asset types:

### STOCK
- Traditional equities (AAPL, GOOGL, MSFT)
- Listed on major exchanges (NYSE, NASDAQ, etc.)

### CRYPTO
- Cryptocurrencies (BTC, ETH, SOL)
- Traded on crypto exchanges (Binance, Coinbase, Kraken)

### ETF
- Exchange-Traded Funds (SPY, QQQ, ARK)
- Traded like stocks on traditional exchanges

### FOREX
- Currency pairs (EUR/USD, GBP/JPY)
- Foreign exchange market

### COMMODITY
- Physical goods (Gold, Silver, Oil)
- Commodity futures and spot prices

## Integration with Other Modules

### Dependencies
- **data-base**: For BaseEntity and common data utilities
- **framework-common**: For shared constants and utilities

### Used By
- **business-watchlist**: Business logic for watchlist management
- **service-market**: REST APIs for watchlist operations
- **data-user**: For user data aggregation

## Security Considerations

### Data Access
- Watchlist items are user-specific and isolated
- Repository enforces userId in all operations
- No cross-user data access possible at this layer

### Sensitive Information
- User notes may contain trading strategies
- Symbol watching patterns could reveal investment interests
- Audit trail tracks all modifications

## Testing Guidelines

### Unit Tests
Focus on testing:
- CRUD operation contracts
- Symbol uniqueness validation
- Priority ordering logic
- Search functionality
- Bulk operation atomicity

### Integration Tests
Verify with Firebase:
- Subcollection creation and access
- Transaction handling for bulk operations
- Real-time update subscriptions
- Query performance with indexes

## Future Enhancements

### Potential Features
- Price alerts per watchlist item
- Historical price tracking
- Watchlist sharing between users
- Watchlist templates (e.g., "Tech Stocks", "Crypto Top 10")
- Integration with real-time market data
- Custom fields/metadata per item
- Watchlist categories/groups
- Import/export functionality

### Scalability Improvements
- Implement watchlist archiving
- Add server-side pagination
- Optimize queries with custom indexes
- Consider denormalization for performance
- Add caching layer for frequent reads