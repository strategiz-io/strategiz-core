# Quality Monitoring System

This document describes the Strategiz quality monitoring system, which provides real-time code quality metrics and compliance tracking through the admin console at `console.strategiz.io/quality`.

## Architecture Overview

The quality monitoring system uses a **hybrid approach** combining:

1. **Real-time Compliance Scanning** - Framework pattern enforcement (exception handling, service patterns, controller patterns)
2. **Build-Time Static Analysis** - Code quality analysis using free, open-source tools
3. **Cached Metrics Storage** - Firestore-backed cache with 24-hour freshness

```
┌─────────────────────┐
│  GitHub Actions     │
│  (on push to main)  │
└──────────┬──────────┘
           │
           ├─ SpotBugs (bugs)
           ├─ PMD (code smells)
           ├─ Checkstyle (style)
           └─ JaCoCo (coverage)
           │
           ▼
┌─────────────────────┐
│  Python Aggregator  │
│  (parse XML reports)│
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  POST /cache        │
│  (save to Firestore)│
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Admin Console      │
│  (read from cache)  │
└─────────────────────┘
```

## Components

### 1. Maven Analysis Plugins

Four free analysis tools configured in `pom.xml`:

#### SpotBugs
- **Purpose**: Bug detection and security vulnerability scanning
- **Version**: 4.8.6.4
- **Output**: `target/spotbugs/*.xml`
- **Detects**: Null pointer dereferences, resource leaks, security vulnerabilities

#### PMD
- **Purpose**: Code quality analysis and code smell detection
- **Version**: 3.25.0
- **Output**: `target/pmd/pmd.xml`, `target/pmd/cpd.xml`
- **Detects**: Unused variables, overcomplicated expressions, duplicate code

#### Checkstyle
- **Purpose**: Code style compliance (Google Java Style Guide)
- **Version**: 3.5.0
- **Output**: `target/checkstyle/checkstyle-result.xml`
- **Detects**: Formatting violations, naming conventions, code organization

#### JaCoCo
- **Purpose**: Test coverage analysis
- **Version**: 0.8.12
- **Output**: `target/site/jacoco/jacoco.xml`
- **Measures**: Line coverage, branch coverage, method coverage

### 2. Python Aggregator Script

**Location**: `scripts/aggregate-quality-metrics.py`

**Purpose**: Parse XML reports from all tools and aggregate into unified JSON format

**Input**:
- SpotBugs XML report
- PMD XML report
- Checkstyle XML report
- JaCoCo XML report

**Output**: JSON payload matching `CachedQualityMetrics` schema

**Key Functions**:
- `parse_spotbugs_report()` - Extract bugs and vulnerabilities
- `parse_pmd_report()` - Extract code smells
- `parse_checkstyle_report()` - Extract style violations
- `parse_jacoco_report()` - Extract coverage percentage
- `calculate_technical_debt()` - Estimate fix time in hours/days
- `calculate_reliability_rating()` - SonarQube-style A-E rating
- `calculate_quality_gate_status()` - Pass/fail determination

**Quality Gate Criteria**:
```python
- No critical vulnerabilities
- Bugs < 50
- Code smells < 500
- Coverage > 50%
```

### 3. GitHub Actions Workflow

**Location**: `.github/workflows/quality-analysis.yml`

**Trigger**: Push to `main` branch, pull requests, manual dispatch

**Steps**:
1. Checkout code with full history
2. Set up Java 21 and Python 3.11
3. Build project with `mvn clean compile -DskipTests`
4. Run analysis with `mvn verify -DskipTests`
5. Aggregate metrics with Python script
6. Upload reports as artifacts (30-day retention)
7. Post metrics to backend API (main branch only)
8. Comment on PR with quality summary

**Required Secrets**:
- `API_URL` (default: `https://api.strategiz.io`)
- `QUALITY_ANALYSIS_TOKEN` - Service account token for API authentication

### 4. Backend Cache System

#### Data Layer

**Entity**: `data-quality/CachedQualityMetricsEntity.java`

**Firestore Path**: `system/quality_cache/cache/{analysisId}`
- `latest` - Most recent analysis results
- `{commitHash}-{timestamp}` - Historical results

