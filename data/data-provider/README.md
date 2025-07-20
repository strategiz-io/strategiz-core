# Data Providers Module

The data-providers module provides repository interfaces and entity definitions for the connected providers subcollection in Firebase Firestore. This module manages the user's connections to various trading platforms, brokers, and financial service providers.

## Architecture

This module follows the Strategiz data layer architecture:
- **Entity Layer**: `entity/` package with `*Entity.java` naming convention
- **Repository Layer**: Repository interfaces defining data access contracts
- **Base Entity**: All entities extend `BaseEntity` for consistent audit field management

## Collection Structure

### Subcollection: `users/{userId}/providers`
The providers subcollection contains documents representing connected financial service providers:

```
users/
└── {userId}/
    └── providers/                     # Subcollection
        └── {providerId}/              # Document ID (auto-generated)
            ├── providerName           # Provider identifier (kraken, binance, etc.)
            ├── providerType           # Type of provider
            ├── accountType            # Real or paper trading
            ├── displayName            # User-friendly name
            ├── status                 # Connection status
            ├── connectedAt            # Connection timestamp
            ├── lastSyncAt             # Last data sync
            ├── configuration          # Provider-specific config
            ├── metadata               # Additional provider data
            └── [audit fields]         # From BaseEntity
```

## Entity

### ConnectedProviderEntity
**File**: `entity/ConnectedProviderEntity.java`
**Collection**: `users/{userId}/providers`
**Purpose**: Represents a connection to a financial service provider

#### Key Fields
- `providerId` (String): Unique identifier for the connection (document ID)
- `providerName` (String): Provider identifier (e.g., "kraken", "binance", "schwab")
- `providerType` (String): Type - EXCHANGE, BROKER, BANK, DATA_PROVIDER
- `accountType` (String): REAL or PAPER (demo/sandbox)
- `displayName` (String): User-friendly display name
- `status` (String): ACTIVE, INACTIVE, ERROR, PENDING
- `connectedAt` (Instant): When the provider was connected
- `lastSyncAt` (Instant): Last successful data synchronization
- `lastErrorAt` (Instant): Last error occurrence
- `errorMessage` (String): Latest error message if any
- `configuration` (Map<String, Object>): Provider-specific settings
- `metadata` (Map<String, Object>): Additional provider metadata
- Inherits audit fields from BaseEntity: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

#### Provider Types
- **EXCHANGE**: Cryptocurrency exchanges (Binance, Kraken, Coinbase)
- **BROKER**: Traditional brokers (Charles Schwab, Fidelity, Interactive Brokers)
- **BANK**: Banking connections (Plaid, Open Banking)
- **DATA_PROVIDER**: Market data providers (Alpha Vantage, IEX Cloud)

#### Validation
- Provider name and type are required (`@NotBlank`)
- Status must be one of the defined values
- Account type must be REAL or PAPER

#### Usage
```java
ConnectedProviderEntity provider = new ConnectedProviderEntity();
provider.setProviderName("kraken");
provider.setProviderType("EXCHANGE");
provider.setAccountType("REAL");
provider.setDisplayName("Kraken Main Account");
provider.setStatus("ACTIVE");
provider.setConnectedAt(Instant.now());
```

## Repository Interface

### ConnectedProviderRepository
**File**: `repository/ConnectedProviderRepository.java`
**Purpose**: Data access contract for provider connection operations

#### Core CRUD Operations
```java
// Connect provider to user
ConnectedProviderEntity connectProvider(String userId, ConnectedProviderEntity provider);

// Get all connected providers
List<ConnectedProviderEntity> findByUserId(String userId);

// Get specific provider connection
Optional<ConnectedProviderEntity> findByUserIdAndProviderId(String userId, 
                                                            String providerId);

// Update provider connection
ConnectedProviderEntity updateProvider(String userId, String providerId, 
                                     ConnectedProviderEntity provider);

// Disconnect provider
void disconnectProvider(String userId, String providerId);
```

#### Search and Filter Operations
```java
// Find by provider name
Optional<ConnectedProviderEntity> findByUserIdAndProviderName(String userId, 
                                                              String providerName);

// Get providers by type
List<ConnectedProviderEntity> findByUserIdAndType(String userId, String providerType);

// Get providers by status
List<ConnectedProviderEntity> findByUserIdAndStatus(String userId, String status);

// Get trading providers (exchanges and brokers)
List<ConnectedProviderEntity> findTradingProviders(String userId);
```

#### Status Management
```java
// Update provider status
void updateStatus(String userId, String providerId, String status);

// Mark last sync time
void markLastSync(String userId, String providerId);

// Mark provider error
void markError(String userId, String providerId, String errorMessage);

// Clear error status
void clearError(String userId, String providerId);

// Get providers with errors
List<ConnectedProviderEntity> findProvidersWithErrors(String userId);
```

#### Configuration Management
```java
// Update provider configuration
void updateConfiguration(String userId, String providerId, 
                        Map<String, Object> configuration);

// Update single configuration value
void updateConfigValue(String userId, String providerId, 
                      String key, Object value);

// Get configuration value
Optional<Object> getConfigValue(String userId, String providerId, String key);
```

#### Utility Operations
```java
// Check if provider is connected
boolean hasProviderConnected(String userId, String providerName);

// Count connected providers
long countByUserId(String userId);

// Count by type
long countByUserIdAndType(String userId, String providerType);

// Get recently connected
List<ConnectedProviderEntity> findRecentlyConnected(String userId, int limit);

// Get providers needing sync
List<ConnectedProviderEntity> findProvidersNeedingSync(String userId, 
                                                       Duration syncInterval);
```

## Usage Examples

