<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/badge/PASETO-v4-blue?style=for-the-badge" alt="PASETO v4"/>
  <img src="https://img.shields.io/badge/OpenFGA-Ready-purple?style=for-the-badge" alt="OpenFGA"/>
</p>

# Authorization Framework

> **A comprehensive, two-layer authorization framework for Strategiz-Core providing both coarse-grained scope checks and fine-grained relationship-based access control.**

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Annotations Reference](#annotations-reference)
  - [@RequireAuth](#requireauth)
  - [@RequireScope](#requirescope)
  - [@Authorize](#authorize)
  - [@AuthUser](#authuser)
- [AuthenticatedUser API](#authenticateduser-api)
- [SecurityContextHolder](#securitycontextholder)
- [Fine-Grained Authorization (FGA)](#fine-grained-authorization-fga)
- [Error Handling](#error-handling)
- [Configuration](#configuration)
- [Migration Guide](#migration-guide)
- [Testing](#testing)
- [Reference Tables](#reference-tables)

---

## Overview

The Authorization Framework implements a **defense-in-depth** security model with two complementary layers:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│   Layer 1: SCOPE-BASED (Coarse-Grained)                                     │
│   ══════════════════════════════════════                                    │
│   • Fast, local check (no network call)                                     │
│   • Validates PASETO token scopes                                           │
│   • Answers: "Can this user perform this TYPE of action?"                   │
│   • Example: portfolio:read, trading:execute                                │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Layer 2: FGA (Fine-Grained)                                               │
│   ══════════════════════════════                                            │
│   • Relationship-based authorization                                        │
│   • Validates user→resource relationships                                   │
│   • Answers: "Can this user access THIS SPECIFIC resource?"                 │
│   • Example: user:123 → owner → portfolio:abc                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why Two Layers?

| Aspect | Layer 1 (Scope) | Layer 2 (FGA) |
|--------|-----------------|---------------|
| **Speed** | Instant (in-token) | Fast (cached/mock) |
| **Granularity** | Action-level | Resource-level |
| **Use Case** | "Can read portfolios?" | "Can read THIS portfolio?" |
| **Data Source** | PASETO claims | Relationship tuples |

---

## Architecture

### Request Flow

```
                                    ┌─────────────────────────────┐
                                    │      HTTP Request           │
                                    │  Authorization: Bearer xxx  │
                                    └──────────────┬──────────────┘
                                                   │
                                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                        PasetoAuthenticationFilter                            │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  1. Extract token from Authorization header or Cookie                  │  │
│  │  2. Validate PASETO v4 signature                                       │  │
│  │  3. Extract claims: sub, scope, acr, amr, demoMode                     │  │
│  │  4. Build AuthenticatedUser and store in SecurityContext               │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                              AOP Aspects                                     │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                         @RequireAuth                                 │     │
│  │  ┌─────────────────────────────────────────────────────────────┐    │     │
│  │  │  ✓ Is user authenticated?                                   │    │     │
│  │  │  ✓ Does ACR level meet minimum requirement?                 │    │     │
│  │  │  ✓ Is demo mode allowed for this endpoint?                  │    │     │
│  │  └─────────────────────────────────────────────────────────────┘    │     │
│  └─────────────────────────────────────────────────────────────────────┘     │
│                                       │                                      │
│                                       ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                         @RequireScope                                │     │
│  │  ┌─────────────────────────────────────────────────────────────┐    │     │
│  │  │  ✓ Does token contain required scope(s)?                    │    │     │
│  │  │  ✓ Match mode: ANY (at least one) or ALL (every scope)      │    │     │
│  │  └─────────────────────────────────────────────────────────────┘    │     │
│  └─────────────────────────────────────────────────────────────────────┘     │
│                                       │                                      │
│                                       ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                          @Authorize                                  │     │
│  │  ┌─────────────────────────────────────────────────────────────┐    │     │
│  │  │  ✓ Resolve resourceId from SpEL expression                  │    │     │
│  │  │  ✓ Check FGA: user:{id} → {relation} → {resource}:{id}      │    │     │
│  │  └─────────────────────────────────────────────────────────────┘    │     │
│  └─────────────────────────────────────────────────────────────────────┘     │
│                                                                              │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
                          ┌────────────────────────┐
                          │   Controller Method    │
                          │      Executes          │
                          └────────────────────────┘
```

### Module Structure

```
framework-authorization/
├── 📁 annotation/
│   ├── AuthUser.java           # Parameter injection annotation
│   ├── Authorize.java          # FGA relationship check
│   ├── RequireAuth.java        # Authentication + ACR check
│   ├── RequireScope.java       # Scope validation
│   ├── ResourceType.java       # FGA resource type enum
│   └── ScopeMode.java          # ANY/ALL matching mode
│
├── 📁 aspect/
│   ├── AuthenticationAspect.java      # Processes @RequireAuth
│   ├── ScopeAuthorizationAspect.java  # Processes @RequireScope
│   └── FGAAuthorizationAspect.java    # Processes @Authorize
│
├── 📁 config/
│   ├── AuthorizationAutoConfiguration.java  # Spring Boot auto-config
│   └── AuthorizationProperties.java         # Configuration properties
│
├── 📁 context/
│   ├── AuthenticatedUser.java    # Immutable user representation
│   ├── SecurityContext.java      # Request-scoped context
│   └── SecurityContextHolder.java # Thread-local holder
│
├── 📁 exception/
│   └── AuthorizationErrorDetails.java  # Error definitions
│
├── 📁 fga/
│   ├── FGAClient.java       # Interface (swap for real OpenFGA)
│   ├── FGAMockClient.java   # In-memory implementation
│   └── TupleKey.java        # Relationship tuple record
│
├── 📁 filter/
│   └── PasetoAuthenticationFilter.java  # Token extraction
│
└── 📁 resolver/
    └── AuthUserArgumentResolver.java    # @AuthUser resolution
```

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-authorization</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Configure (Optional)

```properties
# Enable/disable (default: true)
strategiz.authorization.enabled=true

# Custom skip paths (merged with defaults)
strategiz.authorization.skip-paths=/custom/public/**,/webhooks/**
```

### 3. Annotate Your Endpoints

```java
@RestController
@RequestMapping("/v1/portfolios")
public class PortfolioController {

    @GetMapping("/{id}")
    @RequireAuth                              // ① Must be authenticated
    @RequireScope("portfolio:read")           // ② Must have scope
    @Authorize(                               // ③ Must be owner/viewer
        relation = "viewer",
        resource = ResourceType.PORTFOLIO,
        resourceId = "#id"
    )
    public ResponseEntity<Portfolio> getPortfolio(
            @PathVariable String id,
            @AuthUser AuthenticatedUser user) {  // ④ Auto-injected

        log.info("User {} accessing portfolio {}", user.getUserId(), id);
        return ResponseEntity.ok(portfolioService.findById(id));
    }
}
```

**That's it!** The framework handles token validation, scope checking, and relationship authorization automatically.

---

## Annotations Reference

### @RequireAuth

Ensures the request is authenticated and optionally validates the authentication level.

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    String minAcr() default "1";      // Minimum ACR level required
    boolean allowDemoMode() default true;  // Allow demo users?
}
```

#### Usage Examples

```java
// ═══════════════════════════════════════════════════════════════════════════
// Basic Authentication
// ═══════════════════════════════════════════════════════════════════════════
@GetMapping("/dashboard")
@RequireAuth  // Any authenticated user (ACR >= 1)
public ResponseEntity<Dashboard> getDashboard() { ... }


// ═══════════════════════════════════════════════════════════════════════════
// Require Multi-Factor Authentication
// ═══════════════════════════════════════════════════════════════════════════
@PostMapping("/settings/security")
@RequireAuth(minAcr = "2")  // MFA required (TOTP, SMS, etc.)
public ResponseEntity<?> updateSecuritySettings() { ... }


// ═══════════════════════════════════════════════════════════════════════════
// High-Security Operations
// ═══════════════════════════════════════════════════════════════════════════
@PostMapping("/trading/execute")
@RequireAuth(minAcr = "3", allowDemoMode = false)  // Hardware key + no demo
public ResponseEntity<?> executeTrade() { ... }


// ═══════════════════════════════════════════════════════════════════════════
// Class-Level Authentication
// ═══════════════════════════════════════════════════════════════════════════
@RestController
@RequireAuth(minAcr = "2")  // All endpoints in this controller require MFA
@RequestMapping("/v1/admin")
public class AdminController { ... }
```

#### ACR (Authentication Context Class Reference) Levels

| Level | Name | Description | Examples |
|:-----:|------|-------------|----------|
| `0` | None | Partial/no authentication | Signup in progress |
| `1` | Single-Factor | Password only | Standard login |
| `2` | Multi-Factor | Password + one factor | TOTP, SMS OTP, Email OTP |
| `3` | Strong MFA | Hardware key + factor | Passkey + TOTP |

---

### @RequireScope

Validates that the user's PASETO token contains the required scope(s).

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireScope {
    String[] value();                    // Required scope(s)
    ScopeMode mode() default ScopeMode.ALL;  // ALL or ANY
}
```

#### Usage Examples

```java
// ═══════════════════════════════════════════════════════════════════════════
// Single Scope
// ═══════════════════════════════════════════════════════════════════════════
@GetMapping
@RequireScope("portfolio:read")
public ResponseEntity<List<Portfolio>> listPortfolios() { ... }


// ═══════════════════════════════════════════════════════════════════════════
// ANY Mode - User needs at least ONE of the scopes
// ═══════════════════════════════════════════════════════════════════════════
@GetMapping("/{id}")
@RequireScope(value = {"portfolio:read", "portfolio:admin"}, mode = ScopeMode.ANY)
public ResponseEntity<Portfolio> getPortfolio(@PathVariable String id) { ... }


// ═══════════════════════════════════════════════════════════════════════════
// ALL Mode (default) - User needs EVERY scope
// ═══════════════════════════════════════════════════════════════════════════
@DeleteMapping("/{id}")
@RequireScope({"portfolio:read", "portfolio:write", "portfolio:delete"})
public ResponseEntity<Void> deletePortfolio(@PathVariable String id) { ... }


// ═══════════════════════════════════════════════════════════════════════════
// Combining with @RequireAuth
// ═══════════════════════════════════════════════════════════════════════════
@PostMapping("/strategies/execute")
@RequireAuth(minAcr = "2")              // MFA required
@RequireScope("strategy:execute")        // Must have execute scope
public ResponseEntity<?> executeStrategy() { ... }
```

#### Scope Naming Convention

```
{domain}:{action}

Examples:
  portfolio:read      - Read portfolio data
  portfolio:write     - Modify portfolio
  trading:execute     - Execute trades
  strategy:backtest   - Run backtests
  admin:users         - Manage users
```

---

### @Authorize

Fine-grained authorization checking user→resource relationships via FGA (Fine-Grained Authorization).

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorize {
    String relation();           // Required relation (e.g., "owner", "viewer")
    ResourceType resource();     // Resource type
    String resourceId();         // SpEL expression for resource ID
}
```

#### Usage Examples

```java
// ═══════════════════════════════════════════════════════════════════════════
// Simple Path Variable
// ═══════════════════════════════════════════════════════════════════════════
@DeleteMapping("/{portfolioId}")
@Authorize(
    relation = "owner",
    resource = ResourceType.PORTFOLIO,
    resourceId = "#portfolioId"        // References @PathVariable
)
public ResponseEntity<Void> deletePortfolio(@PathVariable String portfolioId) { ... }


// ═══════════════════════════════════════════════════════════════════════════
// Request Body Property
// ═══════════════════════════════════════════════════════════════════════════
@PostMapping("/share")
@Authorize(
    relation = "owner",
    resource = ResourceType.STRATEGY,
    resourceId = "#request.strategyId"  // Nested property access
)
public ResponseEntity<?> shareStrategy(@RequestBody ShareRequest request) { ... }


// ═══════════════════════════════════════════════════════════════════════════
// Viewer Relation (Inherited from Owner)
// ═══════════════════════════════════════════════════════════════════════════
@GetMapping("/{id}/analytics")
@Authorize(
    relation = "viewer",               // Owners are also viewers
    resource = ResourceType.PORTFOLIO,
    resourceId = "#id"
)
public ResponseEntity<Analytics> getAnalytics(@PathVariable String id) { ... }


// ═══════════════════════════════════════════════════════════════════════════
// Full Stack Example
// ═══════════════════════════════════════════════════════════════════════════
@PutMapping("/{id}")
@RequireAuth(minAcr = "2")                    // Layer 0: MFA required
@RequireScope("portfolio:write")               // Layer 1: Scope check
@Authorize(                                    // Layer 2: FGA check
    relation = "owner",
    resource = ResourceType.PORTFOLIO,
    resourceId = "#id"
)
public ResponseEntity<Portfolio> updatePortfolio(
        @PathVariable String id,
        @RequestBody UpdatePortfolioRequest request,
        @AuthUser AuthenticatedUser user) {

    // If we get here, user:
    // ✓ Is authenticated with MFA
    // ✓ Has portfolio:write scope
    // ✓ Is the owner of this specific portfolio

    return ResponseEntity.ok(portfolioService.update(id, request, user.getUserId()));
}
```

#### SpEL Expression Reference

| Expression | Description | Example |
|------------|-------------|---------|
| `#paramName` | Method parameter by name | `#portfolioId` |
| `#param.property` | Nested property | `#request.strategyId` |
| `#param.nested.prop` | Deep nesting | `#request.target.id` |

---

### @AuthUser

Injects the authenticated user into a controller method parameter.

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
    boolean required() default true;  // Throw 401 if not authenticated?
}
```

#### Usage Examples

```java
// ═══════════════════════════════════════════════════════════════════════════
// Required (default) - Throws 401 if not authenticated
// ═══════════════════════════════════════════════════════════════════════════
@GetMapping("/me")
public ResponseEntity<UserProfile> getMyProfile(@AuthUser AuthenticatedUser user) {
    return ResponseEntity.ok(profileService.getProfile(user.getUserId()));
}


// ═══════════════════════════════════════════════════════════════════════════
// Optional - Returns null if not authenticated
// ═══════════════════════════════════════════════════════════════════════════
@GetMapping("/products")
public ResponseEntity<List<Product>> listProducts(
        @AuthUser(required = false) AuthenticatedUser user) {

    if (user != null) {
        // Return personalized results
        return ResponseEntity.ok(productService.getPersonalized(user.getUserId()));
    }
    // Return generic results
    return ResponseEntity.ok(productService.getPublic());
}


// ═══════════════════════════════════════════════════════════════════════════
// Combining Multiple Features
// ═══════════════════════════════════════════════════════════════════════════
@PostMapping
@RequireAuth
@RequireScope("portfolio:write")
public ResponseEntity<Portfolio> createPortfolio(
        @RequestBody CreatePortfolioRequest request,
        @AuthUser AuthenticatedUser user) {

    // Create portfolio owned by this user
    Portfolio portfolio = portfolioService.create(request, user.getUserId());

    // Grant FGA ownership
    fgaClient.write(
        "user:" + user.getUserId(),
        "owner",
        "portfolio:" + portfolio.getId()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
}
```

---

## AuthenticatedUser API

The `AuthenticatedUser` class provides a rich API for accessing token claims and performing authorization checks.

### Construction (via Filter)

```java
// Built automatically by PasetoAuthenticationFilter from PASETO claims:
AuthenticatedUser user = AuthenticatedUser.builder()
    .userId("usr_abc123")                           // from 'sub' claim
    .scopes(Set.of("portfolio:read", "trading:execute"))  // from 'scope' claim
    .acr("2")                                       // from 'acr' claim
    .amr(List.of("pwd", "totp"))                   // from 'amr' claim
    .demoMode(false)                               // from 'demoMode' claim
    .build();
```

### Available Methods

```java
// ═══════════════════════════════════════════════════════════════════════════
// Basic Accessors
// ═══════════════════════════════════════════════════════════════════════════
String userId = user.getUserId();           // "usr_abc123"
Set<String> scopes = user.getScopes();      // ["portfolio:read", "trading:execute"]
String acr = user.getAcr();                 // "2"
List<String> amr = user.getAmr();           // ["pwd", "totp"]
boolean demo = user.isDemoMode();           // false


// ═══════════════════════════════════════════════════════════════════════════
// Scope Checking
// ═══════════════════════════════════════════════════════════════════════════
boolean canRead = user.hasScope("portfolio:read");           // true
boolean canAdmin = user.hasScope("admin:users");             // false

boolean canReadOrWrite = user.hasAnyScope(                   // true
    "portfolio:read",
    "portfolio:write"
);

boolean hasFullAccess = user.hasAllScopes(                   // false
    "portfolio:read",
    "portfolio:write",
    "portfolio:delete"
);


// ═══════════════════════════════════════════════════════════════════════════
// ACR Level Checking
// ═══════════════════════════════════════════════════════════════════════════
boolean isMultiFactor = user.isMultiFactor();     // true (ACR >= 2)
boolean meetsLevel2 = user.meetsMinAcr("2");      // true
boolean meetsLevel3 = user.meetsMinAcr("3");      // false


// ═══════════════════════════════════════════════════════════════════════════
// Practical Examples
// ═══════════════════════════════════════════════════════════════════════════
// Conditional feature access
if (user.hasScope("trading:execute") && user.isMultiFactor()) {
    enableLiveTrading();
}

// Role-based UI customization
if (user.hasAnyScope("admin:read", "admin:write")) {
    showAdminPanel();
}

// Demo mode handling
if (user.isDemoMode()) {
    return mockTradingResponse();
}
```

---

## SecurityContextHolder

Access the current authenticated user from anywhere in your code (service layer, utilities, etc.).

```java
// ═══════════════════════════════════════════════════════════════════════════
// Get User (Throws if not authenticated)
// ═══════════════════════════════════════════════════════════════════════════
AuthenticatedUser user = SecurityContextHolder.requireAuthenticatedUser();
// Throws StrategizException(NOT_AUTHENTICATED) if no user


// ═══════════════════════════════════════════════════════════════════════════
// Get User ID (Throws if not authenticated)
// ═══════════════════════════════════════════════════════════════════════════
String userId = SecurityContextHolder.requireUserId();
// Throws StrategizException(NOT_AUTHENTICATED) if no user


// ═══════════════════════════════════════════════════════════════════════════
// Get User Optionally
// ═══════════════════════════════════════════════════════════════════════════
Optional<AuthenticatedUser> maybeUser = SecurityContextHolder.getAuthenticatedUser();
maybeUser.ifPresent(u -> log.info("Request from user: {}", u.getUserId()));


// ═══════════════════════════════════════════════════════════════════════════
// Check Authentication Status
// ═══════════════════════════════════════════════════════════════════════════
if (SecurityContextHolder.isAuthenticated()) {
    // User is logged in
}


// ═══════════════════════════════════════════════════════════════════════════
// Service Layer Example
// ═══════════════════════════════════════════════════════════════════════════
@Service
public class AuditService {

    public void logAction(String action, String resourceId) {
        String userId = SecurityContextHolder.getAuthenticatedUser()
            .map(AuthenticatedUser::getUserId)
            .orElse("anonymous");

        auditRepository.save(new AuditEntry(userId, action, resourceId, Instant.now()));
    }
}
```

---

## Fine-Grained Authorization (FGA)

### Concept: Relationship-Based Access Control

FGA uses **tuples** to represent relationships between users and resources:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Relationship Tuple                               │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│    user:usr_abc123  ──── owner ────▶  portfolio:port_xyz789              │
│         │                   │                    │                       │
│       Subject           Relation              Object                     │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Current Implementation: Mock Client

The framework includes an in-memory mock client for development:

```java
@Service
public class PortfolioService {

    @Autowired
    private FGAClient fgaClient;

    // ═══════════════════════════════════════════════════════════════════
    // Grant Access on Resource Creation
    // ═══════════════════════════════════════════════════════════════════
    public Portfolio create(CreatePortfolioRequest request, String userId) {
        Portfolio portfolio = portfolioRepository.save(
            Portfolio.builder()
                .name(request.getName())
                .createdBy(userId)
                .build()
        );

        // Grant owner relation
        fgaClient.write(
            "user:" + userId,
            "owner",
            "portfolio:" + portfolio.getId()
        );

        return portfolio;
    }


    // ═══════════════════════════════════════════════════════════════════
    // Share Access with Another User
    // ═══════════════════════════════════════════════════════════════════
    public void shareWithUser(String portfolioId, String targetUserId) {
        // Grant viewer relation
        fgaClient.write(
            "user:" + targetUserId,
            "viewer",
            "portfolio:" + portfolioId
        );
    }


    // ═══════════════════════════════════════════════════════════════════
    // Revoke Access on Deletion
    // ═══════════════════════════════════════════════════════════════════
    public void delete(String portfolioId, String userId) {
        portfolioRepository.deleteById(portfolioId);

        // Revoke owner relation
        fgaClient.delete(
            "user:" + userId,
            "owner",
            "portfolio:" + portfolioId
        );
    }


    // ═══════════════════════════════════════════════════════════════════
    // Manual Authorization Check
    // ═══════════════════════════════════════════════════════════════════
    public boolean canUserAccess(String userId, String portfolioId) {
        return fgaClient.check(
            "user:" + userId,
            "viewer",  // Owners inherit viewer
            "portfolio:" + portfolioId
        );
    }
}
```

### Future: Real OpenFGA Integration

When ready for production, add the `client-openfga` module:

```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>client-openfga</artifactId>
</dependency>
```

Configure:

```properties
strategiz.openfga.api-url=http://openfga:8080
strategiz.openfga.store-id=${OPENFGA_STORE_ID}
strategiz.openfga.authorization-model-id=${OPENFGA_MODEL_ID}
```

The `FGAClient` interface allows seamless swapping between mock and real implementations using `@ConditionalOnMissingBean`.

---

## Error Handling

All authorization failures throw `StrategizException` with `AuthorizationErrorDetails`.

### Error Types

| Error Code | HTTP | When Thrown |
|------------|------|-------------|
| `NOT_AUTHENTICATED` | 401 | No valid token present |
| `INVALID_TOKEN` | 401 | Token validation failed |
| `TOKEN_EXPIRED` | 401 | Token has expired |
| `INSUFFICIENT_PERMISSIONS` | 403 | Missing required scope |
| `ACCESS_DENIED` | 403 | FGA relationship check failed |
| `AUTH_LEVEL_REQUIRED` | 403 | ACR level too low |
| `DEMO_MODE_RESTRICTED` | 403 | Demo users blocked |

### Error Response Format

```json
{
  "error": {
    "code": "INSUFFICIENT_PERMISSIONS",
    "message": "You don't have permission for this action",
    "module": "authorization"
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/v1/portfolios/abc123"
}
```

### Security Considerations

> **Important:** Error messages are intentionally generic. Detailed information (required scopes, resource IDs, specific reasons) is **only logged server-side** to prevent information disclosure attacks.

```java
// What the USER sees:
"You don't have permission for this action"

// What gets LOGGED server-side:
"Authorization failed: user=usr_123 missing scopes=[trading:execute] (has=[portfolio:read], mode=ALL)"
```

---

## Configuration

### Properties Reference

```properties
# ═══════════════════════════════════════════════════════════════════════════
# Core Settings
# ═══════════════════════════════════════════════════════════════════════════

# Enable/disable the entire framework (default: true)
strategiz.authorization.enabled=true

# Paths to skip authentication (Ant-style patterns)
# These are ADDED to the defaults, not replacing them
strategiz.authorization.skip-paths=/webhooks/**,/public/**


# ═══════════════════════════════════════════════════════════════════════════
# Token Configuration (Required)
# ═══════════════════════════════════════════════════════════════════════════

# PASETO signing keys (base64 encoded, 32 bytes each)
# Typically loaded from Vault in production
auth.token.session-key=${SESSION_KEY}
auth.token.identity-key=${IDENTITY_KEY}
```

### Default Skip Paths

The following paths are skipped by default (no authentication required):

```
/actuator/**        - Health checks, metrics
/v3/api-docs/**     - OpenAPI documentation
/swagger-ui/**      - Swagger UI
/v1/auth/**         - Authentication endpoints
/health             - Health endpoint
/info               - Info endpoint
```

---

## Migration Guide

### Before: Manual Token Handling

```java
@PostMapping
public ResponseEntity<?> createProvider(
        @RequestBody CreateProviderRequest request,
        @RequestHeader("Authorization") String authHeader) {

    // Manual token extraction
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new StrategizException(AuthErrors.INVALID_TOKEN);
    }

    String token = authHeader.substring(7);

    // Manual validation
    var validation = sessionAuthBusiness.validateToken(token);
    if (validation.isEmpty()) {
        throw new StrategizException(AuthErrors.INVALID_TOKEN);
    }

    // Manual user extraction
    String userId = validation.get().getUserId();

    // Business logic...
    return providerService.create(request, userId);
}
```

### After: Using Annotations

```java
@PostMapping
@RequireAuth
@RequireScope("provider:write")
public ResponseEntity<?> createProvider(
        @RequestBody CreateProviderRequest request,
        @AuthUser AuthenticatedUser user) {

    // userId automatically available and validated
    return providerService.create(request, user.getUserId());
}
```

### Migration Checklist

- [ ] Remove manual `Authorization` header parsing
- [ ] Remove manual token validation calls
- [ ] Add `@RequireAuth` to endpoints requiring authentication
- [ ] Add `@RequireScope` for scope-based access control
- [ ] Add `@Authorize` for resource-level access control
- [ ] Replace manual user extraction with `@AuthUser`
- [ ] Update error handling (framework throws `StrategizException`)

---

## Testing

### Unit Testing with Mock Security Context

```java
class PortfolioServiceTest {

    @BeforeEach
    void setUp() {
        // Create test user
        AuthenticatedUser testUser = AuthenticatedUser.builder()
            .userId("test-user-123")
            .scopes(Set.of("portfolio:read", "portfolio:write"))
            .acr("2")
            .amr(List.of("pwd", "totp"))
            .demoMode(false)
            .build();

        // Set in security context
        SecurityContext context = new SecurityContext();
        context.setAuthenticatedUser(testUser);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetUserIdFromContext() {
        String userId = SecurityContextHolder.requireUserId();
        assertThat(userId).isEqualTo("test-user-123");
    }

    @Test
    void shouldCheckScopes() {
        AuthenticatedUser user = SecurityContextHolder.requireAuthenticatedUser();

        assertThat(user.hasScope("portfolio:read")).isTrue();
        assertThat(user.hasScope("admin:users")).isFalse();
        assertThat(user.hasAllScopes("portfolio:read", "portfolio:write")).isTrue();
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReject_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/v1/portfolios/123"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @WithMockPasetoUser(scopes = {"portfolio:read"})  // Custom annotation
    void shouldAllow_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/v1/portfolios/123"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockPasetoUser(scopes = {"other:scope"})
    void shouldReject_WhenMissingScope() throws Exception {
        mockMvc.perform(get("/v1/portfolios/123"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"));
    }
}
```

---

## Reference Tables

### Resource Types

| ResourceType | FGA Object Prefix | Description |
|--------------|-------------------|-------------|
| `PORTFOLIO` | `portfolio:` | User investment portfolio |
| `STRATEGY` | `strategy:` | Trading strategy |
| `WATCHLIST` | `watchlist:` | Stock watchlist |
| `PROVIDER_INTEGRATION` | `provider:` | Connected broker/exchange |
| `ALERT` | `alert:` | Price/news alert |
| `PROFILE` | `profile:` | User profile |

### Common Scopes

| Scope | Description | Typical Endpoints |
|-------|-------------|-------------------|
| `portfolio:read` | View portfolio data | GET /portfolios |
| `portfolio:write` | Modify portfolios | POST/PUT /portfolios |
| `strategy:read` | View strategies | GET /strategies |
| `strategy:write` | Create/edit strategies | POST/PUT /strategies |
| `strategy:execute` | Run strategies | POST /strategies/execute |
| `trading:execute` | Execute live trades | POST /orders |
| `provider:read` | View connected providers | GET /providers |
| `provider:write` | Connect/disconnect | POST/DELETE /providers |
| `watchlist:read` | View watchlists | GET /watchlists |
| `watchlist:write` | Modify watchlists | POST/PUT /watchlists |
| `alerts:read` | View alerts | GET /alerts |
| `alerts:write` | Manage alerts | POST/PUT/DELETE /alerts |
| `profile:read` | View profile | GET /profile |
| `profile:write` | Update profile | PUT /profile |

### FGA Relations

| Relation | Description | Inherits |
|----------|-------------|----------|
| `owner` | Full control over resource | `editor`, `viewer` |
| `editor` | Can modify resource | `viewer` |
| `viewer` | Can read resource | - |

---

## License

Part of the Strategiz Platform. All rights reserved.
