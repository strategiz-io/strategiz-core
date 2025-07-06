# Strategiz Core - Naming Convention Standards

## Overview
This document establishes consistent naming conventions for request/response models, client models, and data models across all layers of the Strategiz Core application using CRUD operation prefixes.

## Core Principles

### 1. CRUD Operation Prefixes
All models should start with one of these operation prefixes:
- **Create** - For creating new resources
- **Read** - For retrieving/querying existing resources  
- **Update** - For modifying existing resources
- **Delete** - For removing existing resources

### 2. Layer-Specific Patterns
Each application layer follows the same CRUD prefix pattern but serves different purposes:
- **Service Layer**: API request/response models
- **Client Layer**: External API integration models
- **Data Layer**: Database/repository operation models

## Naming Patterns

### Service Layer (API Endpoints)
```java
// Pattern: {Operation}{Entity}{Request/Response}
CreateUserRequest           // POST /users
CreateUserResponse          
ReadUserRequest             // GET /users/{id}
ReadUserResponse            
UpdateUserRequest           // PUT /users/{id}
UpdateUserResponse          
DeleteUserRequest           // DELETE /users/{id}
DeleteUserResponse          

// Examples
CreatePortfolioRequest
ReadPortfolioSummaryResponse
UpdateStrategyRequest
DeleteSessionResponse
```

### Client Layer (External API Integration)
```java
// Pattern: {Operation}{Provider}{Entity}{Request/Response}
ReadCoinbaseBalanceRequest      // Coinbase API balance call
ReadCoinbaseBalanceResponse     
CreateBinanceOrderRequest       // Binance API order creation
CreateBinanceOrderResponse      
ReadAlphaVantageQuoteRequest    // Alpha Vantage quote request
ReadAlphaVantageQuoteResponse   

// Examples
ReadKrakenAccountRequest
CreateCoinbaseProOrderRequest
ReadBinanceUSTickerResponse
UpdateFacebookAuthRequest
```

### Data Layer (Database Operations)
```java
// Pattern: {Operation}{Entity}Data{Request/Response}
CreateUserDataRequest           // Database user creation
CreateUserDataResponse          
ReadPortfolioDataRequest        // Database portfolio query
ReadPortfolioDataResponse       
UpdateStrategyDataRequest       // Database strategy update
UpdateStrategyDataResponse      

// Examples
CreateDeviceDataRequest
ReadExchangeDataResponse
UpdateAuthDataRequest
DeleteSessionDataResponse
```

## Special Cases

### Non-CRUD Operations
For operations that don't fit standard CRUD patterns:
```java
// Authentication & Authorization
AuthenticateUserRequest
AuthenticateUserResponse
ValidateSessionRequest
ValidateSessionResponse
RefreshTokenRequest
RefreshTokenResponse

// Calculations & Analytics
CalculateMetricsRequest
CalculateMetricsResponse
AnalyzePortfolioRequest
AnalyzePortfolioResponse

// Search & Query
SearchStrategiesRequest
SearchStrategiesResponse
QueryMarketDataRequest
QueryMarketDataResponse

// Batch Operations
CreateMultipleOrdersRequest
CreateMultipleOrdersResponse
ReadBulkPortfoliosRequest
ReadBulkPortfoliosResponse
```

### Complex Entity Names
For multi-word entities, use PascalCase:
```java
CreateUserProfileRequest
ReadMarketSentimentResponse
UpdateTradingStrategyRequest
DeleteWalletAddressResponse
```

## File Organization

