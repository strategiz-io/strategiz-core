# Provider Service Architecture

## Overview

The Provider Service handles step 3 of the signup process - connecting users to their financial providers (exchanges, brokerages, etc.) through a **sequential, user-controlled flow**.

## Architecture Principles

### 🎯 **Core Design Decisions**

1. **Sequential Flow**: One provider at a time, not parallel
2. **User Control**: Users can skip/cancel at any point
3. **Business Module Separation**: Provider-specific logic in separate business modules
4. **Single Controller**: One orchestration endpoint for all providers
5. **Stateful Service**: Tracks user progress through provider sequence

### 🏗️ **Architecture Components**

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Step 3)                       │
├─────────────────────────────────────────────────────────────┤
│                SignupProviderController                     │
│                  (Single Entry Point)                      │
├─────────────────────────────────────────────────────────────┤
│                SignupProviderService                        │
│              (Sequential Flow Manager)                      │
├─────────────────────────────────────────────────────────────┤
│  Business Modules (Provider-Specific Logic)                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Coinbase    │ │ Kraken      │ │ Binance     │          │
│  │ Business    │ │ Business    │ │ Business    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                     Database                                │
│              (Provider Sequence State)                      │
└─────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. SignupProviderController

**Responsibility**: Single orchestration endpoint for all provider interactions

```java
@RestController
@RequestMapping("/api/signup/provider")
public class SignupProviderController {
    
    // Initialize provider sequence
    POST /connect
    
    // Handle provider callbacks (OAuth, API keys, etc.)
    POST /callback/{providerId}
    
    // Skip current provider
    POST /skip
    
    // Get current provider status
    GET /status
    
    // Complete signup (exit flow)
    POST /complete
}
```

### 2. SignupProviderService

**Responsibility**: Sequential flow management and routing to business modules

```java
@Service
public class SignupProviderService {
    
    // Core flow methods
    public ProviderSequenceResponse initiateProviderSequence(userId, selectedProviders);
    public NextProviderResponse getNextProvider(sequenceId);
    public ProviderCallbackResponse handleProviderCallback(providerId, callbackData);
    public SkipProviderResponse skipCurrentProvider(sequenceId);
    public SignupCompleteResponse completeSignup(sequenceId);
    
    // Business module routing
    private ProviderBusiness getProviderBusiness(providerId);
}
```

### 3. Business Modules

**Responsibility**: Provider-specific logic (OAuth, API keys, validation, etc.)

Each provider has its own business module with standardized interface:

```java
// Common interface
public interface ProviderSignupBusiness {
    String getProviderId();
    ProviderConnectionInfo getConnectionInfo(String userId);
    boolean handleConnection(String userId, Map<String, String> connectionData);
    boolean validateConnection(String userId);
    void disconnect(String userId);
}

// Provider-specific implementations
@Component
public class CoinbaseSignupBusiness implements ProviderSignupBusiness {
    // OAuth 2.0 flow implementation
}

@Component  
public class KrakenSignupBusiness implements ProviderSignupBusiness {
    // API key flow implementation
}
```

## Sequential Flow Design

### User Experience Flow

```
User selects: [Coinbase, Kraken, Binance]
    ↓
Start Coinbase OAuth
    ↓
[Complete] ──→ Next: Kraken API keys
    ↓              ↓
[Skip] ────────→ [Complete] ──→ Next: Binance OAuth
    ↓              ↓              ↓
Next: Kraken ───→ [Skip] ────→ [Complete] ──→ Dashboard
    ↓              ↓              ↓
Kraken API ────→ Next: Binance  [Skip] ────→ Dashboard
    ↓              ↓              ↓
...            Binance OAuth   Dashboard
```

### State Management

The service maintains a `ProviderSequence` state object:

```java
public class ProviderSequence {
    String sequenceId;
    String userId;
    List<String> selectedProviders;
    int currentProviderIndex;
    Map<String, ProviderConnectionStatus> connectionStatuses;
    Date createdAt;
    Date expiresAt;
    SequenceStatus status; // ACTIVE, PAUSED, COMPLETED, EXPIRED
}

public class ProviderConnectionStatus {
    String providerId;
    ConnectionState state; // PENDING, CONNECTING, CONNECTED, SKIPPED, FAILED
    Date attemptedAt;
    Date completedAt;
    String connectionData; // OAuth tokens, API keys, etc.
}
```

