# Client - SonarQube

HTTP client for fetching code quality metrics from self-hosted SonarQube instance.

## Purpose

Integrates with SonarQube REST API to retrieve static analysis metrics:
- Bugs (logic errors)
- Vulnerabilities (security issues)
- Code smells (maintainability issues)
- Test coverage percentage
- Code duplication percentage
- Technical debt (estimated fix time)
- Reliability rating (A-E scale)

## Configuration

### Vault Configuration (Production)

Store SonarQube credentials in Vault:

```bash
# Vault path: secret/strategiz/sonarqube
vault kv put secret/strategiz/sonarqube \
  url="https://strategiz-sonarqube-<hash>.run.app" \
  token="<your-sonarqube-api-token>" \
  project-key="strategiz-io_strategiz-core"
```

### Application Properties (Development Fallback)

Add to `application-dev.properties`:

```properties
sonarqube.url=http://localhost:9000
sonarqube.token=<dev-token>
sonarqube.project-key=strategiz-io_strategiz-core
```

## SonarQube API Token

Generate token in SonarQube web UI:
1. Log in to SonarQube
2. Go to **My Account** → **Security**
3. Generate new token (name: `strategiz-api`)
4. Copy token and store in Vault

## Usage

```java
@Autowired
private SonarQubeClient sonarQubeClient;

public void getMetrics() {
    SonarQubeMetrics metrics = sonarQubeClient.getProjectMetrics();

    System.out.println("Bugs: " + metrics.getBugs());
    System.out.println("Vulnerabilities: " + metrics.getVulnerabilities());
    System.out.println("Code Smells: " + metrics.getCodeSmells());
    System.out.println("Coverage: " + metrics.getCoverage() + "%");
    System.out.println("Technical Debt: " + metrics.getTechnicalDebt());
    System.out.println("Rating: " + metrics.getRating());
}
```

## SonarQube API Endpoints

This client calls the following SonarQube API endpoints:

### GET /api/measures/component
Fetches metrics for a specific project component.

**Query Parameters:**
- `component` - Project key (e.g., `strategiz-io_strategiz-core`)
- `metricKeys` - Comma-separated list of metrics to fetch

**Metrics requested:**
- `bugs` - Number of bugs
- `vulnerabilities` - Number of vulnerabilities
- `code_smells` - Number of code smells
- `coverage` - Test coverage percentage
- `duplicated_lines_density` - Percentage of duplicated lines
- `sqale_index` - Technical debt in minutes
- `reliability_rating` - Reliability rating (1-5 → A-E)

## Authentication

SonarQube uses **Basic Authentication** with token:
- Username: API token
- Password: (empty)

Example:
```
Authorization: Basic <base64(token:)>
```

## Error Handling

If SonarQube is unavailable, returns empty metrics:
```java
SonarQubeMetrics(
    bugs=0,
    vulnerabilities=0,
    codeSmells=0,
    coverage=0.0,
    duplications=0.0,
    technicalDebt="0m",
    rating="N/A"
)
```

## Testing

```bash
# Run tests
mvn test -pl client-sonarqube

# Test SonarQube connection
curl -u <token>: http://localhost:9000/api/system/health
```

## Dependencies

- **framework-secrets** - Vault integration for credentials
- **spring-boot-starter-web** - RestTemplate for HTTP calls
- **jackson-databind** - JSON parsing
