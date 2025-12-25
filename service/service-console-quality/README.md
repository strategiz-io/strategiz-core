# Service - Console Quality

Quality metrics and compliance scanning for the admin console dashboard.

## Purpose

This module provides REST endpoints for the Quality Dashboard at `console.strategiz.io/quality`, exposing:
- Framework compliance metrics (exception handling, BaseService adoption, etc.)
- SonarQube integration (bugs, vulnerabilities, code smells, technical debt)
- Codebase violation tracking with file paths and line numbers
- Quality grade calculation and trending

## Architecture

```
Console Frontend (console.strategiz.io/quality)
    ↓
service-console-quality (this module)
    ↓
client-sonarqube (SonarQube API integration)
```

## REST Endpoints

All endpoints require admin authentication (`@RequireAuth` with ADMIN role check).

### GET /v1/console/quality/overview
Returns overall quality summary:
```json
{
  "overallGrade": "A-",
  "complianceScore": 94.3,
  "sonarQubeRating": "A",
  "bugs": 3,
  "vulnerabilities": 0,
  "codeSmells": 42,
  "technicalDebt": "4h 30m",
  "lastUpdated": "2025-12-24T10:30:00Z"
}
```

### GET /v1/console/quality/compliance
Returns framework compliance breakdown:
```json
{
  "exceptionHandling": {
    "compliance": 88.0,
    "violations": 90,
    "total": 748,
    "grade": "B+"
  },
  "servicePattern": {
    "compliance": 100.0,
    "violations": 0,
    "total": 96,
    "grade": "A+"
  },
  "controllerPattern": {
    "compliance": 95.0,
    "violations": 4,
    "total": 80,
    "grade": "A"
  }
}
```

### GET /v1/console/quality/sonarqube
Proxies SonarQube metrics:
```json
{
  "bugs": 3,
  "vulnerabilities": 0,
  "codeSmells": 42,
  "coverage": 67.5,
  "duplications": 2.3,
  "technicalDebt": "4h 30m",
  "rating": "A"
}
```

### GET /v1/console/quality/violations
Returns top violations with file paths:
```json
{
  "violations": [
    {
      "type": "EXCEPTION_HANDLING",
      "file": "service/service-labs/CreateStrategyService.java",
      "line": 123,
      "message": "Raw IllegalArgumentException - use StrategizException",
      "severity": "MEDIUM"
    }
  ],
  "total": 90
}
```

## Key Components

### ComplianceScanner
Scans codebase for framework compliance violations:
- Exception handling (StrategizException usage)
- Service pattern (BaseService extension)
- Controller pattern (BaseController extension)
- Returns file paths and line numbers for violations

### QualityMetricsService
Aggregates metrics from multiple sources:
- ComplianceScanner for framework compliance
- SonarQubeClient for static analysis metrics
- Calculates overall quality grade (A-F scale)

### AdminQualityController
REST controller exposing quality endpoints:
- Requires admin authentication
- Returns JSON responses
- Integrates with both ComplianceScanner and SonarQubeClient

## Dependencies

- **service-framework-base**: BaseController, authentication, exception handling
- **client-sonarqube**: SonarQube API integration
- **Spring Boot Web**: REST endpoints
- **Spring Validation**: Request/response validation

## Usage

Add dependency in `application-api/pom.xml`:
```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>service-console-quality</artifactId>
    <version>${project.version}</version>
</dependency>
```

Endpoints will be automatically registered at `/v1/console/quality/*`.

## Testing

```bash
# Run tests
mvn test -pl service-console-quality

# Test compliance scanning
curl http://localhost:8080/v1/console/quality/compliance

# Test SonarQube integration
curl http://localhost:8080/v1/console/quality/sonarqube
```

## Future Enhancements

- Historical trending (store metrics over time)
- Violation auto-fixing suggestions
- Custom compliance rules
- Email alerts for quality regressions
