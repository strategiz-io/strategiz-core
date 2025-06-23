# business-token-auth

## Overview

The `business-token-auth` module provides centralized token management and authentication services for the Strategiz platform using PASETO tokens. This module is responsible for securely creating, validating, refreshing, and revoking authentication tokens.

## Features

- **Secure Token Management**: Uses PASETO (Platform-Agnostic Security Tokens) for cryptographically secure tokens
- **Token Persistence**: Stores tokens with metadata for comprehensive session management
- **Refresh Token Workflow**: Implements secure token refresh mechanism
- **Token Revocation**: Supports revoking single tokens or all tokens for a user
- **Token Expiration**: Automatic cleanup of expired tokens
- **Session Management**: Unified session handling for the entire platform

## Key Components

### SessionAuthBusiness

Core business logic for session and token management:

- Token pair (access + refresh) creation
- Token validation
- Token refresh
- Token revocation
- User session management
- Scheduled cleanup of expired tokens

### PasetoTokenProvider

Low-level utility for PASETO token operations:

- Token generation (V2/symmetric and V4/asymmetric)
- Token parsing and validation
- Claims extraction and verification
- Token lifecycle management

## Configuration

### Required Properties

```properties
# Token configuration
auth.token.version=v2                # Token version (v2 or v4)
auth.token.secret=your-secret-key    # Base64-encoded secret key (32 bytes) for V2 tokens
auth.token.access.validity=30m       # Access token validity duration
auth.token.refresh.validity=7d       # Refresh token validity duration
auth.token.audience=strategiz        # Token audience claim
auth.token.issuer=strategiz.io       # Token issuer claim

# Session configuration
session.expiry.seconds=86400         # Session expiry in seconds (24 hours by default)
```

## Usage Guide

### Creating a Session

```java
// Inject the service
@Autowired
private SessionAuthBusiness sessionAuthBusiness;

// Create a token pair (access + refresh tokens)
String userId = "user-123";
String deviceId = "device-456";  // Optional
String ipAddress = "192.168.1.1"; // Client IP

TokenPair tokens = sessionAuthBusiness.createTokenPair(userId, deviceId, ipAddress, "user", "admin");

// Use the tokens
String accessToken = tokens.accessToken();
String refreshToken = tokens.refreshToken();
```

### Validating a Token

```java
boolean isValid = sessionAuthBusiness.validateSession(accessToken).isPresent();

// Or to get the user ID from a valid token
Optional<String> userId = sessionAuthBusiness.validateSession(accessToken);
if (userId.isPresent()) {
    // Token is valid, proceed with the user ID
}
```

### Refreshing a Token

```java
Optional<String> newAccessToken = sessionAuthBusiness.refreshAccessToken(refreshToken, clientIpAddress);
if (newAccessToken.isPresent()) {
    // Use the new access token
} else {
    // Refresh token was invalid or expired
}
```

### Revoking Tokens

```java
// Revoke a single token
boolean revoked = sessionAuthBusiness.deleteSession(accessToken);

// Revoke all tokens for a user
boolean allRevoked = sessionAuthBusiness.deleteUserSessions(userId);
```

## Integration

This module should be used as the single source of truth for all token and session management across the Strategiz platform. Other modules (like `service-auth`) should delegate their token and session management to this module.

Example integration with `service-auth`:

```java
@Service
public class SessionService {
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SessionService(SessionAuthBusiness sessionAuthBusiness) {
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    public String createSession(String userId) {
        return sessionAuthBusiness.createTokenPair(userId, null, null).accessToken();
    }
    
    public boolean validateSession(String token) {
        return sessionAuthBusiness.validateSession(token).isPresent();
    }
}
```

## Security Considerations

1. **Secret Management**: Always use a secure, environment-specific secret key in production.
2. **Token Storage**: Store tokens securely on client-side (HttpOnly cookies preferred).
3. **Token Validation**: Always validate tokens server-side before granting access.
4. **Token Expiry**: Use appropriate expiration times for access tokens (short-lived) and refresh tokens (longer-lived).
5. **Revocation**: Implement proper token revocation on logout or security events.

## Dependencies

- `jpaseto`: Java implementation of PASETO
- Spring Framework
- `data-auth`: For token persistence

## Cleanup Process

The module automatically cleans up expired tokens via a scheduled job that runs hourly. This helps keep the database clean and ensures optimal performance.