**Fields**:
```java
String analysisId;
Instant analyzedAt;
String gitCommitHash;
String gitBranch;
int bugs;
int vulnerabilities;
int codeSmells;
double coverage;
double duplications;
String technicalDebt;
String reliabilityRating;
String securityRating;
String maintainabilityRating;
String qualityGateStatus;
int totalIssues;
int newIssues;
String analysisSource;
String buildNumber;
```

#### Repository

**Interface**: `data-quality/CachedQualityMetricsRepository.java`

**Implementation**: `client-firebase/CachedQualityMetricsRepositoryImpl.java`

**Methods**:
- `save(entity)` - Save to both `latest` and history
- `getLatest()` - Fetch most recent cached metrics
- `findById(analysisId)` - Fetch specific analysis by ID
- `deleteAll()` - Clear all cached metrics

#### Service Layer

**Service**: `service-console-quality/QualityMetricsService.java`

**Key Methods**:

##### `getSonarQubeMetrics()`
Cache-first pattern:
```java
1. Try cache (if < 24 hours old)
2. Fall back to SonarQube API (if available)
3. Return empty metrics (if neither available)
```

##### `cacheAnalysisResults(metrics)`
Called by CI/CD pipeline to store analysis results:
```java
1. Convert model to entity
2. Save to Firestore (both latest and history)
3. Log success
```

##### `getLatestCachedMetrics()`
Retrieve most recent cached metrics for API responses.

#### REST Controller

**Controller**: `service-console-quality/AdminQualityController.java`

**Endpoints**:

##### `POST /v1/console/quality/cache`
Cache analysis results from CI/CD pipeline.

**Request Body**:
```json
{
  "analysisId": "a1b2c3d4-20251225120000",
  "analyzedAt": "2025-12-25T12:00:00Z",
  "gitCommitHash": "a1b2c3d4e5f6...",
  "gitBranch": "main",
  "bugs": 12,
  "vulnerabilities": 0,
  "codeSmells": 245,
  "coverage": 67.5,
  "duplications": 0.0,
  "technicalDebt": "150h",
  "reliabilityRating": "B",
  "securityRating": "A",
  "maintainabilityRating": "B",
  "qualityGateStatus": "PASSED",
  "totalIssues": 257,
  "newIssues": 0,
  "analysisSource": "github-actions",
  "buildNumber": "123"
}
```

**Response**: `200 OK`

**Authentication**: Requires service account token (TODO: Add `@RequireAuth`)

##### `GET /v1/console/quality/cache/latest`
Retrieve most recent cached analysis results.

**Response**:
```json
{
  "analysisId": "a1b2c3d4-20251225120000",
  "analyzedAt": "2025-12-25T12:00:00Z",
  ...
}
```

or `404 Not Found` if no cached metrics exist.

### 5. Frontend Dashboard

**Component**: `apps/console/src/features/console/screens/ConsoleQualityScreen.tsx`

**Features**:
- Overall quality overview card (grade, compliance score, SonarQube rating)
- Bugs, vulnerabilities, code smells counts
- Technical debt estimate
- Compliance breakdown by pattern (exception handling, service, controller)
- Top violations table with file paths and line numbers

**API Integration**:
```typescript
// Redux action
dispatch(fetchQualityOverview());

// API client
consoleApiClient.getQualityOverview()
  → GET /v1/console/quality/overview
```

## Usage

### Running Analysis Locally

```bash
# Run all analysis tools
mvn verify -DskipTests

# Run specific tool
mvn spotbugs:check
mvn pmd:pmd
mvn checkstyle:check
mvn jacoco:report

# Aggregate results
export GIT_COMMIT_HASH=$(git rev-parse HEAD)
export GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
python scripts/aggregate-quality-metrics.py quality-metrics.json
```

### Viewing Reports Locally

**SpotBugs**: `target/spotbugs/spotbugsXml.xml`
**PMD**: `target/pmd/pmd.xml`
**Checkstyle**: `target/checkstyle/checkstyle-result.xml`
**JaCoCo**: `target/site/jacoco/index.html` (open in browser)

### Manual Cache Upload

```bash
# Generate metrics
python scripts/aggregate-quality-metrics.py quality-metrics.json

# Post to backend
curl -X POST https://api.strategiz.io/v1/console/quality/cache \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d @quality-metrics.json
```

### Accessing Dashboard

