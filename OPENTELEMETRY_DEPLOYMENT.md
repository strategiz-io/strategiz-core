# OpenTelemetry Deployment Guide

## Overview

This document tracks the deployment of OpenTelemetry Java agent to enable metrics export to Grafana Cloud.

## Changes Made

### 1. Dockerfile Updates
- Added OpenTelemetry Java agent v2.11.0 download
- Updated CMD to include `-javaagent:/app/opentelemetry-javaagent.jar` flag

```dockerfile
# Download OpenTelemetry Java agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.11.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

# Attach agent at runtime
CMD ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
```

### 2. Cloud Run Environment Variables

OpenTelemetry Java agent requires environment variables (not application.properties):

```bash
# OTLP Endpoint
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway-prod-us-east-3.grafana.net/otlp

# Protocol
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf

# Authentication (Grafana Cloud write token)
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic <base64_token>

# Service identification
OTEL_SERVICE_NAME=strategiz-core
OTEL_RESOURCE_ATTRIBUTES=service.namespace=strategiz;deployment.environment=production;service.version=1.0.0

# Exporters
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
OTEL_TRACES_EXPORTER=otlp
```

### 3. GCP Billing Module

Temporarily disabled due to credentials format issue:

```properties
# application-prod.properties
gcp.billing.enabled=false
gcp.billing.demo-mode=true
```

**Issue**: `Invalid base64 credentials in Vault` error in GcpBillingConfig
**To Fix**: Resolve base64 vs raw JSON credential format handling

## Grafana Cloud Configuration

### OTLP Ingestion (Write)
- **Endpoint**: `https://otlp-gateway-prod-us-east-3.grafana.net/otlp`
- **Auth**: Basic auth with write token + instance ID suffix
- **Token**: `strategiz-metrics-write` (stored in GCP Secret Manager as `GRAFANA_OTLP_AUTH`)

### Prometheus Query (Read)
- **Endpoint**: `https://prometheus-prod-66-prod-us-east-3.grafana.net/api/prom`
- **Auth**: Basic auth with read token
- **Token**: `strategiz-metrics-read` (stored in GCP Secret Manager as `GRAFANA_PROMETHEUS_AUTH`)

## Deployment Timeline

1. **Initial Build** (revision 00089): ❌ Failed
   - Error: Failed to connect to localhost:4318
   - Cause: Missing OTLP environment variables

2. **Second Build** (revision 00090): ❌ Failed  
   - Error: HTTP 401 from Grafana OTLP + GCP credentials error
   - Cause: Missing auth header + GCP billing config issue

3. **Third Build** (revision 00091): ❌ Failed
   - Error: GCP billing credentials prevented startup
   - Action: Disabled GCP billing module

4. **Fourth Build** (in progress): ⏳ Building
   - Commit: d8bbba88f
   - Changes: GCP billing disabled
   - Expected: Successful deployment with OpenTelemetry enabled

## Testing Metrics Export

After successful deployment:

1. Make test requests to generate metrics:
```bash
curl -I https://api.strategiz.io/health
curl https://api.strategiz.io/v1/some-endpoint
```

2. Wait 1-2 minutes for metrics to propagate

3. Query Prometheus API:
```bash
curl -H "Authorization: Basic <base64_read_token>" \
  "https://prometheus-prod-66-prod-us-east-3.grafana.net/api/prom/api/v1/label/__name__/values"
```

4. Check observability dashboard:
```
https://console.strategiz.io/observability
```

## Known Issues

### 1. GCP Billing Credentials Format
**Status**: Temporarily disabled  
**Error**: `Invalid base64 credentials in Vault`  
**Location**: `GcpBillingConfig.metricServiceClient()`  
**Fix Required**: Investigate VaultSecretService.readSecret() behavior - does it auto-decode base64?

### 2. OpenTelemetry Auth (To Verify)
**Status**: Configured, pending verification  
**Issue**: Previous revisions showed HTTP 401 from Grafana  
**Fix Applied**: Set `OTEL_EXPORTER_OTLP_HEADERS` with correct auth format  
**Test Required**: Verify metrics appear in Grafana after deployment

## Related Documentation

- [GRAFANA_PROMETHEUS_FIX.md](GRAFANA_PROMETHEUS_FIX.md) - Prometheus query authentication setup
- [GRAFANA_SETUP.md](GRAFANA_SETUP.md) - Grafana Cloud initial configuration
- [application-prod.properties](application-api/src/main/resources/application-prod.properties) - Production configuration

---

*Last updated: 2025-12-28*
*Current status: Build in progress (cad10e4a-5224-48cb-964d-d538520bb2b5)*
