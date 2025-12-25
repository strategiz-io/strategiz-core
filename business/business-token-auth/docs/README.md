# Business Token Auth Module

## Overview

The `business-token-auth` module is the **single source of truth** for all token-related operations in Strategiz. It handles token creation, claims population, scope calculation, and session management using **PASETO V2 local tokens** (symmetric encryption).

## 🎯 Responsibilities

- **Token Creation & Signing** - Generate PASETO V2 local tokens
- **Claims Population** - Calculate and populate all token claims
- **Scope Management** - Determine user permissions based on authentication context
- **Session Management** - Handle token lifecycle and validation
- **Security Policies** - Enforce authentication and authorization rules

## 🎫 Token Model

### **3 Token Types, 2 Keys**

Strategiz issues three types of PASETO tokens using two separate signing keys:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         STRATEGIZ TOKEN MODEL                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                         identity-key                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────┐    │    │
│  │  │  IDENTITY TOKEN                                              │    │    │
│  │  │  • Purpose: Pre-authentication (signup, profile creation)    │    │    │
│  │  │  • ACR: 0                                                    │    │    │
│  │  │  • Scope: profile:create                                     │    │    │
│  │  │  • Duration: 30 minutes                                      │    │    │
│  │  │  • token_type: "identity"                                    │    │    │
│  │  └─────────────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                          session-key                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────┐    │    │
│  │  │  ACCESS TOKEN                                                │    │    │
│  │  │  • Purpose: Authenticated user sessions                      │    │    │
│  │  │  • ACR: 1, 2, or 3 (based on auth strength)                 │    │    │
│  │  │  • Scope: Full user scopes                                   │    │    │
│  │  │  • Duration: 24 hours                                        │    │    │
│  │  │  • token_type: "session"                                     │    │    │
│  │  └─────────────────────────────────────────────────────────────┘    │    │
│  │  ┌─────────────────────────────────────────────────────────────┐    │    │
│  │  │  REFRESH TOKEN                                               │    │    │
│  │  │  • Purpose: Obtain new access tokens                         │    │    │
│  │  │  • Duration: 7 days                                          │    │    │
│  │  │  • type: "REFRESH"                                           │    │    │
│  │  └─────────────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **Tokens vs Secrets**

| Classification | What It Is | Examples |
|----------------|------------|----------|
| **Token** | Credential YOU issue to clients | Identity Token, Access Token, Refresh Token |
| **Secret** | Credential YOU store from external sources | Provider OAuth credentials, API keys, signing keys |

Provider OAuth credentials (e.g., Coinbase access tokens) are **secrets**, not tokens in the Strategiz model.

## 🎫 Token Specification

### **Token Format: PASETO V2.local**

```
v2.local.eyJzdWIiOiJ1c3JfcHViXzd4OWsybThwNG42cSIsImp0aSI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMCIsImlzcyI6InN0cmF0ZWdpei5pbyIsImF1ZCI6InN0cmF0ZWdpeiIsImlhdCI6MTcwMzk4MDgwMCwiZXhwIjoxNzA0MDY3MjAwLCJhbXIiOlsxLDNdLCJhY3IiOiIyIiwiYXV0aF90aW1lIjoxNzAzOTgwODAwLCJzY29wZSI6InVzZXI6YmFzaWMifQ.encrypted_payload_here
```

### **Token Structure**
- **Header**: `v2.local.` (PASETO version 2, symmetric encryption)
- **Payload**: Encrypted and authenticated JSON claims
- **Authentication**: HMAC-SHA384 for authentication

## 📋 Claims Reference

### **Standard OIDC Claims**

| Claim | Type | Description | Example |
|-------|------|-------------|---------|
| `sub` | string | Public User ID | `"usr_pub_7x9k2m8p4n6q"` |
| `jti` | string | Unique Token ID (UUID v4) | `"550e8400-e29b-41d4-a716-446655440000"` |
| `iss` | string | Token Issuer | `"strategiz.io"` |
| `aud` | string | Token Audience | `"strategiz"` |
| `iat` | number | Issued At (Unix timestamp) | `1703980800` |
| `exp` | number | Expires At (Unix timestamp) | `1704067200` |
| `auth_time` | number | Authentication Time | `1703980800` |

### **Strategiz Custom Claims**

| Claim | Type | Description | Example |
|-------|------|-------------|---------|
| `amr` | array | Authentication Methods (numeric) | `[1, 3]` |
| `acr` | string | Authentication Context Reference | `"2"` |
| `scope` | string | Space-separated permissions | `"read:portfolio write:trades"` |

