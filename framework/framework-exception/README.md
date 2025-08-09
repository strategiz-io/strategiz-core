# Layered Exception Framework

This framework provides a layered, properties-based approach to exception handling that follows clean architecture principles.

## Key Features

- **Layered Architecture**: HTTP concerns only in service modules, clean separation in business/data layers
- **Centralized Error Messages**: All error messages defined in properties files per module
- **Type Safety**: ErrorDetails interface for service modules, simple enums for other layers
- **4-Field Standard**: Every error returns exactly 4 fields: `code`, `message`, `developer`, `moreInfo`
- **Message Formatting**: Support for parameter substitution using {0}, {1}, etc.
- **Internationalization Ready**: Built on Spring's MessageSource for future i18n support
- **Simple HTTP Status**: 200/201 for success, 400 for client errors, 401 for auth, 500 for server errors

## Naming Convention

### ErrorDetails Classes
- **Service Modules**: `Service{ModuleName}ErrorDetails` (e.g., `ServiceAuthErrorDetails`, `ServiceProviderErrorDetails`)
- **Other Modules**: Simple `{ModuleName}Errors` (e.g., `CoinbaseErrors`, `AuthBusinessErrors`)

### Properties Files  
- **All Modules**: `service-{module-name}-errors.properties` (e.g., `service-auth-errors.properties`, `service-provider-errors.properties`)

## Architecture by Module Type

### Service Modules (HTTP Layer)
**Use ErrorDetails interface with HTTP status codes**

Service modules handle REST endpoints and need HTTP status mapping:

```java
// service-auth/exception/ServiceAuthErrorDetails.java
public enum ServiceAuthErrorDetails implements ErrorDetails {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "session-expired"),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "user-not-found");
    
    private final HttpStatus httpStatus;
    private final String propertyKey;
    
    ServiceAuthErrorDetails(HttpStatus httpStatus, String propertyKey) {
        this.httpStatus = httpStatus;
        this.propertyKey = propertyKey;
    }
    
    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
    
    @Override
    public String getPropertyKey() { return propertyKey; }
}

// Usage in service controllers:
throw new StrategizException(ServiceAuthErrorDetails.INVALID_CREDENTIALS, "service-auth", userEmail);
```

### Business/Data/Client/Framework Modules
**Use simple enums without HTTP concerns**

These modules should remain HTTP-agnostic:

```java
// business-auth/exception/AuthBusinessErrors.java
public enum AuthBusinessErrors {
    INVALID_CREDENTIALS,
    PASSWORD_ENCRYPTION_FAILED,
    USER_CREATION_FAILED
}

// data-auth/exception/AuthDataErrors.java  
public enum AuthDataErrors {
    USER_NOT_FOUND,
    DATABASE_CONNECTION_FAILED,
    QUERY_TIMEOUT
}

// client-coinbase/CoinbaseErrors.java
public enum CoinbaseErrors {
    API_CONNECTION_FAILED,
    INVALID_CREDENTIALS,
    RATE_LIMITED
}

// Usage in non-service modules:
throw new StrategizException(AuthBusinessErrors.PASSWORD_ENCRYPTION_FAILED, "business-auth", userId);
```

## Properties File Format

Each module defines error messages in `/src/main/resources/messages/service-{module-name}-errors.properties`:

```properties
# Format: [error-key].[property] = [value]
# Properties: code, message, developer, more-info
# Developer message placeholders: {0} = module name, {1..n} = custom parameters

invalid-credentials.code=AUTH_INVALID_CREDENTIALS
invalid-credentials.message=Invalid username or password
invalid-credentials.developer=Authentication failed in module: {0}, user: [MASKED_EMAIL], method: {1}
invalid-credentials.more-info=authentication/invalid-credentials

user-not-found.code=USER_NOT_FOUND
user-not-found.message=User account not found
user-not-found.developer=User lookup failed in module: {0}, user id: {1}
user-not-found.more-info=users/user-not-found
```

## HTTP Response Format

BaseController automatically converts to clean API responses:

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid username or password",
  "developer": "Authentication failed in module: service-auth, user: [MASKED_EMAIL], method: password",
  "moreInfo": "https://docs.strategiz.io/errors/authentication/invalid-credentials"
}
```

## HTTP Status Code Mapping

Service modules using ErrorDetails specify exact status codes. BaseController uses simple pattern matching for legacy/simple enums:

- **401 Unauthorized**: UNAUTHORIZED, AUTHENTICATION, CREDENTIALS, SESSION, TOKEN
- **400 Bad Request**: INVALID, VALIDATION, NOT_FOUND, ALREADY_EXISTS
- **500 Internal Server Error**: Everything else

## Simple Status Code Philosophy

We use minimal HTTP status codes:

- **200**: Success (GET, PUT, DELETE)
- **201**: Created (POST)
- **400**: Client provided bad input
- **401**: Authentication required/failed
- **500**: Server failed to process valid input

This covers 99% of real-world API needs without over-engineering.

## Current Implementation Status

### Service Modules with ErrorDetails ✅
- `service-auth`: ServiceAuthErrorDetails + service-auth-errors.properties
- `service-dashboard`: ServiceDashboardErrorDetails + service-dashboard-errors.properties  
- `service-marketing`: ServiceMarketingErrorDetails + service-marketing-errors.properties
- `service-provider`: ServiceProviderErrorDetails + service-provider-errors.properties
- `service-strategy`: ServiceStrategyErrorDetails + service-strategy-errors.properties
- `service-base`: ServiceBaseErrorDetails + service-base-errors.properties

### Service Modules Needing ErrorDetails ❌
These modules have properties files but need ErrorDetails enums:
- `service-marketplace`: Has service-marketplace-errors.properties (needs ServiceMarketplaceErrorDetails)
- `service-portfolio`: Has service-portfolio-errors.properties (needs ServicePortfolioErrorDetails)
- `service-profile`: Has service-profile-errors.properties (needs ServiceProfileErrorDetails)

### Client Modules with Simple Enums ✅
- `client-coinbase`: CoinbaseErrors enum (no HTTP status)
- `client-firebase-sms`: FirebaseSmsErrors enum (no HTTP status)

## Benefits of Layered Approach

1. **Clean Architecture**: HTTP concerns only where appropriate (service layer)
2. **Type Safety**: Compile-time error checking with enums
3. **Consistency**: All errors follow the same 4-field structure  
4. **Maintainability**: Error messages centralized in properties files
5. **Flexibility**: ErrorDetails for complex cases, simple enums for others
6. **Internationalization**: Easy to add multiple language support
7. **Simple HTTP Status**: Minimal status codes without over-engineering

## Quick Reference

```java
// Service modules (with HTTP status)
throw new StrategizException(ServiceAuthErrorDetails.INVALID_CREDENTIALS, "service-auth", userEmail);

// Other modules (HTTP-agnostic)
throw new StrategizException(CoinbaseErrors.API_CONNECTION_FAILED, "client-coinbase", endpoint);

// Properties file lookup automatic
// BaseController handles HTTP status mapping
// ErrorMessageService handles message formatting
```

This framework balances simplicity with proper architectural separation, keeping HTTP concerns in the service layer while maintaining clean, reusable error handling throughout the application. 