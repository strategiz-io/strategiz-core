# Hybrid Observability System

## Overview

The Strategiz platform uses a **hybrid observability approach** combining two complementary systems:

1. **Micrometer (Primary)** - Real-time, in-memory metrics for instant observability
2. **Grafana Cloud (Secondary)** - Historical metrics for trend analysis and long-term monitoring

This provides the best of both worlds: instant visibility into current system health AND the ability to analyze trends over time.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         HTTP Request (any endpoint)                │    │
│  └──────────────────┬─────────────────────────────────┘    │
│                     │                                        │
│                     ▼                                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │    Spring Boot Actuator Auto-Instrumentation      │    │
│  │    ✓ Tracks all HTTP requests automatically        │    │
│  │    ✓ Records latency (p50, p95, p99, max)         │    │
│  │    ✓ Counts success/error responses                │    │
│  │    ✓ Tags: uri, method, status, exception          │    │
│  └──────────────────┬─────────────────────────────────┘    │
│                     │                                        │
│           ┌─────────┴──────────┐                           │
│           │                    │                            │
│           ▼                    ▼                            │
│  ┌────────────────┐   ┌────────────────────┐              │
│  │   Micrometer   │   │  OpenTelemetry     │              │
│  │  MeterRegistry │   │   OTLP Exporter    │              │
│  │  (in-memory)   │   │  (to Grafana)      │              │
│  └────────┬───────┘   └─────────┬──────────┘              │
│           │                     │                           │
└───────────┼─────────────────────┼───────────────────────────┘
            │                     │
            │                     │
            ▼                     ▼
   ┌─────────────────┐   ┌──────────────────┐
   │  Admin Console  │   │  Grafana Cloud   │
   │  Real-time UI   │   │  (Prometheus)    │
   │  /endpoints     │   │  Historical      │
   └─────────────────┘   └──────────────────┘