## 🔢 Numeric Mappings

### **Authentication Methods (AMR)**

```java
public class AuthMethodRegistry {
    private static final Map<Integer, String> AUTH_METHODS = Map.of(
        1, "password",
        2, "sms_otp", 
        3, "passkeys",
        4, "totp",
        5, "email_otp",
        6, "backup_codes"
    );
}
```

### **Authentication Context Reference (ACR)**

```java
public class AcrRegistry {
    // ACR Levels - Single value indicating authentication strength
    public static final String NO_AUTH = "0";           // No authentication / Partial signup
    public static final String SINGLE_FACTOR = "1";     // Basic authentication (password only)
    public static final String MULTI_FACTOR = "2";      // Standard MFA (password + TOTP/SMS)
    public static final String STRONG_MFA = "3";        // Hardware MFA (passkey + another factor)
}
```

**Important**: 
- **ACR "0"**: User cannot access protected resources (signup incomplete)
- **ACR "1"+**: User has valid authenticated session and can access account
- **ACR determines authentication strength, NOT authorization** - scopes are based on user entitlements

## 🔐 Progressive Authentication

### **Step 1: Profile Created**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "jti": "440e7300-d19a-31c4-b612-335544330000",
  "iss": "strategiz.io",
  "aud": "strategiz",
  "iat": 1703980700,
  "exp": 1703984300,
  "amr": [],
  "acr": "0",
  "scope": "read:profile write:profile read:auth_methods write:auth_methods"
}
```

### **Step 2A: Password Authentication**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "iss": "strategiz.io",
  "aud": "strategiz",
  "iat": 1703980800,
  "exp": 1704067200,
  "amr": [1],
  "acr": "1",
  "auth_time": 1703980800,
  "scope": "read:profile write:profile read:portfolio read:positions read:market_data read:watchlists write:watchlists"
}
```

### **Step 2B: Passkey Authentication**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "iss": "strategiz.io",
  "aud": "strategiz",
  "iat": 1703980800,
  "exp": 1704067200,
  "amr": [3],
  "acr": "3",
  "auth_time": 1703980800,
  "scope": "read:profile write:profile admin:portfolio read:positions write:positions delete:positions read:trades write:trades admin:strategies admin:settings"
}
```

### **Step 2C: Multi-Factor Authentication**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "iss": "strategiz.io",
  "aud": "strategiz",
  "iat": 1703980800,
  "exp": 1704067200,
  "amr": [1, 4],
  "acr": "2",
  "auth_time": 1703980800,
  "scope": "read:profile write:profile read:portfolio write:portfolio read:positions write:positions read:trades write:trades read:strategies write:strategies"
}
```

## 🎯 Scope Management

### **Scope Format**
```
{operation}:{resource}
```

### **Operations** (from `service-framework-base`)
- `read` - GET operations
- `write` - POST, PUT, PATCH operations  
- `delete` - DELETE operations
- `admin` - Administrative operations

### **Resources** (derived from endpoints)
- `profile` - User profile information
- `portfolio` - Portfolio data
- `positions` - Trading positions
- `trades` - Trade history and execution  
- `strategies` - Trading strategies
- `orders` - Order management
- `market_data` - Market information
- `watchlists` - User watchlists
- `settings` - Account settings
- `auth_methods` - Authentication methods
- `sessions` - Session management

### **Scope Calculation Logic**

```java
@Service
public class ScopeCalculationService {
    
    /**
     * Calculate user scopes based on entitlements and permissions
     * NOT based on ACR level - ACR only indicates authentication strength
     */
    public String calculateUserScopes(String userId) {
        // Fetch actual user permissions from database/service
        UserEntitlements entitlements = entitlementService.getForUser(userId);
        
        // Return scopes based on user's actual permissions
        return String.join(" ", entitlements.getScopes());
    }
    
    /**
     * Check if user can access resource based on ACR requirements
     * Some operations may require higher authentication levels
     */
    public boolean canAccessResource(String acr, String resource) {
        // Sensitive operations require MFA (ACR 2+)
        if (isSensitiveResource(resource)) {
            return Integer.parseInt(acr) >= 2;
        }
        
        // Standard resources require authentication (ACR 1+)
        return Integer.parseInt(acr) >= 1;
    }
}
```

## 🔒 Security Implementation

### **Token Creation**

