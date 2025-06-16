# API Auth Module

Authentication and session management REST API controllers for the Strategiz platform.

## Overview

This module provides HTTP endpoints for user authentication and session management, following clean separation of concerns between authentication methods and session lifecycle management.

## Architecture

### Synapse Architecture Compliance

```
API Layer (Controllers) → SERVICE Layer → BUSINESS Layer → DATA Layer
```

- **Controllers**: Handle HTTP requests/responses only
- **Services**: Authentication-specific business logic
- **Business**: Shared session management logic
- **Data**: Token storage and retrieval

### Module Dependencies

```
api-auth depends on:
├── service-auth (authentication services)
├── business-token-auth (session management business logic)
└── data-auth (token persistence)
```

## Controllers

### 1. PasskeyController (`/auth/passkey`)

Handles WebAuthn (passkey) authentication using platform authenticators (Touch ID, Face ID, Windows Hello).

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/passkey/register/begin` | Start passkey registration |
| `POST` | `/auth/passkey/register/complete` | Complete passkey registration |
| `POST` | `/auth/passkey/authenticate/begin` | Start passkey authentication |
| `POST` | `/auth/passkey/authenticate/complete` | Complete passkey authentication |
| `GET` | `/auth/passkey/list` | List user's registered passkeys |
| `DELETE` | `/auth/passkey/{credentialId}` | Remove a passkey |

#### Authentication Flow

```
1. Client → POST /auth/passkey/authenticate/begin
   ← Server returns challenge

2. Client → WebAuthn ceremony (biometric verification)

3. Client → POST /auth/passkey/authenticate/complete + assertion
   ← Server returns session tokens (access + refresh)
```

#### Features

- **Platform-only authenticators**: Enforces biometric verification
- **Discoverable credentials**: Better UX with resident keys
- **Challenge-response security**: Prevents replay attacks
- **Device management**: List and remove registered passkeys

### 2. SessionController (`/auth/sessions`)

Handles session lifecycle management independent of authentication method.

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/sessions/refresh` | Refresh access token using refresh token |
| `POST` | `/auth/sessions/validate` | Validate access token |
| `POST` | `/auth/sessions/revoke` | Revoke specific session token |
| `POST` | `/auth/sessions/revoke-all/{userId}` | Revoke all sessions for user |

#### Session Management Flow

```
1. User authenticates via any method (passkey, TOTP, OAuth)
   → Receives session tokens

2. Client uses access token for API requests

3. When access token expires:
   Client → POST /auth/sessions/refresh + refresh token
   ← Server returns new access token

4. On logout:
   Client → POST /auth/sessions/revoke + token
   ← Server revokes session
```

#### Features

- **Method-agnostic**: Works with any authentication method
- **Token refresh**: Seamless session extension
- **Selective revocation**: Revoke individual or all sessions
- **Security validation**: Verify token authenticity and expiration

### 3. TotpController (`/auth/totp`)

