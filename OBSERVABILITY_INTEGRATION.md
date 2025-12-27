# Observability Integration - Complete Architecture

## Overview

This document describes the complete observability architecture for Strategiz, integrating both Java and Python services with Grafana Cloud, and exposing metrics via custom REST APIs for the console.

## Architecture

```
┌─────────────────┐         ┌─────────────────┐
│  Java Services  │         │ Python Services │
│  (Spring Boot)  │         │    (gRPC)       │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │ Micrometer/OTLP          │ OpenTelemetry/OTLP
         └───────────┬───────────────┘
                     ↓
         ┌───────────────────────┐
         │   Grafana Cloud OTLP  │
         │  Gateway (Mimir/Tempo)│
         └───────────┬───────────┘
                     │
         ┌───────────▼────────────────────────┐
         │  Metrics Storage & Dashboards      │
         │  - Mimir (Prometheus metrics)      │
         │  - Tempo (Distributed traces)      │
         │  - Loki (Logs)                     │
         │  - Grafana (Dashboards)            │
         └───────────┬────────────────────────┘
                     │
         ┌───────────▼───────────┐
         │  Java REST API Layer  │
         │  GrafanaMetricsClient │
         │  ↓ Queries Prometheus │
         │  AdminObservability   │
         │  Controller           │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │   Console UI (React)  │
         │   - ExecutionMetrics  │
         │   - Real-time charts  │
         └───────────────────────┘
```

## Components

### 1. Metrics Export (OpenTelemetry)

**Java Services** (`service-monitoring/`):
- Uses Micrometer with OTLP exporter
- Configuration: `application-prod.properties`
- Custom metrics: `StrategizMetrics.java`
- Exports to: `${GRAFANA_OTLP_ENDPOINT}`

**Python Execution Service** (`application-strategy-execution/`):
- Uses OpenTelemetry Python SDK
- Configuration: `observability.py`
- Custom metrics: execution time, cache hits, errors
- Exports to: Same `${GRAFANA_OTLP_ENDPOINT}`

### 2. Metrics Storage (Grafana Cloud)

- **OTLP Endpoint**: `https://otlp-gateway-prod-us-central-0.grafana.net/otlp`
- **Authentication**: Basic Auth (Instance ID:API Token as Base64)
- **Storage**: Grafana Mimir (Prometheus-compatible)
- **Retention**: 14 days (free tier)

### 3. Metrics Query API (Java)

**New Components Created:**

1. **GrafanaMetricsClient** (`service-monitoring/client/GrafanaMetricsClient.java`)
   - Queries Grafana Prometheus API
   - Methods: `query()`, `queryRange()`
   - Returns structured metric results

2. **ExecutionMetricsService** (`service-monitoring/service/ExecutionMetricsService.java`)
   - Business logic for execution metrics
   - Methods:
     - `getExecutionHealth()` - Current health snapshot
     - `getExecutionLatency(duration)` - Latency percentiles over time
     - `getExecutionThroughput(duration)` - Request rates by status
     - `getCachePerformance(duration)` - Cache metrics
     - `getExecutionErrors(duration)` - Errors by type

3. **AdminObservabilityController** (`service-console/controller/AdminObservabilityController.java`)
   - REST endpoints at `/v1/console/observability/execution/*`
   - Endpoints:
     - `GET /health` - Health metrics
     - `GET /latency?durationMinutes=60` - Latency data
     - `GET /throughput?durationMinutes=60` - Throughput data
     - `GET /cache?durationMinutes=60` - Cache data
     - `GET /errors?durationMinutes=60` - Error data

### 4. Console UI Integration

**New Component:**

`ExecutionMetrics.tsx` - React component that:
- Fetches metrics from REST API endpoints
- Displays real-time health cards
- Renders latency, throughput, and cache charts using Recharts
- Auto-refreshes every 60 seconds

**Updated:**

`ConsoleObservabilityScreen.tsx`:
- "Strategy Execution" tab now uses `ExecutionMetrics` component
- No longer uses Grafana iframe embed
- Fully custom UI with API-driven data

## Configuration

### Environment Variables