```java
@Service
public class TokenCreationService {
    
    @Autowired
    private PasetoTokenService pasetoService;
    
    @Autowired
    private ScopeCalculationService scopeService;
    
    public String createToken(String userId, List<String> completedAuthMethods) {
        // Calculate ACR based on completed methods
        String acr = calculateAcr(userId, completedAuthMethods);

        // Build token claims
        TokenClaims claims = TokenClaims.builder()
            .subject(generatePublicUserId(userId))
            .jwtId(UUID.randomUUID().toString())
            .issuer("strategiz.io")
            .audience("strategiz")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(Duration.ofHours(24)))
            .authenticationMethods(encodeAuthMethods(completedAuthMethods))
            .authenticationContextClass(acr)
            .authenticationTime(Instant.now())
            .scope(scopeService.calculateScopes(acr, userId))
            .build();
            
        return pasetoService.generateToken(claims);
    }
}
```

### **Token Validation**

```java
@Service
public class TokenValidationService {
    
    public boolean isValidToken(String token) {
        try {
            TokenClaims claims = pasetoService.validateToken(token);
            return claims.getExpiresAt().isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean hasRequiredAccess(String token, String requiredAcr) {
        TokenClaims claims = pasetoService.validateToken(token);
        return meetsMinimumAcr(claims.getAcr(), requiredAcr);
    }
    
    public boolean hasScope(String token, String requiredScope) {
        TokenClaims claims = pasetoService.validateToken(token);
        List<String> scopes = Arrays.asList(claims.getScope().split(" "));
        return scopes.contains(requiredScope);
    }
}
```

## 🍪 Cookie Management

### **Cookie Settings**
```java
@Service
public class CookieService {
    
    public void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("strategiz_session", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        cookie.setSameSite("Strict");
        cookie.setDomain("strategiz.io");
        response.addCookie(cookie);
    }
}
```

## 🔄 Session Management

### **Session Lifecycle**
1. **Token Creation** - After successful authentication
2. **Token Validation** - On each request
3. **Token Refresh** - Before expiration (if needed)
4. **Token Revocation** - On logout or security events

### **Revocation Support**
```java
@Service  
public class TokenRevocationService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public void revokeToken(String jti) {
        // Add JTI to blacklist
        redisTemplate.opsForSet().add("revoked_tokens", jti);
        redisTemplate.expire("revoked_tokens", Duration.ofDays(1));
    }
    
    public boolean isTokenRevoked(String jti) {
        return redisTemplate.opsForSet().isMember("revoked_tokens", jti);
    }
}
```

## 🧪 Testing Examples

### **Unit Tests**
```java
@Test
public void testTokenCreation() {
    String userId = "user123";
    List<String> authMethods = Arrays.asList("password", "passkeys");
    
    String token = tokenCreationService.createToken(userId, authMethods);
    
    TokenClaims claims = pasetoService.validateToken(token);
    assertEquals("3", claims.getAcr());
    assertTrue(claims.getScope().contains("admin:portfolio"));
}
```

### **Integration Tests**
```java
@Test
public void testScopeBasedAccess() {
    // Create token with single-factor auth
    String token = createTestToken("1");
    
    // Should have read access
    assertTrue(tokenValidationService.hasScope(token, "read:portfolio"));
    
    // Should NOT have delete access
    assertFalse(tokenValidationService.hasScope(token, "delete:positions"));
}
```

## 📊 Metrics & Monitoring

### **Key Metrics**
- Token creation rate
- Token validation success/failure rate
- Authentication method usage
- ACR level distribution
- Scope usage patterns

### **Security Alerts**
- Multiple failed token validations
- Unusual authentication patterns
- High-value operations with low assurance
- Token revocation spikes

## 🔗 Integration with Services

### **Service Usage**
```java
// In any service controller
@RestController
public class ExampleController {
    
    @GetMapping("/data")
    @PreAuthorize("hasScope('read:data')")
    public ResponseEntity<?> getData(@AuthenticationPrincipal JwtToken token) {
        String userId = token.getSubject();
        String acr = token.getClaim("acr");
        
        // Business logic based on user and assurance level
        return ResponseEntity.ok(dataService.getData(userId, acr));
    }
}
```

## 🚀 Quick Start

1. **Add Dependency** - Include `business-token-auth` in your service
2. **Token Creation** - Use `TokenCreationService` after authentication
3. **Token Validation** - Use `@PreAuthorize` annotations
4. **Claims Access** - Extract user context from token claims

For complete implementation examples, see the `examples/` directory.
