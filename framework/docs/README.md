<p align="center">
  <img src="https://img.shields.io/badge/Strategiz-Framework-0A66C2?style=for-the-badge" alt="Strategiz Framework"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21"/>
</p>

<h1 align="center">Strategiz Framework Documentation</h1>

<p align="center">
  <strong>Cross-cutting concerns and shared infrastructure for the Strategiz Platform</strong>
</p>

---

## Framework Modules at a Glance

| Module | Purpose | Status | Documentation |
|--------|---------|:------:|---------------|
| [**Authorization**](#-authorization) | Two-layer auth with PASETO + OpenFGA | ![Ready](https://img.shields.io/badge/-Ready-success) | [Full Docs](../framework-authorization/docs/README.md) |
| [**Exception**](#-exception-handling) | Layered exception framework | ![Ready](https://img.shields.io/badge/-Ready-success) | [Full Docs](../framework-exception/docs/README.md) |
| [**Logging**](#-logging) | Structured JSON logging | ![Ready](https://img.shields.io/badge/-Ready-success) | [Full Docs](../framework-logging/docs/README.md) |
| [**Secrets**](#-secrets-management) | HashiCorp Vault integration | ![Ready](https://img.shields.io/badge/-Ready-success) | [Full Docs](../framework-secrets/docs/README.md) |
| [**API Docs**](#-api-documentation) | OpenAPI/Swagger config | ![Ready](https://img.shields.io/badge/-Ready-success) | [Full Docs](../framework-api-docs/docs/README.md) |

---

## Quick Navigation

```
framework/
├── framework-authorization/     # Authentication & Authorization
├── framework-exception/         # Exception Handling
├── framework-logging/           # Structured Logging
├── framework-secrets/           # Vault Integration
└── framework-api-docs/          # OpenAPI Configuration
```

---

<h2 id="authorization">
  <img src="https://img.shields.io/badge/-Authorization-purple?style=flat-square" alt="Authorization"/>
</h2>

### Two-Layer Security Model

```
┌─────────────────────────────────────────────────────────────────────┐
│  Layer 1: SCOPE-BASED                    Layer 2: FGA              │
│  ════════════════════                    ═══════════               │
│                                                                     │
│  "Can user perform this                  "Can user access THIS     │
│   TYPE of action?"                        SPECIFIC resource?"      │
│                                                                     │
│  Fast, local check                       Relationship-based        │
│  PASETO token scopes                     user → relation → resource│
│                                                                     │
│  Example: portfolio:read                 Example: user:123 → owner │
│                                                   → portfolio:abc  │
└─────────────────────────────────────────────────────────────────────┘
```

### Core Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@RequireAuth` | Authentication + ACR level | `@RequireAuth(minAcr = "2")` |
| `@RequireScope` | Token scope validation | `@RequireScope("portfolio:read")` |
| `@Authorize` | FGA relationship check | `@Authorize(relation = "owner", ...)` |
| `@AuthUser` | Inject authenticated user | `@AuthUser AuthenticatedUser user` |

### Quick Example

```java
@GetMapping("/{id}")
@RequireAuth                              // Must be authenticated
@RequireScope("portfolio:read")           // Must have scope
@Authorize(                               // Must be owner/viewer
    relation = "viewer",
    resource = ResourceType.PORTFOLIO,
    resourceId = "#id"
)
public ResponseEntity<Portfolio> getPortfolio(
        @PathVariable String id,
        @AuthUser AuthenticatedUser user) {
    return ResponseEntity.ok(service.get(id));
}
```

### ACR Levels

| Level | Name | Description |
|:-----:|------|-------------|
| `0` | None | Signup in progress |
| `1` | Single-Factor | Password only |
| `2` | Multi-Factor | Password + TOTP/SMS |
| `3` | Strong MFA | Hardware key + factor |

**[Full Documentation](../framework-authorization/docs/README.md)**

---

<h2 id="exception">
  <img src="https://img.shields.io/badge/-Exception-red?style=flat-square" alt="Exception"/>
</h2>

### Layered Exception Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Service Layer              Business/Data/Client Layers            │
│   ═════════════              ══════════════════════════             │
│                                                                     │
│   ErrorDetails interface     Simple enums (HTTP-agnostic)           │
│   with HTTP status codes                                            │
│                                                                     │
│   ServiceAuthErrorDetails    AuthBusinessErrors                     │
│   ServiceProviderErrorDetails CoinbaseErrors                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Standard Error Response

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid username or password",
  "developer": "Authentication failed in module: service-auth",
  "moreInfo": "https://docs.strategiz.io/errors/auth/invalid-credentials"
}
```

### Quick Example

```java
// Service modules (with HTTP status)
public enum ServiceAuthErrorDetails implements ErrorDetails {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "session-expired");
    // ...
}

throw new StrategizException(ServiceAuthErrorDetails.INVALID_CREDENTIALS, "service-auth");

// Other modules (HTTP-agnostic)
throw new StrategizException(CoinbaseErrors.API_CONNECTION_FAILED, "client-coinbase");
```

### HTTP Status Philosophy

| Status | Meaning |
|:------:|---------|
| `200` | Success (GET, PUT, DELETE) |
| `201` | Created (POST) |
| `400` | Client provided bad input |
| `401` | Authentication required/failed |
| `500` | Server failed |

**[Full Documentation](../framework-exception/docs/README.md)**

---

<h2 id="logging">
  <img src="https://img.shields.io/badge/-Logging-blue?style=flat-square" alt="Logging"/>
</h2>

### Structured JSON Logging

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Features                                                          │
│   ════════                                                          │
│                                                                     │
│   • Structured JSON format for production                           │
│   • Request correlation (X-Correlation-ID)                          │
│   • Performance monitoring & slow query detection                   │
│   • Security event logging with data masking                        │
│   • Thread-local context management                                 │
│   • Zero-config Spring Boot integration                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Quick Example

```java
// Structured logging
StructuredLogger.info()
    .operation("user_signup")
    .userId("usr_123")
    .email("user@example.com")
    .log("User signup completed");

// Performance logging
StructuredLogger.performance()
    .operation("database_query")
    .duration(150)
    .log("Query executed");

// Security logging (with masking)
StructuredLogger.security()
    .operation("login_attempt")
    .userId("usr_123")
    .log("Suspicious activity detected");
```

### Log Output (Production)

```json
{
  "@timestamp": "2024-01-15T14:30:45.123Z",
  "level": "INFO",
  "message": "User signup completed",
  "requestId": "req-abc123",
  "correlationId": "corr-def456",
  "userId": "usr_456",
  "operation": "user_signup",
  "email": "u***r@example.com",
  "durationMs": 150
}
```

**[Full Documentation](../framework-logging/docs/README.md)**

---

<h2 id="secrets">
  <img src="https://img.shields.io/badge/-Secrets-yellow?style=flat-square" alt="Secrets"/>
</h2>

### HashiCorp Vault Integration

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Vault KV Store                                                    │
│   ══════════════                                                    │
│                                                                     │
│   secret/strategiz/                                                 │
│   ├── oauth/                                                        │
│   │   ├── google          (client-id, client-secret)                │
│   │   ├── facebook        (client-id, client-secret)                │
│   │   ├── coinbase        (client-id, client-secret)                │
│   │   ├── alpaca          (client-id, client-secret, api-url)       │
│   │   └── schwab          (client-id, client-secret)                │
│   ├── tokens/                                                       │
│   │   ├── dev             (session-key, identity-key)               │
│   │   └── prod            (session-key, identity-key)               │
│   └── gemini/                                                       │
│       └── api-key                                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
# Vault Connection
strategiz.vault.enabled=true
strategiz.vault.address=${VAULT_ADDR:http://localhost:8200}
strategiz.vault.secrets-path=secret

# Spring Cloud Vault
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.uri=${VAULT_ADDR:http://localhost:8200}
spring.cloud.vault.token=${VAULT_TOKEN:}
```

### Quick Example

```java
@Service
public class OAuthVaultConfig {

    @Value("${oauth.providers.google.client-id}")
    private String googleClientId;  // Loaded from Vault

    @Value("${oauth.providers.google.client-secret}")
    private String googleClientSecret;  // Loaded from Vault
}
```

### Local Development

```bash
# Start Vault in dev mode
vault server -dev

# Set environment variables
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root

# Add secrets
vault kv put secret/strategiz/oauth/google \
  client-id="your-id" \
  client-secret="your-secret"
```

---

<h2 id="api-docs">
  <img src="https://img.shields.io/badge/-API%20Docs-green?style=flat-square" alt="API Docs"/>
</h2>

### OpenAPI/Swagger Configuration

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Endpoints                                                         │
│   ═════════                                                         │
│                                                                     │
│   /v3/api-docs          - OpenAPI JSON specification                │
│   /swagger-ui.html      - Swagger UI interface                      │
│   /swagger-ui/**        - Swagger UI resources                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
# SpringDoc OpenAPI Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=alpha
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.display-request-duration=true
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.packagesToScan=io.strategiz
springdoc.pathsToMatch=/v1/**
```

### Quick Example

```java
@RestController
@Tag(name = "Portfolios", description = "Portfolio management endpoints")
@RequestMapping("/v1/portfolios")
public class PortfolioController {

    @Operation(
        summary = "Get portfolio by ID",
        description = "Retrieves a specific portfolio with all holdings"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Portfolio found"),
        @ApiResponse(responseCode = "404", description = "Portfolio not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String id) {
        // ...
    }
}
```

---

## Module Dependencies

```
                    ┌─────────────────────┐
                    │   framework-api-docs │
                    └─────────────────────┘
                              │
                              ▼
┌─────────────────┐   ┌─────────────────────┐   ┌─────────────────┐
│ framework-logging│◄──│ framework-exception │◄──│ framework-secrets│
└─────────────────┘   └─────────────────────┘   └─────────────────┘
                              │
                              ▼
                    ┌─────────────────────────┐
                    │ framework-authorization │
                    │  (PASETO + OpenFGA)     │
                    └─────────────────────────┘
```

---

## Adding to Your Module

```xml
<!-- Authorization Framework -->
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-authorization</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Exception Framework -->
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-exception</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Logging Framework -->
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-logging</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Secrets Framework -->
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-secrets</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- API Documentation -->
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-api-docs</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## Best Practices

### 1. Authorization

```java
// Always use @RequireAuth before @RequireScope
@RequireAuth(minAcr = "2")
@RequireScope("trading:execute")
@Authorize(relation = "owner", resource = ResourceType.PORTFOLIO, resourceId = "#id")
```

### 2. Exception Handling

```java
// Service layer: Use ErrorDetails with HTTP status
throw new StrategizException(ServiceAuthErrorDetails.INVALID_CREDENTIALS, "service-auth");

// Business layer: Use simple enums (HTTP-agnostic)
throw new StrategizException(AuthBusinessErrors.PASSWORD_MISMATCH, "business-auth");
```

### 3. Logging

```java
// Use structured logging over plain log statements
StructuredLogger.info()
    .operation("order_placed")
    .userId(user.getUserId())
    .amount(order.getTotal())
    .log("Order completed successfully");
```

### 4. Secrets

```java
// Never hardcode secrets - always use Vault
@Value("${oauth.providers.google.client-secret}")
private String clientSecret;  // Loaded from Vault at runtime
```

---

## Troubleshooting

### Authorization Issues

```bash
# Check if token is being extracted
logging.level.io.strategiz.framework.authorization=DEBUG
```

### Vault Connection Issues

```bash
# Verify Vault is running
vault status

# Check token validity
vault token lookup

# Test secret access
vault kv get secret/strategiz/oauth/google
```

### Logging Not Working

```yaml
# Enable framework logging
logging:
  level:
    io.strategiz.framework.logging: DEBUG
```

---

## Contributing

When adding new framework modules:

1. Follow the existing naming convention: `framework-{name}`
2. Create `docs/README.md` with comprehensive documentation
3. Add to the module table in this document
4. Include auto-configuration via `spring.factories`

---

<p align="center">
  <strong>Strategiz Platform</strong><br/>
  <em>All rights reserved</em>
</p>