**Java App** (`application-api/application-prod.properties`):
```properties
# OTLP Export (metrics collection)
otel.exporter.otlp.endpoint=${GRAFANA_OTLP_ENDPOINT:https://otlp-gateway-prod-us-central-0.grafana.net/otlp}
otel.exporter.otlp.headers=Authorization=Basic ${GRAFANA_OTLP_AUTH_HEADER:}

# Prometheus Query API (metrics retrieval)
grafana.prometheus.url=${GRAFANA_PROMETHEUS_URL:https://prometheus-prod-01-eu-west-0.grafana.net/api/prom}
grafana.prometheus.auth=${GRAFANA_PROMETHEUS_AUTH:}
```

**Python Service** (`cloudbuild-execution.yaml`):
```yaml
- '--set-env-vars'
- 'GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-us-central-0.grafana.net/otlp'
- '--update-secrets'
- 'GRAFANA_OTLP_AUTH_HEADER=GRAFANA_OTLP_AUTH:latest'
```

### Secrets (GCP Secret Manager)

**Required Secrets:**

1. **GRAFANA_OTLP_AUTH** - Base64 encoded credentials for OTLP export
   - Format: `echo -n "<instance_id>:<api_token>" | base64`
   - Used by: Both Java and Python services

2. **GRAFANA_PROMETHEUS_AUTH** - Base64 encoded credentials for Prometheus API
   - Format: Same as GRAFANA_OTLP_AUTH
   - Used by: Java API for querying metrics

## Setup Instructions

### Step 1: Ensure Grafana Cloud Credentials Are Stored

```bash
# Check if secret exists
gcloud secrets list | grep GRAFANA_OTLP_AUTH

# If not, create it (get credentials from Grafana Cloud Portal)
# Instance ID from: https://grafana.com/orgs/yourorg/stacks
# API Token from: Security → Service Accounts → Create token with MetricsPublisher role

INSTANCE_ID="your-instance-id"
API_TOKEN="your-api-token"

# Create base64 encoded auth header
AUTH_HEADER=$(echo -n "${INSTANCE_ID}:${API_TOKEN}" | base64)

# Store in Secret Manager
echo -n "${AUTH_HEADER}" | gcloud secrets create GRAFANA_OTLP_AUTH \
  --data-file=- \
  --replication-policy=automatic
```

### Step 2: Grant Access to Cloud Run Services

```bash
# Java API service
gcloud secrets add-iam-policy-binding GRAFANA_OTLP_AUTH \
  --member="serviceAccount:43628135674-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Python execution service (if different service account)
gcloud secrets add-iam-policy-binding GRAFANA_OTLP_AUTH \
  --member="serviceAccount:43628135674-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### Step 3: Deploy Python Execution Service

```bash
# Build and deploy using Cloud Build
gcloud builds submit \
  --config=cloudbuild-execution.yaml \
  --substitutions=_SERVICE_NAME=strategiz-execution
```

The deployment will automatically:
- Mount the `GRAFANA_OTLP_AUTH` secret
- Export metrics to Grafana Cloud
- Enable sub-100ms performance with caching

### Step 4: Deploy Java API Service

```bash
# Your existing deployment process
# Ensure GRAFANA_OTLP_AUTH and GRAFANA_PROMETHEUS_AUTH secrets are mounted
```

### Step 5: Verify Metrics Flow

1. **Check Python service logs:**
   ```bash
   gcloud run services logs read strategiz-execution --region=us-east1 --limit=50
   ```

   Look for:
   - `✅ Tracing configured: https://otlp-gateway...`
   - `✅ Metrics configured: https://otlp-gateway...`

2. **Test execution service:**
   ```bash
   # Run a test strategy execution
   python3 test-complex-strategy.py
   ```

3. **Check Grafana Cloud:**
   - Go to Grafana Cloud → Explore
   - Query: `strategy_execution_time`
   - Should see data points (may take 1-2 minutes)

4. **Test REST API:**
   ```bash
   # Get execution health metrics
   curl -H "Authorization: Bearer $YOUR_TOKEN" \
     https://api.strategiz.io/v1/console/observability/execution/health
   ```

5. **Verify Console:**
   - Open console → Observability → Strategy Execution tab
   - Should see health metrics, charts loading

## Metrics Reference

### Python Execution Service Metrics

All metrics prefixed with `strategy_`:

| Metric | Type | Description |
|--------|------|-------------|
| `strategy_execution_time` | Histogram | Execution time in milliseconds |
| `strategy_compilation_time` | Histogram | Code compilation time in milliseconds |
| `strategy_cache_hits` | Counter | Number of cache hits |
| `strategy_cache_misses` | Counter | Number of cache misses |
| `strategy_requests_total` | Counter | Total requests (labeled by status) |
| `strategy_errors_total` | Counter | Total errors (labeled by error_type) |
| `strategy_executions_active` | Gauge | Current active executions |
| `strategy_market_data_size` | Histogram | Market data bars processed |

### PromQL Queries Used

**Success Rate:**
```promql
1 - (sum(rate(strategy_requests_total{status="failure"}[5m])) / sum(rate(strategy_requests_total[5m])))
```

**P99 Latency:**
```promql
histogram_quantile(0.99, sum(rate(strategy_execution_time_bucket[5m])) by (le))
```

**Cache Hit Rate:**
```promql
sum(rate(strategy_cache_hits[5m])) / (sum(rate(strategy_cache_hits[5m])) + sum(rate(strategy_cache_misses[5m])))
```

## Benefits of This Architecture

### ✅ No Duplication
- Single Grafana Cloud instance for all services
- Reuses existing OTLP configuration
- Unified secret management

### ✅ Custom UI Control
- Full control over dashboard layout
- Consistent with Strategiz design system
- No iframe security/CSP issues
- Responsive mobile-friendly charts

### ✅ API-Driven
- RESTful endpoints for all metrics
- Can be consumed by mobile apps, CLI tools
- Easy to add new metrics/charts
- Cacheable responses

### ✅ Cost Effective
- Single Grafana Cloud stack (free tier)
- Prometheus queries are free
- No duplicate metric storage

### ✅ Extensible
- Easy to add more services (Auth, Providers, etc.)
- Pattern established for future integrations
- Can add alerting, SLOs, etc.

## Next Steps

1. **Add More Metric Endpoints:**
   - Auth service metrics (`/observability/auth/*`)
   - Provider integration metrics (`/observability/providers/*`)
   - API performance metrics (`/observability/api/*`)

2. **Implement Alerting:**
   - Create Grafana alerts for critical metrics
   - Integrate with notification channels (Slack, email)

3. **Add SLO Tracking:**
   - Define SLOs (e.g., 99.9% availability, p95 < 50ms)
   - Track error budgets

4. **Performance Optimization:**
   - Cache Prometheus query results (5-60s TTL)
   - Implement query result aggregation
   - Add request batching

## Troubleshooting

### Metrics Not Appearing in Console

1. **Check Java service logs:**
   ```bash
   gcloud run services logs read strategiz-api --limit=50
   ```
   Look for errors from `GrafanaMetricsClient`

2. **Test Prometheus API directly:**
   ```bash
   curl -u "<instance_id>:<api_token>" \
     "https://prometheus-prod-01-eu-west-0.grafana.net/api/prom/api/v1/query?query=up"
   ```

3. **Verify secrets are mounted:**
   ```bash
   gcloud run services describe strategiz-api \
     --format="value(spec.template.spec.containers[0].env)"
   ```

### High Latency on Metrics API

- Add caching to `ExecutionMetricsService`
- Reduce query range (e.g., 15min instead of 60min)
- Use coarser step intervals for range queries

### CORS Errors in Console

- Ensure API allows console origin in CORS config
- Check network tab for actual error response

## Files Changed

### Java (strategiz-core)
- `service-monitoring/client/GrafanaMetricsClient.java` (new)
- `service-monitoring/service/ExecutionMetricsService.java` (new)
- `service-console/controller/AdminObservabilityController.java` (new)
- `application-api/resources/application-prod.properties` (updated)
- `cloudbuild-execution.yaml` (updated)

### React (strategiz-ui)
- `apps/console/components/observability/ExecutionMetrics.tsx` (new)
- `apps/console/screens/ConsoleObservabilityScreen.tsx` (updated)

### Documentation
- `OBSERVABILITY_INTEGRATION.md` (new - this file)
- `GRAFANA_SETUP.md` (existing - still valid for dashboard JSON)

---

**Summary:** Complete observability integration that reuses existing Grafana Cloud infrastructure, exposes metrics via custom REST APIs, and provides a fully custom UI in the console. No duplication, API-driven, and ready for production.
