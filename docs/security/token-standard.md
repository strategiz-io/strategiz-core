# Token Standard

**Version**: 1.0
**Last Updated**: January 2026
**Status**: MANDATORY for all authentication modules

---

## Table of Contents

1. [Overview](#overview)
2. [Token Types](#token-types)
3. [Token Claims Specification](#token-claims-specification)
4. [AMR (Authentication Methods) Encoding](#amr-authentication-methods-encoding)
5. [ACR (Authentication Context) Levels](#acr-authentication-context-levels)
6. [Token Expiry Configuration](#token-expiry-configuration)
7. [Cookie Configuration](#cookie-configuration)
8. [Security Best Practices](#security-best-practices)
9. [Implementation Reference](#implementation-reference)

---

## Overview

Strategiz uses **PASETO V2.local** tokens for authentication. PASETO (Platform-Agnostic Security Tokens) provides:

- **Symmetric encryption** - Token payload is encrypted, not just signed
- **Stateless validation** - All information needed is in the token
- **Tamper-proof** - Cryptographically authenticated

### Two-Key Security Model

| Key | Vault Path | Purpose | Used For |
|-----|------------|---------|----------|
| `identity-key` | `tokens.{env}.identity-key` | Pre-authentication | Identity tokens, Recovery tokens |
| `session-key` | `tokens.{env}.session-key` | Authenticated sessions | Access tokens, Refresh tokens |

This separation ensures that a compromise of the identity key (used in the exposed signup flow) does not grant access to authenticated sessions.

### Token Storage

Tokens are delivered via **HTTP-only cookies** to prevent XSS attacks:

```http
Set-Cookie: strategiz-access-token=v2.local.eyJ...;
           HttpOnly; Secure; SameSite=Lax;
           Path=/; Max-Age=1800; Domain=strategiz.io
```

---

## Token Types

Strategiz issues four types of tokens:

| Token | Purpose | Key | Duration | ACR |
|-------|---------|-----|----------|-----|
| **Access Token** | API authorization | session-key | 30 minutes | 1-3 |
| **Refresh Token** | Obtain new access tokens | session-key | 7 days | N/A |
| **Identity Token** | Signup flow (pre-auth) | identity-key | 15 minutes | 0 |
| **Recovery Token** | Account recovery actions | identity-key | 15 minutes | 0 |

---

## Token Claims Specification

### Access Token Claims

Used for API authorization. Contains full authentication context.

| Claim | Required | Type | Description | Example |
|-------|----------|------|-------------|---------|
| `sub` | Yes | String | User ID | `"user_abc123"` |
| `iat` | Yes | Number | Issued at (Unix timestamp) | `1704067200` |
| `exp` | Yes | Number | Expiration (Unix timestamp) | `1704069000` |
| `iss` | Yes | String | Issuer | `"strategiz.io"` |
| `aud` | Yes | String | Audience | `"strategiz"` |
| `type` | Yes | String | Token type identifier | `"ACCESS"` |
| `acr` | Yes | String | Authentication context reference | `"2"` |
| `amr` | Yes | Array[Number] | Authentication methods used | `[1, 4]` |
| `scope` | Optional | String | Permissions (space-separated) | `"read write"` |
| `jti` | Optional | String | Unique token ID | `"tok_xyz789"` |
| `demoMode` | Optional | Boolean | Demo mode flag | `false` |
| `auth_time` | Optional | Number | When authentication occurred | `1704067200` |

### Refresh Token Claims

Used only to obtain new access tokens. **Keep minimal** - no auth context needed.

| Claim | Required | Type | Description | Example |
|-------|----------|------|-------------|---------|
| `sub` | Yes | String | User ID | `"user_abc123"` |
| `iat` | Yes | Number | Issued at | `1704067200` |
| `exp` | Yes | Number | Expiration | `1704672000` |
| `iss` | Yes | String | Issuer | `"strategiz.io"` |
| `type` | Yes | String | Token type identifier | `"REFRESH"` |
| `jti` | Yes | String | Unique ID (for revocation tracking) | `"ref_xyz789"` |

**Important:** Refresh tokens should NOT include `acr`, `amr`, or `scope`. They are only for obtaining new access tokens.

### Identity Token Claims

Used during signup flow before user is fully authenticated.

| Claim | Required | Type | Description | Example |
|-------|----------|------|-------------|---------|
| `sub` | Yes | String | User ID | `"user_abc123"` |
| `iat` | Yes | Number | Issued at | `1704067200` |
| `exp` | Yes | Number | Expiration | `1704068100` |
| `iss` | Yes | String | Issuer | `"strategiz.io"` |
| `type` | Yes | String | Token type identifier | `"IDENTITY"` |
| `acr` | Yes | String | Always "0" (unauthenticated) | `"0"` |
| `scope` | Yes | String | Limited scope | `"profile:create"` |

### Recovery Token Claims

Used for account recovery actions (disable MFA, reset passkey, etc.).

| Claim | Required | Type | Description | Example |
|-------|----------|------|-------------|---------|
| `sub` | Yes | String | User ID | `"user_abc123"` |
| `iat` | Yes | Number | Issued at | `1704067200` |
| `exp` | Yes | Number | Expiration | `1704068100` |
| `iss` | Yes | String | Issuer | `"strategiz.io"` |
| `type` | Yes | String | Token type identifier | `"RECOVERY"` |
| `acr` | Yes | String | Always "0" (unauthenticated) | `"0"` |
| `scope` | Yes | String | Recovery scope | `"account:recover"` |
| `recovery_id` | Yes | String | Unique recovery session ID | `"rec_abc123"` |

---

## AMR (Authentication Methods) Encoding

The `amr` claim contains an array of numeric codes representing authentication methods used:

| Code | Method | Description |
|------|--------|-------------|
| 1 | `password` | Email/password authentication |
| 2 | `sms_otp` | SMS one-time password |
| 3 | `passkeys` | FIDO2/WebAuthn passkeys |
| 4 | `totp` | Time-based one-time password (Google Authenticator, etc.) |
| 5 | `email_otp` | Email one-time password |
| 6 | `backup_codes` | Recovery backup codes |
| 7 | `google` | Google OAuth |
| 8 | `facebook` | Facebook OAuth |
| 9 | `apple` | Apple Sign-In |
| 10 | `microsoft` | Microsoft OAuth |

**Example:** User logged in with password + TOTP: `amr: [1, 4]`

---

## ACR (Authentication Context) Levels

The `acr` claim indicates the strength of authentication:

| Level | Name | Requirements | Use Cases |
|-------|------|--------------|-----------|
| **0** | Partial/Signup | In-progress signup, unverified | Signup flow, email verification |
| **1** | Single-Factor | One factor (password, SMS OTP, or Email OTP) | Basic operations, viewing data |
| **2** | Multi-Factor | Two or more factors | Money transfers, trading, settings changes |
| **3** | Strong MFA | Passkey + another factor | Live trading, large transfers, admin actions |

### ACR Calculation Logic

```
if (isPartialAuth OR no auth methods):
    ACR = "0"
else if (passkeys + another method):
    ACR = "3"
else if (2+ methods OR just passkeys):
    ACR = "2"
else:
    ACR = "1"
```

---

## Token Expiry Configuration

### Configurable Tokens

| Token | Default | Config Property | Notes |
|-------|---------|-----------------|-------|
| Access Token | 30 min (1800s) | `app.cookie.access-token-max-age` | Short-lived for security |
| Refresh Token | 7 days (604800s) | `app.cookie.refresh-token-max-age` | Session continuity |

### Hardcoded Tokens

| Token | Duration | Rationale |
|-------|----------|-----------|
| Identity Token | 15 minutes | Signup flow should complete quickly |
| Recovery Token | 15 minutes | Recovery actions are time-sensitive |

### Configuration Files

**Development** (`application-dev.properties`):
```properties
app.cookie.access-token-max-age=1800      # 30 minutes
app.cookie.refresh-token-max-age=604800   # 7 days
```

**Production** (`application-prod.properties`):
```properties
# Uses defaults (same as dev) or override via environment variables
```

### Critical Rule

**Token `exp` claim MUST match cookie `max-age`.** Both are configured from the same property to ensure consistency.

---

## Cookie Configuration

### Cookie Attributes

| Cookie Name | HttpOnly | Secure | SameSite | MaxAge | Path |
|-------------|----------|--------|----------|--------|------|
| `strategiz-access-token` | Yes | Yes | Configurable | 30 min | `/` |
| `strategiz-refresh-token` | Yes | Yes | Configurable | 7 days | `/` |
| `strategiz-session` | Yes | Yes | Configurable | Session | `/` |

### Environment-Specific Configuration

| Setting | Development | Production |
|---------|-------------|------------|
| `app.cookie.secure` | `true` | `true` |
| `app.cookie.domain` | `localhost` | `strategiz.io` |
| `app.cookie.same-site` | `Lax` | `None` (cross-subdomain) |

### Cookie Clearing

When clearing cookies (logout), you MUST set the same attributes as when they were created, otherwise modern browsers may not clear them properly.

---

## Security Best Practices

### Token Design

1. **`type` claim prevents token misuse** - A refresh token cannot be used as an access token
2. **Refresh tokens = minimal claims** - Only include what's needed to issue new access tokens
3. **`jti` on refresh tokens** - Enables revocation tracking in the database
4. **Token expiry matches cookie max-age** - Both configured from the same property

### Token Validation

1. **Always validate signature first** - Before reading any claims
2. **Check `exp` claim** - Reject expired tokens
3. **Verify `type` claim** - Ensure token is being used for its intended purpose
4. **Check `acr` for sensitive operations** - Require MFA for high-risk actions

### Storage Security

1. **Never store tokens in localStorage** - Use HTTP-only cookies
2. **Never log token values** - They contain sensitive information
3. **Rotate signing keys periodically** - Store in Vault with versioning

### Do NOT

- Include sensitive data in token claims (PII, credentials)
- Use tokens as permanent storage (they expire)
- Skip signature validation
- Trust client-provided user IDs (always extract from token)

---

## Implementation Reference

### Token Creation

**File:** `framework/framework-token-issuance/src/main/java/io/strategiz/framework/token/issuer/PasetoTokenIssuer.java`

Key methods:
- `createAuthenticationToken()` - Creates access tokens
- `createRefreshToken()` - Creates refresh tokens
- `createIdentityToken()` - Creates identity tokens
- `createRecoveryToken()` - Creates recovery tokens
- `calculateAcr()` - Calculates ACR from auth methods

### Session Management

**File:** `business/business-token-auth/src/main/java/io/strategiz/business/tokenauth/SessionAuthBusiness.java`

Key methods:
- `createAuthentication()` - Creates access + refresh token pair
- `validateToken()` - Validates token and extracts claims
- `revokeAuthentication()` - Revokes a token/session

### Cookie Management

**File:** `service/service-auth/src/main/java/io/strategiz/service/auth/util/CookieUtil.java`

Key methods:
- `setAccessTokenCookie()` - Sets access token cookie
- `setRefreshTokenCookie()` - Sets refresh token cookie
- `clearAuthCookies()` - Clears all auth cookies (logout)

---

## Related Documentation

- [Authorization Standard](./authorization-standard.md) - Endpoint protection with `@RequireAuth`
- [Security Overview](./overview.md) - High-level security architecture
- [Secrets Management](./secrets-management.md) - Vault integration for signing keys

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | January 2026 | Initial release - comprehensive token standard |

---

## Approval

This standard is **MANDATORY** for all modules that create or validate tokens.

**Approved by**: Engineering Team
**Effective Date**: January 2026
**Review Cycle**: Quarterly
