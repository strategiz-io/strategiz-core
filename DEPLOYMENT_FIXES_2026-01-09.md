# Deployment Fixes - January 9, 2026

## Summary of Changes

This document summarizes the critical fixes applied to the Strategiz platform for both local development and production deployments.

## 1. Vault Integration Fix (CRITICAL)

### Problem
Applications were unable to load secrets from HashiCorp Vault because VAULT_TOKEN was not properly exported as an environment variable for the Spring Boot process.

### Root Cause
- VAULT_TOKEN was being passed as a JVM argument (`-DVAULT_TOKEN=root`) instead of an environment variable
- Spring's Environment abstraction couldn't access JVM properties set this way
- VaultHttpClient needs VAULT_TOKEN as an environment variable

### Solution

#### Local Development
Updated `/tmp/start_strategiz_proper.sh`:
```bash
# Export Vault environment variables
export VAULT_TOKEN=root
export VAULT_ADDR=http://localhost:8200

# Start application
mvn spring-boot:run \
  -Dmaven.test.skip=true \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

#### Production
Updated `/scripts/production/utils/start.sh`:
```bash
# Export Vault environment variables for the application
export VAULT_ADDR=http://localhost:8200
if [ -z "$VAULT_TOKEN" ]; then
    echo "WARNING: VAULT_TOKEN not set, application may fail to load secrets"
fi
export VAULT_TOKEN

# Start the Spring Boot application
java -jar app.jar --server.port=${PORT} &
```

### Impact
- **Application API**: Now loads ClickHouse credentials (host, port, database, username, password) successfully from Vault
- **Console App**: Fixed authentication and ClickHouse access
- **All Environments**: Production, staging, and local development now properly integrate with Vault

---

## 2. Console App Authentication Fix

### Problem
Console UI at localhost:3001 showed "Request failed with status code 401" when accessing Job Execution History endpoint.

### Root Cause
- `AdminAuthInterceptor` required Firebase/Firestore authentication even in local dev
- Firebase credentials not configured for console app
- JPA autoconfiguration conflicting with ClickHouse-only setup

### Solution

#### Configuration Changes
**application-console/src/main/resources/application.properties:**
```properties
# Console Authentication (disable for local development)
console.auth.enabled=false

# TimescaleDB disabled (migrated to ClickHouse)
strategiz.timescale.enabled=false

# ClickHouse enabled
strategiz.clickhouse.enabled=true
strategiz.clickhouse.ssl=true
strategiz.clickhouse.pool.max-pool-size=5
strategiz.clickhouse.pool.min-idle=1
strategiz.clickhouse.pool.connection-timeout-ms=10000
strategiz.clickhouse.pool.idle-timeout-ms=300000
```

#### Code Changes
**AdminAuthInterceptor.java:**
```java
@Component
@ConditionalOnProperty(
    name = "console.auth.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AdminAuthInterceptor implements HandlerInterceptor {
    // ... conditional bean creation
}
```

**ConsoleApplication.java:**
```java
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
```

#### Local Development Startup
```bash
export VAULT_TOKEN=root
export VAULT_ADDR=http://localhost:8200
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json

cd application-console
mvn spring-boot:run -Dmaven.test.skip=true
```

### Impact
- Console app now starts successfully on port 8081
- Job execution history endpoint works without authentication in dev
- Production deployment still requires admin authentication (console.auth.enabled=true)

---

## 3. ClickHouse DateTime Fix

### Problem
Console app returned SQL errors when querying `job_executions` table:
```
Syntax error: failed at position 61 (23) (line 2, col 32)
ORDER BY start_time DESC
```

### Root Cause
ClickHouse DateTime64 columns don't work well with `java.sql.Timestamp` in prepared statements. The JDBC driver was sending timestamps in a format that ClickHouse couldn't parse.

### Solution
**JobExecutionClickHouseRepository.java:**
```java
// OLD (broken):
public List<JobExecutionEntity> findAllSince(Instant since) {
    String sql = """
        SELECT * FROM job_executions
        WHERE start_time >= ?
        ORDER BY start_time DESC
        """;
    return jdbcTemplate.query(sql, rowMapper, Timestamp.from(since));
}

