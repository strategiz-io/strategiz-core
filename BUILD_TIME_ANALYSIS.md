# Build-Time Quality Analysis Strategy

This document explains how to get SonarQube-quality metrics **without running a SonarQube server** by analyzing code during builds and caching results.

## 🎯 Architecture Overview

```
┌─────────────────┐
│  GitHub Push    │
│  to main        │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│  GitHub Actions / Cloud Build   │
│  ┌───────────────────────────┐  │
│  │ 1. Checkout code          │  │
│  │ 2. Run Maven build        │  │
│  │ 3. Run SonarQube scanner  │  │
│  │    (in-memory mode)       │  │
│  │ 4. Extract JSON results   │  │
│  │ 5. POST to backend API    │  │
│  └───────────────────────────┘  │
└────────────┬────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  Backend API                     │
│  POST /v1/console/quality/cache  │
│  ┌────────────────────────────┐  │
│  │ Store in Firestore:        │  │
│  │ system/quality/latest      │  │
│  └────────────────────────────┘  │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  Quality Dashboard               │
│  GET /v1/console/quality/overview│
│  ┌────────────────────────────┐  │
│  │ 1. Run ComplianceScanner   │  │
│  │ 2. Read cached metrics     │  │
│  │    from Firestore          │  │
│  │ 3. Combine & return        │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

## ✅ Benefits

1. **No Infrastructure Cost**: No SonarQube server to run ($0/month)
2. **Always Up-to-Date**: Metrics from latest main branch
3. **Fast Dashboard**: Reads from Firestore cache (milliseconds)
4. **Production Ready**: Works in Cloud Run without external dependencies
5. **Git Context**: Includes commit hash, branch, build number

## 📦 Implementation Options

### Option 1: SonarCloud (Easiest)

**How it works:**
- SonarCloud hosts the analysis platform
- GitHub Actions pushes code → SonarCloud analyzes
- Backend fetches results via SonarCloud API
- Caches in Firestore for fast access

**Setup:**
```yaml
# .github/workflows/quality.yml
- name: SonarCloud Scan
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: |
    mvn sonar:sonar \
      -Dsonar.host.url=https://sonarcloud.io \
      -Dsonar.organization=strategiz \
      -Dsonar.projectKey=strategiz_strategiz-core
```

**Cost:** $10-120/month (but you get hosted analysis + UI)

---

### Option 2: SonarQube Scanner CLI (Free, More Complex)

**How it works:**
- Run `sonar-scanner` in CI/CD with in-memory database
- Parse the generated JSON report file
- Extract metrics and POST to backend
- No SonarQube server needed!

**Setup:**

1. **GitHub Actions Workflow**
```yaml
# .github/workflows/quality-analysis.yml
name: Code Quality Analysis

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Install SonarScanner CLI
        run: |
          wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-linux.zip
          unzip sonar-scanner-cli-5.0.1.3006-linux.zip
          export PATH=$PATH:$(pwd)/sonar-scanner-5.0.1.3006-linux/bin

      - name: Run analysis with in-memory mode
        run: |
          # Run scanner without server (generates local report)
          sonar-scanner \
            -Dsonar.projectKey=strategiz-core \
            -Dsonar.sources=. \
            -Dsonar.exclusions='**/target/**,**/test/**' \
            -Dsonar.java.binaries=target/classes \
            -Dsonar.working.directory=.scannerwork \
            -Dsonar.scm.provider=git

          # Scanner creates JSON report in .scannerwork/report-task.txt

      - name: Parse results and cache
        env:
          BACKEND_URL: ${{ secrets.BACKEND_URL }}
          API_TOKEN: ${{ secrets.API_TOKEN }}
        run: |
          # Extract metrics from scanner output
          python3 scripts/parse-sonar-results.py .scannerwork

          # POST to backend
          curl -X POST $BACKEND_URL/v1/console/quality/cache \
            -H "Authorization: Bearer $API_TOKEN" \
            -H "Content-Type: application/json" \
            -d @quality-metrics.json
```

2. **Python Parser Script**
```python
# scripts/parse-sonar-results.py
import json
import sys
import os

def parse_sonar_report(scannerwork_dir):
    # Read SonarQube scanner output
    report_path = os.path.join(scannerwork_dir, 'report-task.txt')

    # Parse metrics (simplified - actual implementation more complex)
    metrics = {
        'analysisId': os.environ.get('GITHUB_SHA', 'unknown'),
        'analyzedAt': datetime.now().isoformat(),
        'gitCommitHash': os.environ.get('GITHUB_SHA'),
        'gitBranch': os.environ.get('GITHUB_REF_NAME'),
        'bugs': 0,  # Parse from scanner output
        'vulnerabilities': 0,
        'codeSmells': 0,
        'coverage': 0.0,
        'duplications': 0.0,
        'technicalDebt': '0h',
        'reliabilityRating': 'A',
        'analysisSource': 'GitHub Actions',
        'buildNumber': os.environ.get('GITHUB_RUN_NUMBER')
    }

    # Write to file
    with open('quality-metrics.json', 'w') as f:
        json.dump(metrics, f)