## API Design

### 1. Initialize Provider Sequence

```http
POST /api/signup/provider/connect
Content-Type: application/json

{
  "selectedProviders": ["coinbase", "kraken", "binance"],
  "userPreferences": {
    "allowSkip": true,
    "requiredCount": 1
  }
}
```

**Response:**
```json
{
  "sequenceId": "seq_abc123",
  "currentProvider": {
    "providerId": "coinbase",
    "connectionType": "oauth",
    "connectionUrl": "https://coinbase.com/oauth/authorize?...",
    "instructions": "You'll be redirected to Coinbase to connect your account"
  },
  "progress": {
    "current": 1,
    "total": 3,
    "canSkip": true
  }
}
```

### 2. Handle Provider Callback

```http
POST /api/signup/provider/callback/coinbase
Content-Type: application/json

{
  "sequenceId": "seq_abc123",
  "code": "oauth_code_from_coinbase",
  "state": "state_parameter"
}
```

**Response:**
```json
{
  "connectionStatus": "success",
  "nextProvider": {
    "providerId": "kraken",
    "connectionType": "api_key",
    "instructions": "Enter your Kraken API keys",
    "form": {
      "fields": ["api_key", "secret_key"]
    }
  },
  "progress": {
    "current": 2,
    "total": 3,
    "canSkip": true
  }
}
```

### 3. Skip Provider

```http
POST /api/signup/provider/skip
Content-Type: application/json

{
  "sequenceId": "seq_abc123",
  "reason": "user_choice"
}
```

### 4. Get Current Status

```http
GET /api/signup/provider/status/{sequenceId}
```

**Response:**
```json
{
  "sequenceId": "seq_abc123",
  "currentProvider": "kraken",
  "progress": {
    "current": 2,
    "total": 3,
    "completed": ["coinbase"],
    "pending": ["kraken", "binance"],
    "skipped": []
  },
  "canComplete": true
}
```

### 5. Complete Signup

```http
POST /api/signup/provider/complete
Content-Type: application/json

{
  "sequenceId": "seq_abc123"
}
```

## Provider Business Modules

### Directory Structure

```
business/
├── business-coinbase/
│   ├── src/main/java/io/strategiz/business/coinbase/
│   │   ├── CoinbaseSignupBusiness.java
│   │   ├── CoinbaseOAuthClient.java
│   │   └── model/
│   │       ├── CoinbaseAccount.java
│   │       └── CoinbasePortfolio.java
│   └── pom.xml
├── business-kraken/
│   ├── src/main/java/io/strategiz/business/kraken/
│   │   ├── KrakenSignupBusiness.java
│   │   ├── KrakenApiClient.java
│   │   └── model/
│   │       ├── KrakenBalance.java
│   │       └── KrakenCredentials.java
│   └── pom.xml
└── business-binance/
    ├── src/main/java/io/strategiz/business/binance/
    │   ├── BinanceSignupBusiness.java
    │   ├── BinanceOAuthClient.java
    │   └── model/
    └── pom.xml
```

### Business Module Interface

```java
public interface ProviderSignupBusiness {
    
    // Provider identification
    String getProviderId();
    String getProviderName();
    
    // Connection flow
    ProviderConnectionInfo getConnectionInfo(String userId);
    boolean handleConnection(String userId, Map<String, String> connectionData);
    
    // Validation and testing
    boolean validateConnection(String userId);
    boolean testConnection(String userId);
    
    // Management
    void disconnect(String userId);
    ProviderConnectionStatus getConnectionStatus(String userId);
    
    // Provider-specific data
    Map<String, Object> getProviderMetadata();
}
```

### Example: Coinbase Business Module

```java
@Component
public class CoinbaseSignupBusiness implements ProviderSignupBusiness {
    
    @Override
    public String getProviderId() {
        return "coinbase";
    }
    
    @Override
    public ProviderConnectionInfo getConnectionInfo(String userId) {
        return ProviderConnectionInfo.builder()
            .providerId("coinbase")
            .connectionType("oauth")
            .connectionUrl(buildOAuthUrl(userId))
            .instructions("Connect your Coinbase account to import portfolio data")
            .build();
    }
    
    @Override
    public boolean handleConnection(String userId, Map<String, String> connectionData) {
        // Handle OAuth callback
        String code = connectionData.get("code");
        String state = connectionData.get("state");
        
        // Exchange code for tokens
        CoinbaseTokenResponse tokens = coinbaseOAuthClient.exchangeCodeForTokens(code);
        
        // Store tokens securely
        credentialsService.storeCredentials(userId, "coinbase", tokens);
        
        return true;
    }
    
    // ... other methods
}
```

