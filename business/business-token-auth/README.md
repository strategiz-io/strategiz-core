# Business Token Auth Module

## Overview

The `business-token-auth` module is the **single source of truth** for all token-related operations in Strategiz. It handles token creation, claims population, scope calculation, and session management using **PASETO v4.public tokens**.

## 🎯 Responsibilities

- **Token Creation & Signing** - Generate PASETO v4.public tokens
- **Claims Population** - Calculate and populate all JWT claims
- **Scope Management** - Determine user permissions based on authentication context
- **Session Management** - Handle token lifecycle and validation
- **Security Policies** - Enforce authentication and authorization rules

## 🎫 Token Specification

### **Token Format: PASETO v4.public**

```
v4.public.eyJzdWIiOiJ1c3JfcHViXzd4OWsybThwNG42cSIsImp0aSI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMCIsImlzcyI6InN0cmF0ZWdpei5pbyIsImF1ZCI6InN0cmF0ZWdpeiIsImlhdCI6MTcwMzk4MDgwMCwiZXhwIjoxNzA0MDY3MjAwLCJhbXIiOlsxLDNdLCJhY3IiOiIyLjMiLCJhdXRoX3RpbWUiOjE3MDM5ODA4MDAsInNjb3BlIjoidXNlcjpiYXNpYyJ9.signature_here
```

### **Token Structure**
- **Header**: `v4.public.` (PASETO version 4, public key)
- **Payload**: Base64URL encoded JSON claims
- **Signature**: Ed25519 signature (32 bytes)

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
| `acr` | string | Authentication Context Class | `"2.3"` |
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

### **Authentication Context Class (ACR)**

```java
public class AcrRegistry {
    // Authentication Levels
    public static final String NONE = "0";           // No authentication
    public static final String PARTIAL = "1";        // Profile only
    
    // Full Authentication + Assurance Levels
    public static final String AUTH_BASIC = "2.1";        // AAL1 - Single factor
    public static final String AUTH_SUBSTANTIAL = "2.2";  // AAL2 - Multi-factor  
    public static final String AUTH_HIGH = "2.3";         // AAL3 - Hardware crypto
}
```

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
  "acr": "1",
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
  "acr": "2.1",
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
  "acr": "2.3",
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
  "acr": "2.2",
  "auth_time": 1703980800,
  "scope": "read:profile write:profile read:portfolio write:portfolio read:positions write:positions read:trades write:trades read:strategies write:strategies"
}
```

## 🎯 Scope Management

### **Scope Format**
```
{operation}:{resource}
```

### **Operations** (from `service-base`)
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
    
    public String calculateScopes(String acr, String userId) {
        List<String> scopes = new ArrayList<>();
        
        switch (acr) {
            case "0":
                // Anonymous - no scopes
                break;
                
            case "1":
                // Partial authentication - profile and auth setup only
                scopes.addAll(Arrays.asList(
                    "read:profile", "write:profile",
                    "read:auth_methods", "write:auth_methods"
                ));
                break;
                
            case "2.1":
                // Basic assurance - read-only plus basic operations
                scopes.addAll(Arrays.asList(
                    "read:profile", "write:profile",
                    "read:portfolio", "read:positions",
                    "read:market_data", 
                    "read:watchlists", "write:watchlists",
                    "read:strategies"
                ));
                break;
                
            case "2.2":
                // Substantial assurance - most trading operations
                scopes.addAll(Arrays.asList(
                    "read:profile", "write:profile",
                    "read:portfolio", "write:portfolio",
                    "read:positions", "write:positions",
                    "read:trades", "write:trades",
                    "read:strategies", "write:strategies",
                    "read:market_data",
                    "read:watchlists", "write:watchlists"
                ));
                break;
                
            case "2.3":
                // High assurance - all operations including sensitive
                scopes.addAll(Arrays.asList(
                    "read:profile", "write:profile", "delete:profile",
                    "read:portfolio", "write:portfolio", "admin:portfolio",
                    "read:positions", "write:positions", "delete:positions",
                    "read:trades", "write:trades", "delete:trades",
                    "read:strategies", "write:strategies", "admin:strategies",
                    "read:market_data",
                    "read:watchlists", "write:watchlists",
                    "read:settings", "write:settings", "admin:settings",
                    "read:auth_methods", "write:auth_methods",
                    "read:sessions", "write:sessions"
                ));
                break;
        }
        
        return String.join(" ", scopes);
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
    assertEquals("2.3", claims.getAcr());
    assertTrue(claims.getScope().contains("admin:portfolio"));
}
```

### **Integration Tests**
```java
@Test
public void testScopeBasedAccess() {
    // Create token with basic assurance
    String token = createTestToken("2.1");
    
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