### Directory Structure
```
service/
├── service-auth/src/main/java/io/strategiz/service/auth/model/
│   ├── CreateUserRequest.java
│   ├── CreateUserResponse.java
│   ├── ReadUserRequest.java
│   └── ReadUserResponse.java
├── service-portfolio/src/main/java/io/strategiz/service/portfolio/model/
│   ├── CreatePortfolioRequest.java
│   └── ReadPortfolioSummaryResponse.java

client/
├── client-coinbase/src/main/java/io/strategiz/client/coinbase/model/
│   ├── ReadBalanceRequest.java
│   ├── ReadBalanceResponse.java
│   ├── CreateOrderRequest.java
│   └── CreateOrderResponse.java

data/
├── data-user/src/main/java/io/strategiz/data/user/model/
│   ├── CreateUserDataRequest.java
│   ├── CreateUserDataResponse.java
│   ├── ReadUserDataRequest.java
│   └── ReadUserDataResponse.java
```

## Migration Strategy

### Phase 1: New Development (Immediate)
- All new models MUST follow CRUD naming conventions
- Apply to new features and endpoints being developed
- Use in new client integrations

### Phase 2: Client Layer Migration (Priority)
- Most isolated layer, easiest to migrate
- Start with new exchange integrations
- Gradually update existing client models

### Phase 3: Service Layer Migration (Gradual)
- Update models during API versioning
- Maintain backward compatibility during transition
- Use deprecation warnings for old models

### Phase 4: Data Layer Migration (Careful)
- Coordinate with database schema changes
- Update repository interfaces gradually
- Ensure no breaking changes to existing data access

## Benefits

### Developer Experience
- **Instant Clarity**: Know the operation and layer at a glance
- **Consistent Organization**: Models group logically in IDEs
- **Easy Navigation**: Predictable naming makes code search effortless
- **Reduced Cognitive Load**: No guessing about model purpose

### Maintenance & Scaling
- **Clear API Documentation**: Self-documenting endpoint purposes
- **Easier Code Reviews**: Consistent patterns reduce review overhead
- **Better Tooling Support**: Code generators can follow consistent patterns
- **Onboarding**: New developers can quickly understand the codebase

### Architecture Benefits
- **Layer Separation**: Clear distinction between service/client/data concerns
- **Migration Tracking**: Easy to identify legacy vs. new pattern models
- **Consistency**: Same patterns across all application layers
- **Scalability**: Pattern scales with application growth

## Examples by Domain

### Authentication Domain
```java
// Service Layer
CreateUserRequest/CreateUserResponse
ReadUserSessionRequest/ReadUserSessionResponse
UpdatePasswordRequest/UpdatePasswordResponse
DeleteSessionRequest/DeleteSessionResponse

// Client Layer (OAuth providers)
ReadGoogleProfileRequest/ReadGoogleProfileResponse
CreateFacebookAuthRequest/CreateFacebookAuthResponse

// Data Layer
CreateUserDataRequest/CreateUserDataResponse
ReadAuthTokenDataRequest/ReadAuthTokenDataResponse
```

### Portfolio Domain
```java
// Service Layer
CreatePortfolioRequest/CreatePortfolioResponse
ReadPortfolioSummaryRequest/ReadPortfolioSummaryResponse
UpdateAssetAllocationRequest/UpdateAssetAllocationResponse

// Client Layer (Exchanges)
ReadCoinbaseBalanceRequest/ReadCoinbaseBalanceResponse
CreateBinanceOrderRequest/CreateBinanceOrderResponse
ReadKrakenTickerRequest/ReadKrakenTickerResponse

// Data Layer
CreatePortfolioDataRequest/CreatePortfolioDataResponse
ReadHoldingDataRequest/ReadHoldingDataResponse
```

## Enforcement

### Code Review Checklist
- [ ] Model names follow CRUD prefix pattern
- [ ] Layer-appropriate naming (Service/Client/Data)
- [ ] Consistent with domain conventions
- [ ] File placed in correct directory structure

### IDE Configuration
- Configure code templates to generate CRUD-prefixed models
- Set up live templates for common patterns
- Create inspection rules to flag non-compliant naming

### Documentation
- Update API documentation generators to expect CRUD patterns
- Include naming convention in developer onboarding
- Reference this document in code review guidelines

---

## Revision History
- **v1.0** - Initial naming convention standards
- Future revisions will be tracked here

This document is a living standard and should be updated as the application evolves. 