## Database Design

### Tables/Collections

#### provider_sequences
```sql
CREATE TABLE provider_sequences (
    sequence_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    selected_providers JSON NOT NULL,
    current_provider_index INT DEFAULT 0,
    status ENUM('ACTIVE', 'PAUSED', 'COMPLETED', 'EXPIRED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    completed_at TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);
```

#### provider_connections
```sql
CREATE TABLE provider_connections (
    connection_id VARCHAR(255) PRIMARY KEY,
    sequence_id VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    connection_state ENUM('PENDING', 'CONNECTING', 'CONNECTED', 'SKIPPED', 'FAILED') DEFAULT 'PENDING',
    connection_data JSON,
    attempted_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    FOREIGN KEY (sequence_id) REFERENCES provider_sequences(sequence_id),
    INDEX idx_sequence_id (sequence_id),
    INDEX idx_user_provider (user_id, provider_id)
);
```

## Error Handling

### Error Types

1. **Provider Connection Errors**
   - OAuth authorization denied
   - Invalid API keys
   - Provider service unavailable

2. **Flow State Errors**
   - Expired sequence
   - Invalid sequence ID
   - Concurrent access issues

3. **Validation Errors**
   - Missing required parameters
   - Invalid provider ID
   - Malformed connection data

### Error Response Format

```json
{
  "error": {
    "code": "PROVIDER_CONNECTION_FAILED",
    "message": "Failed to connect to Coinbase",
    "details": {
      "providerId": "coinbase",
      "reason": "oauth_authorization_denied",
      "canRetry": true,
      "canSkip": true
    }
  },
  "nextAction": {
    "type": "retry_or_skip",
    "options": ["retry", "skip", "abort"]
  }
}
```

## Security Considerations

1. **State Parameter Validation**: All OAuth flows include state parameters for CSRF protection
2. **Token Storage**: Provider credentials stored using vault/encryption
3. **Session Expiration**: Provider sequences expire after 30 minutes
4. **Rate Limiting**: Prevent abuse of provider connection attempts
5. **Audit Logging**: Log all provider connection attempts and outcomes

## Testing Strategy

### Unit Tests
- Individual business modules
- Sequential flow logic
- Error handling scenarios

### Integration Tests
- End-to-end provider flows
- Database state management
- Provider callback handling

### UI Tests
- Sequential flow user experience
- Skip/cancel functionality
- Error state handling

## Deployment Considerations

### Environment Variables
```env
# Coinbase OAuth
COINBASE_CLIENT_ID=your_coinbase_client_id
COINBASE_CLIENT_SECRET=your_coinbase_client_secret

# Kraken API (for validation)
KRAKEN_API_BASE_URL=https://api.kraken.com

# Flow configuration
PROVIDER_SEQUENCE_TIMEOUT_MINUTES=30
PROVIDER_CONNECTION_RETRY_LIMIT=3
```

### Health Checks
- Provider business module availability
- Database connection health
- External provider API availability

## Future Enhancements

1. **Parallel Provider Support**: Option for advanced users to connect multiple providers simultaneously
2. **Provider Recommendations**: Suggest providers based on user profile
3. **Connection Testing**: Automated testing of provider connections
4. **Provider Analytics**: Track success rates and completion times
5. **Mobile App Support**: Optimize OAuth flows for mobile devices

## Monitoring and Metrics

### Key Metrics
- Provider connection success rates
- Average time to complete sequence
- Most skipped providers
- Drop-off points in the flow

### Alerts
- High provider connection failure rates
- Expired sequences above threshold
- External provider API downtime

---

## Implementation Checklist

- [ ] Create business module structure
- [ ] Implement SignupProviderService
- [ ] Update SignupProviderController
- [ ] Create database schema
- [ ] Implement Coinbase business module
- [ ] Implement Kraken business module
- [ ] Add error handling
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update frontend integration
- [ ] Deploy and monitor 