```

## System 1: Micrometer (Real-Time Observability)

### Purpose
Provides **instant** visibility into endpoint performance and availability without external dependencies.

### How It Works

1. **Automatic Instrumentation**: Spring Boot Actuator automatically tracks all HTTP requests
2. **In-Memory Storage**: Metrics stored in JVM memory via `MeterRegistry`
3. **Query API**: REST endpoints expose aggregated metrics in real-time
4. **Frontend Dashboard**: Admin console displays metrics with drill-down capability

### Data Collected

For **every REST endpoint**, Micrometer tracks:

| Metric | Description |
|--------|-------------|
| `totalRequests` | Total number of requests received |
| `successRequests` | Requests with 2xx/3xx status codes |
| `errorRequests` | Requests with 4xx/5xx status codes |
| `availability` | Success rate percentage (success/total) |
| `errorRate` | Error percentage (error/total) |
| `latencyP50Ms` | Median response time (50th percentile) |
| `latencyP95Ms` | 95th percentile response time |
| `latencyP99Ms` | 99th percentile response time |
| `latencyMaxMs` | Maximum response time observed |
| `latencyMeanMs` | Average response time |
| `errorsByStatus` | Error count breakdown by HTTP status code |
| `errorsByException` | Error count breakdown by exception type |

### REST API Endpoints

All endpoints require admin authentication via `/v1/console/*` path:

#### 1. Get All Endpoints
```http
GET /v1/console/endpoints
```

Returns metrics for all discovered REST endpoints, sorted by request count.

**Response Example:**
```json
[
  {
    "endpoint": "GET /v1/auth/login",
    "method": "GET",
    "uri": "/v1/auth/login",
    "totalRequests": 15420,
    "successRequests": 15280,
    "errorRequests": 140,
    "availability": 0.9909,
    "errorRate": 0.0091,
    "latencyP50Ms": 45.2,
    "latencyP95Ms": 120.5,
    "latencyP99Ms": 245.8,
    "latencyMaxMs": 1205.3,
    "latencyMeanMs": 67.4,
    "errorsByStatus": {
      "400": 30,
      "401": 85,
      "500": 25
    },
    "errorsByException": {
      "InvalidCredentialsException": 85,
      "ValidationException": 30
    }
  }
]
```

#### 2. Get System-Wide Metrics
```http
GET /v1/console/endpoints/system
```

Returns overall system health summary.

**Response Example:**
```json
{
  "totalEndpoints": 85,
  "totalRequests": 450230,
  "successRequests": 445180,
  "errorRequests": 5050,
  "overallAvailability": 0.9888,
  "overallErrorRate": 0.0112,
  "worstP99LatencyMs": 2450.5
}
```

#### 3. Get Metrics by Module
```http
GET /v1/console/endpoints/by-module
```

Returns aggregated metrics grouped by service module (auth, console, provider, labs, etc.).

**Response Example:**
```json
{
  "auth": {
    "module": "auth",
    "endpointCount": 12,
    "totalRequests": 125000,
    "successRequests": 123500,
    "errorRequests": 1500,
    "availability": 0.988,
    "errorRate": 0.012,
    "worstP99LatencyMs": 450.5
  },
  "console": {
    "module": "console",
    "endpointCount": 8,
    "totalRequests": 8500,
    "successRequests": 8450,
    "errorRequests": 50,
    "availability": 0.9941,
    "errorRate": 0.0059,
    "worstP99LatencyMs": 320.2
  }
}
```

#### 4. Get Single Endpoint
```http
GET /v1/console/endpoints/endpoint?method=GET&uri=/v1/auth/login
```

Returns detailed metrics for a specific endpoint.

#### 5. Health Check
```http
GET /v1/console/endpoints/health-check
```

Returns list of unhealthy endpoints (availability < 95% OR p99 latency > 500ms).

**Response Example:**
```json
[
  {
    "endpoint": "POST /v1/labs/execute",
    "availability": 0.92,
    "latencyP99Ms": 1250.5,
    "errorRequests": 450
  }
]
```

### Frontend Dashboard

**Location**: `http://localhost:3001/endpoints` (console app)

The admin console provides a comprehensive UI with 4 tabs:

#### Tab 0: Summary
- **System Health Cards**: Total endpoints, total requests, availability, worst p99 latency
- **Module Table**: Clickable rows showing aggregated metrics per module
  - Click module row → navigates to Module tab with filter applied

#### Tab 1: By Module
- **Module Filter**: Dropdown to select specific module (auth, provider, labs, etc.)
- **Endpoint List**: All endpoints for selected module
- **Expandable Rows**: Click to see performance metrics and error breakdown

#### Tab 2: All Endpoints
- **Search Bar**: Real-time filtering by endpoint name
- **Sortable Columns**: Sort by requests, availability, latency, errors
- **Result Count**: Shows "X of Y endpoints" based on search
- **Expandable Rows**: Same drill-down as Module tab

#### Tab 3: Health
- **Unhealthy Endpoints**: Shows endpoints with availability < 95% or p99 > 500ms
- **Success Message**: Displays if all endpoints are healthy

#### Expandable Row Details

Every endpoint row expands to show:

**Performance Metrics Grid:**
- p50 latency (green if <100ms, yellow if <500ms, red if ≥500ms)
- p95 latency
- p99 latency
- Max latency

**Error Breakdown:**
- **By Status Code**: 400 (30 errors), 401 (85 errors), 500 (25 errors)
- **By Exception**: InvalidCredentialsException (85), ValidationException (30)

**Color-Coded Health Indicators:**
- ✓ Green: availability ≥99% AND p99 <100ms
- ⚠ Yellow: availability ≥95% AND p99 <500ms
- ✗ Red: availability <95% OR p99 ≥500ms

**Auto-Refresh**: Dashboard refreshes every 30 seconds automatically

### Configuration

Enable percentile histograms in `application.properties`:

```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized

# Percentile histograms for accurate latency tracking
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.percentiles.http.server.requests=0.5,0.95,0.99

# SLA boundaries for latency tracking
management.metrics.distribution.slo.http.server.requests=50ms,100ms,200ms,500ms,1s,2s,5s

# Add application tag to all metrics
management.metrics.tags.application=${spring.application.name}
```

### Implementation

**Backend:**
- `EndpointMetricsService.java` - Queries `MeterRegistry` and aggregates metrics
- `AdminEndpointMetricsController.java` - REST API exposing 5 endpoints

**Frontend:**
- `ConsoleEndpointMetricsScreen.tsx` - 734-line React component with all 4 tabs

### Limitations

1. **No Historical Data**: Metrics reset when application restarts
2. **Memory Only**: No persistence across deployments
3. **Limited Time Range**: Can only see current state, not trends over time
4. **No Alerting**: Manual monitoring only

**Solution**: Use Grafana Cloud for historical data and alerting (System 2)

## System 2: Grafana Cloud (Historical Observability)

### Purpose
Provides **long-term** trend analysis, alerting, and correlation across distributed services.

### How It Works

1. **OTLP Export**: OpenTelemetry exporter sends metrics to Grafana Cloud every 60 seconds
2. **Prometheus Storage**: Metrics stored in Grafana Cloud Prometheus (time-series database)
3. **Grafana Dashboards**: Pre-built dashboards visualize trends over days/weeks/months
4. **Alerting**: Automatic alerts for SLA violations (p99 > 500ms, availability < 95%)

### Configuration

**Status**: OpenTelemetry integration is **90% complete** but **disabled in dev mode**.

**Required Secrets** (stored in Vault):

```bash
# Production Vault path: secret/strategiz/grafana
GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-us-east-0.grafana.net/otlp
GRAFANA_OTLP_AUTH_HEADER=Basic <base64-encoded-credentials>
GRAFANA_PROMETHEUS_URL=https://prometheus-prod-01-us-east-0.grafana.net/api/prom
GRAFANA_PROMETHEUS_AUTH=<instance-id>:<api-key>
```

**Production Config** (`application-prod.properties`):

```properties
# OpenTelemetry OTLP export to Grafana Cloud
otel.traces.exporter=otlp
otel.metrics.exporter=otlp
otel.exporter.otlp.endpoint=${GRAFANA_OTLP_ENDPOINT}
otel.exporter.otlp.headers=Authorization=${GRAFANA_OTLP_AUTH_HEADER}
otel.exporter.otlp.protocol=http/protobuf

# Sampling (10% of traces to reduce cost)
otel.traces.sampler=traceidratio
otel.traces.sampler.arg=0.1

# Service name
otel.service.name=strategiz-api
otel.resource.attributes=environment=prod
```

### Grafana Dashboards (Optional)

Once enabled, you can create dashboards showing:

1. **System Overview**: Request rate, error rate, latency trends over time
2. **API Performance**: Per-endpoint latency heatmaps, percentile graphs
3. **Error Analysis**: Error rate trends, error type distribution
4. **Availability SLA**: 99.9% uptime tracking with SLA violations
5. **Provider Health**: Per-provider (Alpaca, Coinbase, etc.) performance

### Enabling Grafana Integration (Optional)

**Step 1: Add Secrets to Vault**

```bash
# Connect to production Vault
export VAULT_ADDR=https://strategiz-vault-43628135674.us-east1.run.app
export VAULT_TOKEN=<your-vault-token>

# Add Grafana credentials
vault kv put secret/strategiz/grafana \
  otlp-endpoint="https://otlp-gateway-prod-us-east-0.grafana.net/otlp" \
  otlp-auth-header="Basic <base64-credentials>" \
  prometheus-url="https://prometheus-prod-01-us-east-0.grafana.net/api/prom" \
  prometheus-auth="<instance-id>:<api-key>"
```

**Step 2: Enable in Production**

OpenTelemetry export is already configured in `application-prod.properties`. Just ensure secrets are in Vault and redeploy.

**Step 3: Create Grafana Dashboards**

Log into Grafana Cloud and create dashboards using PromQL queries:

```promql
# Request rate per endpoint
sum(rate(http_server_requests_seconds_count[5m])) by (uri, method)

# Error rate per endpoint
sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) by (uri)

# P99 latency per endpoint
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (uri, le))

# Availability per endpoint
sum(rate(http_server_requests_seconds_count{status!~"4..|5.."}[5m])) by (uri)
/ sum(rate(http_server_requests_seconds_count[5m])) by (uri)
```

## Comparison: Micrometer vs Grafana

| Feature | Micrometer (Real-Time) | Grafana Cloud (Historical) |
|---------|------------------------|----------------------------|
| **Speed** | Instant (<100ms query) | Slower (2-5s query) |
| **Cost** | Free (in-memory) | Paid ($10-50/month) |
| **Historical Data** | ❌ No (resets on restart) | ✅ Yes (90 days retention) |
| **Alerting** | ❌ No | ✅ Yes (email, Slack, PagerDuty) |
| **Dashboards** | Custom UI | Pre-built + custom |
| **Dependencies** | None (built-in) | Requires Grafana Cloud |
| **Reliability** | 100% (always available) | 99.9% (external service) |
| **Setup Complexity** | ✅ Simple (already done) | Medium (needs secrets) |
| **Coverage** | ✅ 100% of endpoints | ✅ 100% of endpoints |
| **Use Case** | Instant troubleshooting | Trend analysis, SLA tracking |

## Recommended Usage

### When to Use Micrometer
- ✅ Quick health checks during development
- ✅ Instant troubleshooting of production issues
- ✅ Real-time monitoring during deployments
- ✅ Ad-hoc performance analysis
- ✅ When Grafana Cloud is unavailable

### When to Use Grafana
- ✅ Weekly/monthly performance reviews
- ✅ Capacity planning and trend analysis
- ✅ SLA compliance reporting
- ✅ Correlating issues across multiple services
- ✅ Setting up automated alerts

### Best Practice: Use Both!

1. **Daily Operations**: Use Micrometer dashboard for instant visibility
2. **Weekly Reviews**: Use Grafana for trend analysis
3. **Incident Response**: Start with Micrometer (instant), then check Grafana for historical context
4. **Alerts**: Configure Grafana alerts for SLA violations, investigate with Micrometer

## Testing Instructions

### Manual Testing

**Start Backend:**
```bash
cd /Users/cuztomizer/Documents/GitHub/strategiz-core
export VAULT_TOKEN=root
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -pl application-api
```

**Wait for startup** (look for "Started Application" in logs, ~60-90 seconds)

**Start Console:**
```bash
cd /Users/cuztomizer/Documents/GitHub/strategiz-ui
npm run dev:console
```

**Access Dashboard:**
```
http://localhost:3001/endpoints
```

**Generate Test Traffic:**
```bash
# Generate some requests to populate metrics
for i in {1..100}; do
  curl -s http://localhost:8080/v1/auth/session > /dev/null
  curl -s http://localhost:8080/actuator/health > /dev/null
done
```

**Verify All Tabs:**

1. **Summary Tab**:
   - Should show system cards with non-zero values
   - Module table should show auth, console, etc.
   - Click a module row → should navigate to Module tab

2. **Module Tab**:
   - Select module from dropdown
   - Should show filtered endpoint list
   - Click row to expand → should show performance metrics

3. **All Endpoints Tab**:
   - Type "auth" in search box → should filter endpoints
   - Click column headers → should sort
   - Should show "X of Y endpoints" count

4. **Health Tab**:
   - If all healthy: should show success message
   - If unhealthy endpoints exist: should show list with red indicators

**Verify Auto-Refresh:**
- Wait 30 seconds → dashboard should refresh automatically
- Generate more traffic → counts should increase

### API Testing

```bash
# Test all 5 REST endpoints
curl http://localhost:8080/v1/console/endpoints | jq
curl http://localhost:8080/v1/console/endpoints/system | jq
curl http://localhost:8080/v1/console/endpoints/by-module | jq
curl "http://localhost:8080/v1/console/endpoints/endpoint?method=GET&uri=/actuator/health" | jq
curl http://localhost:8080/v1/console/endpoints/health-check | jq
```

## Monitoring Coverage

### Current Coverage: 100%

The hybrid system automatically monitors **every REST endpoint** across all modules:

| Module | Endpoints | Coverage |
|--------|-----------|----------|
| `service-auth` | ~15 | ✅ 100% |
| `service-console` | ~25 | ✅ 100% |
| `service-provider` | ~12 | ✅ 100% |
| `service-labs` | ~10 | ✅ 100% |
| `service-dashboard` | ~8 | ✅ 100% |
| `service-portfolio` | ~6 | ✅ 100% |
| `service-marketplace` | ~5 | ✅ 100% |
| `service-profile` | ~4 | ✅ 100% |
| **Total** | **~85** | **✅ 100%** |

### Zero Configuration Required

No code changes needed to add observability to new endpoints. Spring Boot Actuator automatically instruments:
- All `@RestController` endpoints
- All HTTP methods (GET, POST, PUT, DELETE, PATCH)
- All status codes (2xx, 3xx, 4xx, 5xx)
- All exceptions thrown by controllers

## Troubleshooting

### Issue: No metrics showing in dashboard

**Cause**: No traffic to endpoints yet

**Solution**: Generate test traffic or wait for real users

### Issue: Percentiles show 0.0

**Cause**: Percentile histograms not enabled

**Solution**: Verify `management.metrics.distribution.percentiles-histogram.http.server.requests=true` in `application.properties`

### Issue: Backend won't start

**Cause**: Vault not running or VAULT_TOKEN not set

**Solution**:
```bash
# Start Vault
vault server -dev

# In new terminal
export VAULT_TOKEN=root
```

### Issue: Console shows "Failed to load"

**Cause**: Backend not running on port 8080

**Solution**: Check backend logs and ensure Tomcat started successfully

### Issue: Grafana Cloud not receiving data

**Cause**: OTLP endpoint credentials missing or incorrect

**Solution**: Verify Vault secrets exist and are correct

## Future Enhancements

### Phase 3 (Optional): Advanced Features

1. **SLA Tracking**: Automated SLA compliance reports (99.9% uptime)
2. **Anomaly Detection**: Machine learning to detect unusual patterns
3. **Cost Attribution**: Track cloud costs by endpoint usage
4. **Dependency Mapping**: Visualize endpoint dependencies (A calls B calls C)
5. **Performance Budgets**: Set latency budgets per endpoint, alert on violations

### Phase 4 (Optional): Custom Dashboards

1. **Executive Dashboard**: High-level business metrics (uptime, throughput, cost)
2. **Developer Dashboard**: Per-module drilldown for dev teams
3. **Customer Success Dashboard**: User-facing metrics (signup rate, login success)
4. **SRE Dashboard**: Infrastructure health (CPU, memory, database connections)

## Conclusion

The hybrid observability system provides **instant visibility** into endpoint performance (Micrometer) AND **long-term trend analysis** (Grafana Cloud).

**Phase 1 Complete**: Micrometer dashboard with 100% endpoint coverage
**Phase 2 Pending**: Grafana Cloud integration (optional)

**Recommendation**: Use the Micrometer dashboard for daily operations. Add Grafana Cloud later if you need historical analysis, alerting, or SLA tracking.

---

**Last Updated**: 2026-01-01
**Status**: ✅ Phase 1 Complete (Micrometer), 🔄 Phase 2 Optional (Grafana)
