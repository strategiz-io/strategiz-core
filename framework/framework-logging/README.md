# Framework Logging

Centralized logging infrastructure for Strategiz applications with structured JSON logging, request correlation, and performance monitoring.

## Features

- **Structured JSON Logging**: Consistent, machine-readable log format
- **Request Correlation**: Automatic request tracking across service boundaries
- **Performance Monitoring**: Built-in request timing and slow query detection
- **Context Management**: Thread-local logging context for request-scoped data
- **Fluent API**: Clean, builder-pattern API for structured logging
- **Security Logging**: Specialized security event logging with data masking
- **Auto-Configuration**: Zero-configuration Spring Boot integration

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-logging</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Auto-Configuration

The module auto-configures when added to the classpath. No additional setup required.

## Usage

### Structured Logging

```java
import io.strategiz.framework.logging.StructuredLogger;

// Simple structured logging
StructuredLogger.info()
    .operation("user_signup")
    .userId("usr_123")
    .email("user@example.com")
    .log("User signup completed");

// Performance logging
StructuredLogger.performance()
    .operation("database_query")
    .duration(150)
    .table("users")
    .log("Database query executed");

// Error logging with context
StructuredLogger.error()
    .operation("payment_processing")
    .userId("usr_123")
    .amount(99.99)
    .errorCode("PAYMENT_FAILED")
    .log("Payment processing failed", exception);
```

### Logging Context

```java
import io.strategiz.framework.logging.LoggingContext;

// Set context values
LoggingContext.setUserId("usr_123");
LoggingContext.setOperation("user_registration");
LoggingContext.put("customField", "value");

// Context is automatically included in all log entries
log.info("This will include userId and operation in the log");

// Context is automatically cleared at request end
```

### Specialized Loggers

```java
// Business event logging
StructuredLogger.business()
    .operation("order_placed")
    .userId("usr_123")
    .amount(199.99)
    .log("Order placed successfully");

// Security event logging
StructuredLogger.security()
    .operation("login_attempt")
    .userId("usr_123")
    .field("ipAddress", "192.168.1.1")
    .log("Suspicious login attempt detected");

// Audit logging
StructuredLogger.audit()
    .operation("user_data_access")
    .userId("usr_123")
    .field("resource", "sensitive_data")
    .log("User accessed sensitive data");
```

## Configuration

Configure logging behavior through application properties:

```yaml
strategiz:
  logging:
    enabled: true
    performance:
      enabled: true
      slow-request-threshold: 2000
      log-database-queries: true
      log-external-calls: true
    security:
      enabled: true
      mask-sensitive-data: true
      log-auth-events: true
      log-authz-events: true
    correlation:
      enabled: true
      header-name: X-Correlation-ID
      request-id-header-name: X-Request-ID
      generate-if-missing: true
```

## Log Format

### Development (Console)
```
2024-01-15 14:30:45.123 [http-nio-8080-exec-1] INFO  i.s.s.a.c.SignupController [req-abc123] [usr_456] - User signup completed
```

### Production (JSON)
```json
{
  "@timestamp": "2024-01-15T14:30:45.123Z",
  "level": "INFO",
  "logger": "io.strategiz.service.auth.controller.SignupController",
  "message": "User signup completed",
  "requestId": "req-abc123",
  "correlationId": "corr-def456",
  "userId": "usr_456",
  "operation": "user_signup",
  "email": "u***r@example.com",
  "durationMs": 150,
  "application": "strategiz-core",
  "environment": "production"
}
```

## Request Correlation

The framework automatically:

1. Extracts correlation IDs from headers (`X-Correlation-ID`, `X-Request-ID`)
2. Generates new IDs if not present
3. Includes IDs in all log entries
4. Adds IDs to MDC for framework integration

## Performance Monitoring

Automatic performance logging includes:

- HTTP request timing
- Slow request detection (configurable threshold)
- Database query performance
- External API call timing

## Security Features

- **Data Masking**: Automatically masks sensitive data (emails, IDs)
- **Security Events**: Specialized logging for authentication/authorization
- **Audit Trail**: Comprehensive audit logging for compliance
- **Context Isolation**: Thread-local context prevents data leakage

## Integration

### With Spring Boot

The logging filter runs at highest precedence to ensure context is available for all subsequent filters and interceptors.

### With Logback

The module includes optimized Logback configuration with:
- JSON structured logging
- Async appenders for performance
- Log rotation and retention policies
- Separate appenders for different log types

### With Monitoring

Logs are structured for easy integration with:
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Grafana/Prometheus
- Cloud logging services (CloudWatch, Stackdriver)

## Best Practices

1. **Use Structured Logging**: Prefer `StructuredLogger` over plain `log` statements
2. **Set Context Early**: Set user/operation context as early as possible
3. **Use Standard Fields**: Use provided field methods for consistency
4. **Mask Sensitive Data**: Use `.email()` method for automatic masking
5. **Performance Logging**: Use `.performance()` for timing-critical operations
6. **Error Context**: Include relevant context in error logs

## Thread Safety

- `LoggingContext` uses `ThreadLocal` storage
- `StructuredLogger` is stateless and thread-safe
- MDC integration is thread-safe

## Dependencies

- Spring Boot Starter
- Logback Classic
- Logstash Logback Encoder
- SLF4J API
- Jackson (for JSON processing) 