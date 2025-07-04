# Clean Exception Framework

This framework provides a simplified, properties-based approach to exception handling that follows clean architecture principles.

## Key Features

- **Centralized Error Messages**: All error messages defined in properties files per module
- **Module Prefixes**: Error codes start with module name (AUTH_, DASHBOARD_, PROVIDER_)  
- **4-Field Standard**: Every error returns exactly 4 fields: `code`, `message`, `developerMessage`, `moreInfo`
- **Message Formatting**: Support for parameter substitution using {0}, {1}, etc.
- **Internationalization Ready**: Built on Spring's MessageSource for future i18n support
- **Clean Code**: Just throw `new StrategizException("ERROR_CODE", param1, param2)`

## Usage

### 1. Define Error Messages in Properties

Create `/src/main/resources/error-messages.properties` in each service module:

```properties
# Authentication Module Error Messages
AUTH_INVALID_CREDENTIALS.message=The email or password you entered is incorrect
AUTH_INVALID_CREDENTIALS.developerMessage=Authentication failed for user: {0}
AUTH_INVALID_CREDENTIALS.moreInfo=https://docs.strategiz.io/errors/authentication

AUTH_ACCOUNT_LOCKED.message=Your account has been locked due to multiple failed login attempts
AUTH_ACCOUNT_LOCKED.developerMessage=Account locked for user: {0} - exceeded max attempts
AUTH_ACCOUNT_LOCKED.moreInfo=https://docs.strategiz.io/errors/authentication#account-locked
```

### 2. Configure Spring to Load Properties

Add to `application.properties`:

```properties
spring.messages.basename=error-messages
spring.messages.encoding=UTF-8
spring.messages.cache-duration=PT10S
```

### 3. Throw Exceptions

```java
// Simple usage
throw new StrategizException("AUTH_INVALID_CREDENTIALS", userEmail);

// Multiple parameters
throw new StrategizException("PROVIDER_CONNECTION_FAILED", providerName, errorDetails);

// No parameters
throw new StrategizException("DASHBOARD_MARKET_DATA_UNAVAILABLE");
```

### 4. Automatic HTTP Response

The GlobalExceptionHandler automatically converts to clean API responses:

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "The email or password you entered is incorrect",
  "developerMessage": "Authentication failed for user: user@example.com",
  "moreInfo": "https://docs.strategiz.io/errors/authentication"
}
```

## Error Code Naming Convention

```
<MODULE>_<ERROR_TYPE>_<SPECIFIC_ERROR>

Examples:
- AUTH_INVALID_CREDENTIALS
- AUTH_ACCOUNT_LOCKED
- AUTH_SESSION_EXPIRED
- DASHBOARD_PORTFOLIO_NOT_FOUND
- PROVIDER_CONNECTION_FAILED
- PROVIDER_API_RATE_LIMITED
```

## HTTP Status Mapping

The framework automatically maps error codes to HTTP Status:

- `AUTH_INVALID_CREDENTIALS`, `AUTH_SESSION_EXPIRED` → 401 Unauthorized
- `AUTH_ACCOUNT_LOCKED` → 403 Forbidden  
- `*_NOT_FOUND` → 404 Not Found
- `*_VALIDATION_*`, `*_INVALID_*` → 400 Bad Request
- Default → 500 Internal Server Error

## Module Examples

### Auth Module (`service-auth`)
- `AUTH_INVALID_CREDENTIALS`
- `AUTH_ACCOUNT_LOCKED`
- `AUTH_MFA_REQUIRED`
- `AUTH_SESSION_EXPIRED`
- `AUTH_TOTP_INVALID_CODE`

### Dashboard Module (`service-dashboard`)
- `DASHBOARD_PORTFOLIO_NOT_FOUND`
- `DASHBOARD_PORTFOLIO_ACCESS_DENIED`
- `DASHBOARD_MARKET_DATA_UNAVAILABLE`
- `DASHBOARD_METRICS_CALCULATION_FAILED`

### Provider Module (`service-provider`)
- `PROVIDER_CONNECTION_FAILED`
- `PROVIDER_API_RATE_LIMITED`
- `PROVIDER_INVALID_CREDENTIALS`
- `PROVIDER_DATA_UNAVAILABLE`

## Benefits

1. **Consistency**: All errors follow the same 4-field structure
2. **Maintainability**: Error messages centralized in properties files
3. **Internationalization**: Easy to add multiple language support
4. **Clean Code**: Minimal exception throwing code
5. **Standardization**: Module prefixes prevent code conflicts
6. **Documentation**: Each error links to specific documentation

## Migration from Old System

Replace old exception constructors:

```java
// OLD - Complex constructor with all fields
throw new AuthenticationException(
    "INVALID_CREDENTIALS",
    "The email or password you entered is incorrect", 
    "Authentication failed for user: " + email,
    "https://docs.strategiz.io/errors/authentication"
);

// NEW - Simple with properties lookup
throw new StrategizException("AUTH_INVALID_CREDENTIALS", email);
``` 