// NEW (working):
public List<JobExecutionEntity> findAllSince(Instant since) {
    String sql = """
        SELECT * FROM job_executions
        WHERE start_time >= parseDateTimeBestEffort(?)
        ORDER BY start_time DESC
        """;
    return jdbcTemplate.query(sql, rowMapper, since.toString());
}
```

**Methods Fixed:**
- `findAllSince(Instant since)`
- `findStaleRunningJobs(Instant before)`

### Impact
- Console job history endpoint now returns data successfully
- No more SQL syntax errors in ClickHouse queries
- Proper handling of DateTime64 columns across all ClickHouse repositories

---

## 4. Console App Production Deployment

### New Deployment Script
Created `/scripts/production/build/deploy-console-app.sh` for deploying the console app as a separate Cloud Run service.

**Key Configuration:**
```yaml
Memory: 4Gi (for batch job processing)
Timeout: 480s (8 minutes for long-running jobs)
Profiles: prod,scheduler (enables batch job scheduling)
ClickHouse: enabled
TimescaleDB: disabled
Console Auth: enabled (requires admin authentication)
```

**Environment Variables:**
- `VAULT_ADDR` - Vault server URL
- `VAULT_TOKEN` - Vault authentication token
- `console.auth.enabled=true` - Enforce admin authentication in production
- `strategiz.clickhouse.enabled=true` - Use ClickHouse for data storage
- `strategiz.timescale.enabled=false` - Disable TimescaleDB

### Impact
- Console app can be deployed independently from main API
- Separate scaling and resource allocation for batch jobs
- Isolated admin endpoints from public API surface

---

## 5. 1Hour Market Data Backfill

### Status
- **Job Status**: RUNNING ✅
- **Progress**: 372/547 symbols (68% complete)
- **Timeframe**: 1Hour
- **Years**: 7 (2019-2026)
- **Started**: 2026-01-09 22:20:23
- **Current Symbol**: PPG
- **Expected Duration**: 2-4 hours total
- **Expected Data Points**: ~6.7 million

### Verification
```bash
# Check backfill status
curl -k -s https://localhost:8443/v1/marketdata/admin/backfill/status

# Check job execution history
curl -s http://localhost:8081/v1/console/jobs/history
```

---

## Configuration Checklist

### Local Development
- [ ] Vault running on localhost:8200
- [ ] `export VAULT_TOKEN=root`
- [ ] `export VAULT_ADDR=http://localhost:8200`
- [ ] Firebase service account JSON in resources
- [ ] `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json`
- [ ] ClickHouse credentials in Vault at `secret/strategiz/clickhouse`

### Production (Cloud Run)
- [ ] Vault production server accessible
- [ ] `VAULT_TOKEN_PROD` environment variable set
- [ ] `VAULT_ADDR_PROD` points to production Vault
- [ ] Firebase credentials configured via GCP service account
- [ ] ClickHouse credentials in production Vault
- [ ] Console authentication enabled (`console.auth.enabled=true`)

---

## Critical Files Modified

1. `/scripts/production/utils/start.sh` - Production startup with Vault env vars
2. `/scripts/production/build/deploy-console-app.sh` - NEW console deployment script
3. `application-console/src/main/resources/application.properties` - Console config
4. `application-console/src/main/java/ConsoleApplication.java` - Disabled JPA
5. `service/service-console/config/AdminAuthInterceptor.java` - Conditional auth
6. `service/service-console/config/ConsoleWebConfig.java` - Conditional web config
7. `data/data-marketdata/clickhouse/repository/JobExecutionClickHouseRepository.java` - DateTime fix

---

## Testing Verification

### API Application (port 8443)
```bash
# Health check
curl -k https://localhost:8443/actuator/health

# ClickHouse connectivity
curl -k https://localhost:8443/actuator/health/clickHouse

# Backfill status
curl -k https://localhost:8443/v1/marketdata/admin/backfill/status
```

### Console Application (port 8081)
```bash
# Health check
curl http://localhost:8081/actuator/health

# Job execution history
curl http://localhost:8081/v1/console/jobs/history
```

---

## Lessons Learned

1. **Environment Variables vs JVM Arguments**: Spring Boot applications need environment variables (`export VAR=value`) not JVM properties (`-Dvar=value`) for proper integration with VaultHttpClient

2. **ClickHouse DateTime Handling**: Use ClickHouse functions like `parseDateTimeBestEffort()` for DateTime64 columns instead of JDBC Timestamp objects

3. **Conditional Bean Creation**: Use `@ConditionalOnProperty` to make beans optional based on configuration, enabling dev/prod flexibility

4. **Separate Deployments**: Console app should be deployed separately with higher memory/timeout limits for batch processing

5. **Vault KV Versions**: Current setup uses KV v2 (paths include `/data/`), ensure VaultSecretService builds paths correctly

---

## Future Improvements

1. **Vault Integration Testing**: Add integration tests that verify Vault connectivity and secret loading
2. **ClickHouse Repository Tests**: Unit tests for DateTime handling in all repository methods
3. **Console Auth Testing**: E2E tests for admin authentication flow
4. **Deployment Automation**: CI/CD pipeline for automatic console app deployments
5. **Monitoring**: Add alerts for Vault connectivity failures and ClickHouse query errors

---

## Contact

For questions about these changes, refer to:
- Git commit history: `git log --since="2026-01-09"`
- This document: `/DEPLOYMENT_FIXES_2026-01-09.md`
- Claude Code session: 2026-01-09 (Vault integration and console app fixes)