### Connecting a Provider
```java
@Autowired
private ConnectedProviderRepository providerRepository;

// Connect to Kraken exchange
ConnectedProviderEntity kraken = new ConnectedProviderEntity();
kraken.setProviderName("kraken");
kraken.setProviderType("EXCHANGE");
kraken.setAccountType("REAL");
kraken.setDisplayName("Kraken Main");
kraken.setStatus("PENDING");
kraken.setConnectedAt(Instant.now());

Map<String, Object> config = new HashMap<>();
config.put("apiTier", "PRO");
config.put("enableWebsocket", true);
kraken.setConfiguration(config);

ConnectedProviderEntity connected = 
    providerRepository.connectProvider("user123", kraken);
```

### Managing Provider Status
```java
// Mark provider as active after verification
providerRepository.updateStatus("user123", "providerId456", "ACTIVE");

// Record successful sync
providerRepository.markLastSync("user123", "providerId456");

// Handle provider error
providerRepository.markError("user123", "providerId456", 
                           "API rate limit exceeded");

// Get all providers with errors
List<ConnectedProviderEntity> errorProviders = 
    providerRepository.findProvidersWithErrors("user123");
```

### Filtering Providers
```java
// Get all active exchanges
List<ConnectedProviderEntity> activeExchanges = 
    providerRepository.findByUserIdAndType("user123", "EXCHANGE")
        .stream()
        .filter(p -> "ACTIVE".equals(p.getStatus()))
        .collect(Collectors.toList());

// Get all trading providers (exchanges + brokers)
List<ConnectedProviderEntity> tradingProviders = 
    providerRepository.findTradingProviders("user123");

// Check if user has Binance connected
boolean hasBinance = 
    providerRepository.hasProviderConnected("user123", "binance");
```

### Configuration Updates
```java
// Update entire configuration
Map<String, Object> newConfig = new HashMap<>();
newConfig.put("apiTier", "PREMIUM");
newConfig.put("enableWebsocket", true);
newConfig.put("orderTypes", Arrays.asList("MARKET", "LIMIT", "STOP"));
providerRepository.updateConfiguration("user123", "providerId456", newConfig);

// Update single configuration value
providerRepository.updateConfigValue("user123", "providerId456", 
                                   "apiTier", "PREMIUM");
```

## Provider-Specific Configurations

### Exchange Configuration
```java
Map<String, Object> exchangeConfig = new HashMap<>();
exchangeConfig.put("apiTier", "PRO");
exchangeConfig.put("enableWebsocket", true);
exchangeConfig.put("supportedPairs", Arrays.asList("BTC/USD", "ETH/USD"));
exchangeConfig.put("feeSchedule", "MAKER_TAKER");
exchangeConfig.put("withdrawalEnabled", false);
```

### Broker Configuration
```java
Map<String, Object> brokerConfig = new HashMap<>();
brokerConfig.put("accountNumber", "ENCRYPTED_ACCOUNT_ID");
brokerConfig.put("accountClass", "MARGIN");
brokerConfig.put("tradingPermissions", Arrays.asList("STOCKS", "OPTIONS"));
brokerConfig.put("marketDataSubscription", "REAL_TIME");
```

### Data Provider Configuration
```java
Map<String, Object> dataConfig = new HashMap<>();
dataConfig.put("subscription", "PREMIUM");
dataConfig.put("dataTypes", Arrays.asList("QUOTES", "FUNDAMENTALS", "NEWS"));
dataConfig.put("rateLimit", 500);
dataConfig.put("webhookUrl", "https://api.strategiz.io/webhooks/data");
```

## Implementation Notes

### Firebase Firestore Structure
- Stored as subcollection under `users/{userId}/providers`
- Each provider connection is a separate document
- Supports multiple connections to the same provider
- Configuration stored as nested map for flexibility

### Security Considerations
- API credentials are NOT stored in this module
- Credentials are managed by data-auth module
- Configuration may contain non-sensitive settings only
- Provider metadata should not include secrets

### Status Workflow
1. **PENDING**: Initial connection state
2. **ACTIVE**: Verified and operational
3. **INACTIVE**: Temporarily disabled by user
4. **ERROR**: Connection or sync issues
5. **DISCONNECTED**: Soft-deleted state

### Sync Management
- `lastSyncAt` tracks successful data synchronization
- Sync intervals vary by provider type
- Error states don't update sync timestamp
- Providers can be queried for sync scheduling

## Integration with Other Modules

### Dependencies
- **data-base**: For BaseEntity and common utilities
- **framework-common**: For shared constants

### Related Modules
- **data-auth**: Stores API credentials for providers
- **business-provider**: Business logic for provider management
- **client-*-oauth**: OAuth integration for providers

### Used By
- **service-provider**: REST APIs for provider management
- **business-trading**: For executing trades through providers
- **data-user**: For user data aggregation

## Testing Guidelines

### Unit Tests
Focus on testing:
- Provider connection lifecycle
- Status transition logic
- Configuration management
- Filter and search operations
- Sync scheduling queries

### Integration Tests
Verify with Firebase:
- Subcollection CRUD operations
- Complex queries with multiple filters
- Configuration map storage
- Transaction handling
- Real-time updates

## Future Enhancements

### Planned Features
- Provider health monitoring
- Automatic reconnection logic
- Provider-specific feature flags
- Connection sharing (family accounts)
- Provider performance metrics
- Rate limit tracking
- Cost/fee tracking per provider

### Provider Expansion
- Support for more cryptocurrency exchanges
- Traditional brokerage integrations
- Banking/payment providers
- Alternative data providers
- Social trading platforms

### Technical Improvements
- Provider connection pooling
- Webhook management
- Event streaming per provider
- Provider-specific data schemas
- Migration tools for provider changes