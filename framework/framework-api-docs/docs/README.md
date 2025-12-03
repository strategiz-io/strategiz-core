<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/OpenAPI-3.0-85EA2D?style=for-the-badge&logo=openapiinitiative" alt="OpenAPI"/>
</p>

# API Documentation Framework

> **OpenAPI/Swagger configuration and documentation infrastructure for Strategiz Core**

---

## Overview

This module provides centralized OpenAPI (Swagger) configuration for all REST endpoints in the Strategiz platform.

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Endpoints                                                         │
│   ═════════                                                         │
│                                                                     │
│   /v3/api-docs          OpenAPI 3.0 JSON specification              │
│   /v3/api-docs.yaml     OpenAPI 3.0 YAML specification              │
│   /swagger-ui.html      Interactive Swagger UI                      │
│   /swagger-ui/**        Swagger UI static resources                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-api-docs</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Configuration

Add to `application.properties`:

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

---

## Annotating Controllers

### Controller-Level Documentation

```java
@RestController
@Tag(name = "Portfolios", description = "Portfolio management endpoints")
@RequestMapping("/v1/portfolios")
public class PortfolioController {
    // ...
}
```

### Operation-Level Documentation

```java
@Operation(
    summary = "Get portfolio by ID",
    description = "Retrieves a specific portfolio with all holdings and performance metrics"
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Portfolio found",
        content = @Content(schema = @Schema(implementation = Portfolio.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Portfolio not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
})
@GetMapping("/{id}")
public ResponseEntity<Portfolio> getPortfolio(@PathVariable String id) {
    // ...
}
```

### Parameter Documentation

```java
@Operation(summary = "List portfolios with pagination")
@GetMapping
public ResponseEntity<List<Portfolio>> listPortfolios(
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field", example = "createdAt")
        @RequestParam(defaultValue = "createdAt") String sort) {
    // ...
}
```

### Schema Documentation

```java
@Schema(description = "User portfolio containing holdings and performance data")
public class Portfolio {

    @Schema(description = "Unique portfolio identifier", example = "port_abc123")
    private String id;

    @Schema(description = "Portfolio display name", example = "My Growth Portfolio")
    private String name;

    @Schema(description = "Total portfolio value in USD", example = "125000.50")
    private BigDecimal totalValue;

    @Schema(description = "Portfolio creation timestamp")
    private Instant createdAt;
}
```

---

## Security Documentation

### Documenting Authenticated Endpoints

```java
@Operation(
    summary = "Create new portfolio",
    security = @SecurityRequirement(name = "bearerAuth")
)
@PostMapping
@RequireAuth
@RequireScope("portfolio:write")
public ResponseEntity<Portfolio> createPortfolio(@RequestBody CreatePortfolioRequest request) {
    // ...
}
```

### Global Security Configuration

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Strategiz API")
                .version("1.0")
                .description("Trading platform API"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("PASETO")));
    }
}
```

---

## Grouping APIs

### By Module

```java
@Bean
public GroupedOpenApi portfolioApi() {
    return GroupedOpenApi.builder()
        .group("portfolio")
        .pathsToMatch("/v1/portfolios/**")
        .build();
}

@Bean
public GroupedOpenApi authApi() {
    return GroupedOpenApi.builder()
        .group("auth")
        .pathsToMatch("/v1/auth/**")
        .build();
}
```

---

## Accessing Documentation

| Environment | URL |
|-------------|-----|
| Local | http://localhost:8080/swagger-ui.html |
| Local HTTPS | https://localhost:8443/swagger-ui.html |
| Production | https://api.strategiz.io/swagger-ui.html |

---

## Best Practices

### 1. Use Meaningful Tags

```java
@Tag(name = "Authentication", description = "User authentication and session management")
```

### 2. Provide Examples

```java
@Schema(example = "john.doe@example.com")
private String email;
```

### 3. Document Error Responses

```java
@ApiResponse(responseCode = "400", description = "Invalid input")
@ApiResponse(responseCode = "401", description = "Authentication required")
@ApiResponse(responseCode = "403", description = "Insufficient permissions")
```

### 4. Use Consistent Naming

- Tags: PascalCase (`Portfolios`, `Authentication`)
- Operations: Short, action-oriented (`Get portfolio`, `Create user`)
- Parameters: camelCase with clear descriptions

---

## Dependencies

- SpringDoc OpenAPI Starter WebMVC UI
- Spring Boot Starter Web

---

## Related Documentation

- [Authorization Framework](../../framework-authorization/docs/README.md)
- [Exception Framework](../../framework-exception/docs/README.md)
- [Framework Documentation Hub](../../docs/README.md)
