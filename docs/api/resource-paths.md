# Strategiz Backend API Resource Paths

## Base URL
- Development: `http://localhost:8080`
- Production: `https://strategiz-core-43628135674.us-central1.run.app`

## API Version
All API endpoints use versioning with `/v1` prefix except health endpoints.

## Authentication Service (`/v1/auth`)

### TOTP (Time-based One-Time Password)
```
POST   /v1/auth/totp/registrations              # Initialize TOTP setup
PUT    /v1/auth/totp/registrations/{id}         # Complete TOTP registration
GET    /v1/auth/totp/registrations/{userId}     # Get TOTP registration status
DELETE /v1/auth/totp/registrations/{userId}     # Remove TOTP authentication
POST   /v1/auth/totp/authentications            # Authenticate with TOTP
POST   /v1/auth/totp/authentications/verify     # Verify TOTP code
```

### SMS OTP
```
POST   /v1/auth/sms-otp/registrations           # Register phone number
PUT    /v1/auth/sms-otp/registrations/{id}      # Verify phone registration
POST   /v1/auth/sms-otp/registrations/{id}/resend # Resend SMS code
GET    /v1/auth/sms-otp/registrations/{userId}  # Get SMS registration status
DELETE /v1/auth/sms-otp/registrations/{userId}  # Remove SMS authentication
POST   /v1/auth/sms-otp/authentications         # Request SMS authentication (with userId)
POST   /v1/auth/sms-otp/messages                # Request SMS authentication (phone only)
PUT    /v1/auth/sms-otp/authentications/{id}    # Verify SMS code and authenticate
```

### Passkeys (WebAuthn)
```
POST   /v1/auth/passkeys/registrations          # Start passkey registration
PUT    /v1/auth/passkeys/registrations/{id}     # Complete passkey registration
GET    /v1/auth/passkeys/registrations/options  # Get registration options
POST   /v1/auth/passkeys/authentications        # Start passkey authentication
PUT    /v1/auth/passkeys/authentications/{id}   # Complete passkey authentication
GET    /v1/auth/passkeys                        # List user's passkeys
DELETE /v1/auth/passkeys/{credentialId}         # Remove a passkey
GET    /v1/auth/passkeys/stats                  # Get passkey statistics
```

### OAuth Providers
```
# Generic OAuth endpoints (replace {provider} with google/facebook)
GET    /v1/auth/oauth/{provider}/signup/authorization-url  # Get OAuth signup URL
GET    /v1/auth/oauth/{provider}/signin/authorization-url  # Get OAuth signin URL
GET    /v1/auth/oauth/{provider}/signup/auth              # OAuth signup redirect
GET    /v1/auth/oauth/{provider}/signin/auth              # OAuth signin redirect
POST   /v1/auth/oauth/{provider}/signup/callback          # OAuth signup callback
POST   /v1/auth/oauth/{provider}/signin/callback          # OAuth signin callback
GET    /v1/auth/oauth/{provider}/signup/callback          # OAuth signup callback (GET)
GET    /v1/auth/oauth/{provider}/signin/callback          # OAuth signin callback (GET)
```

### Session Management
```
POST   /v1/auth/session/refresh                 # Refresh access token
POST   /v1/auth/session/validate                # Validate session token
POST   /v1/auth/session/revoke                  # Revoke current session
POST   /v1/auth/session/revoke-all/{userId}     # Revoke all user sessions
POST   /v1/auth/session/current-user            # Get current user from session
POST   /v1/auth/session/user-sessions           # Get all user sessions
POST   /v1/auth/signout                         # Sign out user
```

## Profile Service (`/v1/profile`)
```
POST   /v1/profile                              # Create user profile
GET    /v1/profile/me                           # Get current user profile
GET    /v1/profile/{userId}                     # Get user profile by ID
PUT    /v1/profile/me                           # Update current user profile
POST   /v1/profile/verify-email                 # Verify email address
DELETE /v1/profile/me                           # Delete current user profile
```

## Signup Service (`/v1/signup`)
```
POST   /v1/signup/profile                       # Create profile during signup
```

## Provider Service (`/v1/providers`)
```
POST   /v1/providers                            # Connect new provider
GET    /v1/providers                            # List connected providers
GET    /v1/providers/{providerId}               # Get provider details
PUT    /v1/providers/{providerId}               # Update provider connection
DELETE /v1/providers/{providerId}               # Disconnect provider
GET    /v1/providers/{providerId}/status        # Get provider connection status
GET    /v1/providers/{providerId}/data          # Get provider data
GET    /v1/providers/callback/{provider}        # OAuth callback endpoint
POST   /v1/providers/callback/{provider}        # OAuth callback (POST)
```

## Strategy Service (`/v1/strategies`)
```
POST   /v1/strategies                           # Create new strategy
GET    /v1/strategies                           # List user strategies
GET    /v1/strategies/public                    # List public strategies
GET    /v1/strategies/{strategyId}              # Get strategy details
PUT    /v1/strategies/{strategyId}              # Update strategy
PATCH  /v1/strategies/{strategyId}/status       # Update strategy status
DELETE /v1/strategies/{strategyId}              # Delete strategy
POST   /v1/strategies/{strategyId}/execute      # Execute strategy
POST   /v1/strategies/execute-code              # Execute strategy code
GET    /v1/strategies/providers                 # Get available providers
GET    /v1/strategies/providers/{id}/symbols    # Get provider symbols
```