Handles Time-Based One-Time Password (TOTP) authentication.

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/totp/setup` | Generate TOTP secret and QR code |
| `POST` | `/auth/totp/verify` | Verify TOTP code and complete setup |
| `POST` | `/auth/totp/authenticate` | Authenticate using TOTP code |
| `DELETE` | `/auth/totp/disable` | Disable TOTP for user |

## Authentication vs Session Management

### Clear Separation of Concerns

#### Authentication Controllers (Method-Specific)
- **Purpose**: Handle "how you get in"
- **Scope**: Method-specific logic (WebAuthn, TOTP, OAuth)
- **Output**: Session tokens upon successful authentication
- **Examples**: PasskeyController, TotpController

#### Session Controller (Universal)
- **Purpose**: Handle "managing your session once you're in"
- **Scope**: Method-agnostic session operations
- **Input**: Session tokens from any authentication method
- **Examples**: Token refresh, validation, revocation

### Benefits

✅ **Clean Architecture**: Authentication logic separate from session management  
✅ **Reusable**: Session management works for ALL auth methods  
✅ **Scalable**: Add new auth methods without touching session logic  
✅ **Frontend Simplicity**: One set of session APIs regardless of auth method  
✅ **Security**: Centralized token policies and management  

## Token Management

### Token Types

- **Access Token**: Short-lived (30 minutes), used for API authentication
- **Refresh Token**: Long-lived (7 days), used to obtain new access tokens

### Token Format

Uses **PASETO v2 (local)** tokens for security and simplicity:
- Symmetric encryption with secret key
- Built-in expiration and claims
- Tamper-proof and secure by design

### Token Claims

```json
{
  "sub": "user-id",
  "jti": "token-id", 
  "iat": 1640995200,
  "exp": 1640998800,
  "iss": "strategiz.io",
  "aud": "strategiz",
  "type": "ACCESS|REFRESH",
  "scope": "user session"
}
```

## Security Features

### WebAuthn Security
- **Platform authenticators only**: Requires biometric verification
- **Challenge-response**: Prevents replay attacks
- **Origin validation**: Prevents phishing attacks
- **Credential isolation**: Each credential is unique per user

### Session Security
- **Token rotation**: Refresh tokens can be rotated
- **Selective revocation**: Granular session control
- **Expiration enforcement**: Automatic cleanup of expired tokens
- **IP tracking**: Monitor session origins

### General Security
- **HTTPS only**: All endpoints require secure transport
- **Rate limiting**: Prevent brute force attacks
- **Input validation**: Comprehensive request validation
- **Error handling**: Secure error responses without information leakage

## API Response Format

All endpoints use consistent response format:

### Success Response
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* response data */ },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error description",
  "error": "ERROR_CODE",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Configuration

### Required Environment Variables

```properties
# Token Configuration
auth.token.access.validity=30m
auth.token.refresh.validity=7d
auth.token.secret=base64-encoded-secret
auth.token.issuer=strategiz.io
auth.token.audience=strategiz

# WebAuthn Configuration
webauthn.rp.id=strategiz.io
webauthn.rp.name=Strategiz
webauthn.origin=https://strategiz.io

# Session Configuration
session.expiry.seconds=86400
```

## Usage Examples

### Frontend Integration

```typescript
// Passkey Authentication
const passkeyClient = new PasskeyClient();
const tokens = await passkeyClient.authenticate();

// Session Management
const sessionClient = new SessionClient();
await sessionClient.refreshToken(tokens.refreshToken);
await sessionClient.validateToken(tokens.accessToken);
await sessionClient.revokeSession(tokens.accessToken);
```

### API Client Example

```javascript
// Authenticate with passkey
POST /auth/passkey/authenticate/begin
POST /auth/passkey/authenticate/complete
// → Returns { accessToken, refreshToken }

// Use access token for API calls
GET /api/user/profile
Authorization: Bearer <accessToken>

// Refresh when needed
POST /auth/sessions/refresh
{ "refreshToken": "<refreshToken>" }
// → Returns { accessToken }

// Logout
POST /auth/sessions/revoke
{ "token": "<accessToken>" }
```

## Development

### Running Tests
```bash
mvn test
```

### Building Module
```bash
mvn clean compile
mvn clean install
```

### Dependencies
- Spring Boot (Web, Security, Validation)
- PASETO library for token generation
- WebAuthn4J for passkey verification
- TOTP library for time-based codes

## Future Enhancements

- [ ] OAuth 2.0 / OIDC integration
- [ ] Multi-factor authentication flows
- [ ] Device registration and management
- [ ] Advanced session analytics
- [ ] Adaptive authentication based on risk
- [ ] WebAuthn Level 3 features

## Related Documentation

- [Synapse Architecture Guidelines](../../../docs/ARCHITECTURE.md)
- [Security Best Practices](../../../docs/SECURITY.md)
- [API Design Standards](../../../docs/API_DESIGN.md)
- [Frontend Integration Guide](../../../docs/FRONTEND_INTEGRATION.md)