1. Navigate to `console.strategiz.io`
2. Sign in with admin account
3. Click "Quality" in sidebar
4. View real-time quality metrics

## Configuration

### GitHub Actions Secrets

Required for automatic cache uploads:

1. Go to GitHub repository → Settings → Secrets and variables → Actions
2. Add secret: `QUALITY_ANALYSIS_TOKEN`
   - Value: Service account JWT token with access to `/v1/console/quality/cache`
3. Optionally add: `API_URL` (defaults to `https://api.strategiz.io`)

### Maven Plugin Configuration

All plugins configured in root `pom.xml` under `<build><plugins>`.

**To disable a specific tool**:
```xml
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <configuration>
    <skip>true</skip>  <!-- Add this -->
  </configuration>
</plugin>
```

**To adjust quality gate thresholds**:

Edit `scripts/aggregate-quality-metrics.py`:
```python
def calculate_quality_gate_status(bugs, vulnerabilities, code_smells, coverage):
    if vulnerabilities > 0:  # Change threshold
        return "FAILED"
    if bugs >= 50:  # Change threshold
        return "FAILED"
    # ...
```

### Cache Freshness

Cache is considered fresh for **24 hours**.

To change: Edit `service-console-quality/QualityMetricsService.java`:
```java
private static final int CACHE_FRESHNESS_HOURS = 24;  // Change this
```

## Troubleshooting

### "No cached metrics and SonarQubeClient not available"

**Cause**: No cached metrics in Firestore and SonarQube is not configured.

**Solution**: Wait for next GitHub Actions run, or run analysis manually and upload.

### GitHub Actions workflow fails on metric upload

**Cause**: Missing or invalid `QUALITY_ANALYSIS_TOKEN` secret.

**Solution**:
1. Verify secret is set in repository settings
2. Ensure token has valid permissions
3. Check API logs for authentication errors

### Analysis reports not found

**Cause**: Maven plugins didn't run successfully.

**Solution**:
1. Check Maven build logs for plugin errors
2. Verify plugins are in `verify` phase: `mvn verify -DskipTests`
3. Check report output directories exist

### Python script fails to parse XML

**Cause**: XML report format changed or malformed.

**Solution**:
1. Check XML report manually
2. Update parser in `scripts/aggregate-quality-metrics.py`
3. Add better error handling

## Cost Analysis

All tools are **free and open-source**:

- **SpotBugs**: Free (Apache License 2.0)
- **PMD**: Free (BSD License)
- **Checkstyle**: Free (LGPL)
- **JaCoCo**: Free (EPL 2.0)
- **GitHub Actions**: Free for public repos, 2000 min/month for private
- **Firestore**: Free tier (1GB storage, 50K reads/day)

**Total Monthly Cost**: $0 for public repos, minimal for private repos

Compare to SonarCloud:
- Free tier: Public repos only
- Developer plan: $10/month (100K LOC)
- Enterprise plan: $120/month (1M LOC)

## Future Enhancements

### Planned Features

1. **Baseline Comparison**: Track `newIssues` by comparing against previous analysis
2. **Trend Analysis**: Store historical metrics for trend charts
3. **Quality Gate Enforcement**: Fail builds if quality gate doesn't pass
4. **Custom Rules**: Add project-specific Checkstyle/PMD rules
5. **Dependency Scanning**: Add OWASP Dependency Check for CVE detection
6. **API Authentication**: Add `@RequireAuth` to cache endpoint

### Optional Integrations

1. **SonarQube Self-Hosted**: Add SonarQube server for advanced features
   - See `SONARQUBE_SETUP.md` for setup instructions
   - Update `QualityMetricsService` to use live API

2. **SonarCloud**: Use managed SonarQube service
   - See `SONARQUBE_OPTIONS.md` for comparison
   - Configure in GitHub Actions workflow

3. **SARIF Upload**: Upload results to GitHub Code Scanning
   - Convert reports to SARIF format
   - Use `github/codeql-action/upload-sarif@v3`

## References

- [SpotBugs Documentation](https://spotbugs.github.io/)
- [PMD Documentation](https://pmd.github.io/)
- [Checkstyle Documentation](https://checkstyle.org/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/)
- [SonarQube Quality Gate Ratings](https://docs.sonarqube.org/latest/user-guide/metric-definitions/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