## Dashboard Service (`/v1/dashboard`)
```
# Watchlist
GET    /v1/dashboard/watchlist                  # Get watchlist items
POST   /v1/dashboard/watchlist                  # Add to watchlist
DELETE /v1/dashboard/watchlist/{itemId}         # Remove from watchlist
GET    /v1/dashboard/watchlist/check/{symbol}   # Check if symbol in watchlist

# Portfolio
GET    /v1/dashboard/portfolio/summary          # Get portfolio summary

# Market Analysis
GET    /v1/dashboard/market/sentiment           # Get market sentiment
GET    /v1/dashboard/market/trends              # Get market trends

# Analytics
GET    /v1/dashboard/allocation                 # Get asset allocation
GET    /v1/dashboard/risk                       # Get risk analysis
GET    /v1/dashboard/metrics                    # Get performance metrics
```

## Market Data Service (`/v1/market`)
```
GET    /v1/market/tickers                       # Get market tickers
```

## Market Data Batch Service (`/v1/market-data`)
```
GET    /v1/market-data/batch/status             # Get batch job status
POST   /v1/market-data/batch/trigger            # Trigger batch data fetch
```

## Portfolio Service (`/v1/portfolios`)
```
# Provider-specific portfolio data
GET    /v1/portfolios/providers/coinbase/connection     # Get Coinbase connection status
GET    /v1/portfolios/providers/coinbase/accounts      # Get all Coinbase accounts with balances
GET    /v1/portfolios/providers/coinbase/holdings      # Get holdings with profit/loss
GET    /v1/portfolios/providers/coinbase/transactions  # Get recent transactions
GET    /v1/portfolios/providers/coinbase/prices        # Get current crypto prices

# Legacy endpoint (to be deprecated)
GET    /v1/portfolio/{provider}/data                   # Get portfolio data from provider
```

## Device Service (`/v1/device`)
```
# Anonymous Devices
POST   /v1/device/anonymous/register            # Register anonymous device

# Authenticated Devices  
POST   /v1/device/authenticated/register        # Register authenticated device
GET    /v1/device/authenticated/list            # List user devices
DELETE /v1/device/authenticated/{deviceId}      # Remove device
PUT    /v1/device/authenticated/{deviceId}/trusted/{trusted} # Update device trust
```

## Marketplace Service (`/v1/marketplace`)
```
# Endpoints not yet implemented
```

## Health & Monitoring (`/api`)
```
GET    /api/health                              # Basic health check
GET    /api/system/health                       # Detailed system health
GET    /api/version                             # API version info
```

## Documentation (`/v1/docs`)
```
GET    /v1/docs/architecture                    # Architecture documentation
```

## REST API Guidelines Applied

### Resource Naming
- ✅ Use plural nouns for collections (`/strategies`, `/providers`)
- ✅ Use hierarchical structure for relationships (`/auth/passkeys/registrations`)
- ✅ Use hyphens for multi-word resources (`/sms-otp`, `/market-data`)

### HTTP Methods
- ✅ GET for reading resources
- ✅ POST for creating new resources
- ✅ PUT for full updates
- ✅ PATCH for partial updates
- ✅ DELETE for removing resources

### Status Codes (Standard Implementation)
- 200 OK - Successful GET, PUT
- 201 Created - Successful POST
- 204 No Content - Successful DELETE
- 400 Bad Request - Invalid request
- 401 Unauthorized - Authentication required
- 403 Forbidden - Access denied
- 404 Not Found - Resource not found
- 500 Internal Server Error - Server error

### Versioning
- ✅ URL versioning with `/v1` prefix
- ✅ Consistent across all services

### Authentication
- Bearer token authentication via Authorization header
- Session tokens for web clients
- API keys for service-to-service

### Response Format
All responses follow a consistent JSON structure:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "timestamp": "2025-08-10T12:00:00Z"
}
```

### Error Response Format
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": { ... }
  },
  "timestamp": "2025-08-10T12:00:00Z"
}
```

## Issues to Address

### 1. SMS OTP Endpoint Inconsistency
- **Issue**: Endpoint `/v1/auth/sms-otp/messages` doesn't follow RESTful conventions
- **Fix**: Should be `/v1/auth/sms-otp/authentications/phone` or similar

### 2. Session Endpoints Naming
- **Issue**: Mix of noun and verb naming (`/current-user`, `/logout-server`)
- **Fix**: Should use consistent resource-based naming

### 3. Strategy Execution
- **Issue**: `/execute-code` is not RESTful
- **Fix**: Should be `/v1/strategies/executions` with POST

### 4. Provider Callback
- **Issue**: Callback endpoints mixed with regular CRUD operations
- **Fix**: Consider separate `/v1/callbacks/providers/{provider}` namespace

### 5. Passkey Debug Endpoints
- **Issue**: Debug endpoints in production code
- **Fix**: Should be conditionally exposed only in development

## Recommendations

1. **Standardize authentication endpoints**: All auth methods should follow similar patterns
2. **Implement HATEOAS**: Include links to related resources in responses
3. **Add pagination**: For list endpoints (`/strategies`, `/providers`, etc.)
4. **Add filtering**: Query parameters for filtering collections
5. **Add field selection**: Allow clients to specify which fields to return
6. **Rate limiting**: Implement rate limiting headers
7. **API documentation**: Generate OpenAPI/Swagger documentation
8. **Consistent error codes**: Define standard error code catalog