if __name__ == '__main__':
    parse_sonar_report(sys.argv[1])
```

**Cost:** $0/month (100% free)

---

### Option 3: SpotBugs + PMD + Checkstyle (Alternative)

**How it works:**
- Use free Java analysis tools instead of SonarQube
- Run in CI/CD, parse XML reports
- Similar metrics, different tooling

**Tools:**
- **SpotBugs**: Bug detection
- **PMD**: Code smells
- **Checkstyle**: Style violations
- **JaCoCo**: Code coverage

**Setup:**
```xml
<!-- pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>com.github.spotbugs</groupId>
      <artifactId>spotbugs-maven-plugin</artifactId>
      <version>4.8.3.0</version>
      <configuration>
        <xmlOutput>true</xmlOutput>
        <xmlOutputDirectory>target/spotbugs</xmlOutputDirectory>
      </configuration>
    </plugin>

    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-pmd-plugin</artifactId>
      <version>3.21.2</version>
    </plugin>

    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
    </plugin>
  </plugins>
</build>
```

**GitHub Actions:**
```yaml
- name: Run quality checks
  run: |
    mvn spotbugs:check pmd:check jacoco:report

- name: Parse reports and cache
  run: |
    python3 scripts/aggregate-quality-reports.py
    # POST aggregated metrics to backend
```

**Cost:** $0/month (all tools are free)

---

## 🔧 Backend Implementation

### 1. Add Cache Endpoint

```java
// AdminQualityController.java

@PostMapping("/cache")
public ResponseEntity<Void> cacheAnalysisResults(
        @RequestBody CachedQualityMetrics metrics) {

    qualityMetricsService.cacheResults(metrics);
    return ResponseEntity.ok().build();
}
```

### 2. Update Service to Use Cache

```java
// QualityMetricsService.java

public SonarQubeMetrics getSonarQubeMetrics() {
    // Try to read from cache first
    CachedQualityMetrics cached = readFromCache();

    if (cached != null && isFresh(cached)) {
        return convertToSonarQubeMetrics(cached);
    }

    // Fallback to SonarQube API if available
    if (sonarQubeClient != null) {
        return sonarQubeClient.getProjectMetrics();
    }

    // Return empty metrics
    return new SonarQubeMetrics(0, 0, 0, 0.0, 0.0, "0h", "N/A");
}

private CachedQualityMetrics readFromCache() {
    // Read from Firestore: system/quality/latest
    DocumentSnapshot doc = firestore
        .collection("system")
        .document("quality")
        .collection("cache")
        .document("latest")
        .get()
        .get();

    return doc.exists()
        ? doc.toObject(CachedQualityMetrics.class)
        : null;
}

private boolean isFresh(CachedQualityMetrics metrics) {
    // Consider fresh if < 24 hours old
    Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
    return metrics.getAnalyzedAt().isAfter(oneDayAgo);
}
```

### 3. Add Firestore Entity

```java
// Firestore path: system/quality/cache/latest
{
  "analysisId": "abc123",
  "analyzedAt": "2025-12-25T12:00:00Z",
  "gitCommitHash": "d4f5e6a7b8c9",
  "gitBranch": "main",
  "bugs": 12,
  "vulnerabilities": 3,
  "codeSmells": 156,
  "coverage": 68.4,
  "duplications": 2.3,
  "technicalDebt": "2d 4h",
  "reliabilityRating": "A",
  "analysisSource": "GitHub Actions",
  "buildNumber": "1234"
}
```

---

## 🎨 Frontend Updates

Quality dashboard automatically shows:
- **Last analyzed**: "2 hours ago (commit: d4f5e6a)"
- **Analysis source**: "GitHub Actions #1234"
- Refresh button to trigger new analysis

---

## 📊 Comparison: Server vs Build-Time

| Feature | SonarQube Server | Build-Time Analysis |
|---------|-----------------|-------------------|
| **Infrastructure** | VM or SonarCloud | None (CI/CD only) |
| **Cost** | $30-120/month | $0/month |
| **Real-time** | Yes | On push to main |
| **Historical trends** | Yes | Limited (store N results) |
| **Setup complexity** | Medium | Low-Medium |
| **Production deps** | External API | Firestore only |
| **Dashboard speed** | API call | Cached (fast) |

---

## 🚀 Recommended Approach for Strategiz

**Phase 1: Build-Time with Free Tools**
```
✅ GitHub Actions runs on every push
✅ SpotBugs + PMD + JaCoCo for metrics
✅ Results cached in Firestore
✅ Quality dashboard reads from cache
✅ Cost: $0/month
```

**Phase 2: Upgrade to SonarCloud (Optional)**
```
✅ Better analysis quality
✅ Historical trends
✅ Security hotspots
✅ Cost: $10-120/month
```

---

## 📝 Next Steps

1. **Choose approach**: SonarCloud or Free Tools?
2. **Set up GitHub Actions workflow**
3. **Add cache endpoint to backend**
4. **Test with manual workflow trigger**
5. **Monitor quality dashboard**

Want me to implement the GitHub Actions + cache endpoint for you? 🚀
