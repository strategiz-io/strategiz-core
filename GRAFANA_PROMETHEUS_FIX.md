# Grafana Prometheus Query Fix - Action Required

## Current Status

### ✅ Working
- **OTLP Metrics Export**: Successfully sending metrics to Grafana Cloud
  - Endpoint: `https://otlp-gateway-prod-us-east-3.grafana.net/otlp`
  - Region: `prod-us-east-3`
  - Instance ID: `1475708`
  - Org ID: `1623488`
  - Authentication: ✅ Working with OTLP token

### ❌ Not Working  
- **Prometheus Query API**: Cannot read metrics from Grafana Cloud
  - Current URL: `https://prometheus-1623488.grafana.net/api/prom` (uses org ID - WRONG)
  - Current Auth: Same as OTLP token (has write scope, not read scope)
  - Error: `"authentication error: invalid scope requested"`

## Root Cause

The OTLP ingestion token (`glc_...`) has **write** permissions for sending metrics but does NOT have **read** permissions for querying metrics via Prometheus API.

**Evidence from testing:**
```bash
# Testing with OTLP token (instance:token format)
curl -H "Authorization: Basic <otlp-token>" \
  "https://prometheus-prod-13-prod-us-east-0.grafana.net/api/prom/api/v1/query?query=up"

Response: {"status":"error","error":"authentication error: invalid scope requested"}
```

## Solution: Create Separate Service Account for Queries

You need to create a Grafana Cloud Service Account with **Viewer** or **MetricsPublisher** role for reading metrics.

### Step 1: Log into Grafana Cloud

Visit: https://grafana.com/auth/sign-in

### Step 2: Find Your Prometheus/Mimir Datasource URL

1. Go to **Connections** → **Data Sources**
2. Find your **Mimir** or **Prometheus** datasource (default name might be "grafanacloud-<org>-prom")
3. Copy the **URL** - it should look like:
   ```
   https://prometheus-prod-XX-prod-<region>.grafana.net
   ```
   
   Example from testing: `https://prometheus-prod-13-prod-us-east-0.grafana.net`
   
   **Note**: The region might be `us-east-0` even though OTLP uses `us-east-3` (Grafana Cloud uses different regions for ingestion vs storage).

### Step 3: Create Service Account for Queries

1. Go to **Administration** → **Service Accounts**
2. Click **Add service account**
3. Name: `strategiz-prometheus-query`
4. Role: **Viewer** (for read-only) or **MetricsPublisher** (for read/write)
5. Click **Add service account token**
6. Name the token: `strategiz-backend-query`
7. Copy the generated token (format: `glsa_...`)

### Step 4: Update GCP Secrets

```bash
# Get the Prometheus URL from Step 2 (example below)
PROM_URL="https://prometheus-prod-13-prod-us-east-0.grafana.net/api/prom"

# Get the service account token from Step 3
SERVICE_TOKEN="glsa_XXXXXXXXXXXXX"  # Replace with actual token

# Get your instance ID (from Grafana Cloud stack settings)
INSTANCE_ID="1475708"  # Your instance ID

# Create Basic Auth: <instance-id>:<service-account-token>
PROM_AUTH=$(echo -n "${INSTANCE_ID}:${SERVICE_TOKEN}" | base64)

# Update the secret
echo -n "$PROM_AUTH" | gcloud secrets versions add GRAFANA_PROMETHEUS_AUTH --data-file=-
```

### Step 5: Update Cloud Run Environment

```bash
# Use the URL from Step 2
gcloud run services update strategiz-api \
  --region=us-east1 \
  --update-env-vars GRAFANA_PROMETHEUS_URL="$PROM_URL"
```

### Step 6: Verify

```bash
# Test the connection
PROM_AUTH=$(gcloud secrets versions access latest --secret=GRAFANA_PROMETHEUS_AUTH)
PROM_URL="<your-url-from-step-2>"

curl -H "Authorization: Basic $PROM_AUTH" \
  "${PROM_URL}/api/v1/query?query=up"

# Expected success response:
# {"status":"success","data":{"resultType":"vector","result":[...]}}
```

## Current Configuration Summary

**Cloud Run Environment:**
- GRAFANA_OTLP_ENDPOINT: `https://otlp-gateway-prod-us-east-3.grafana.net/otlp` ✅
- GRAFANA_PROMETHEUS_URL: `https://prometheus-1623488.grafana.net/api/prom` ❌ (needs update)

**GCP Secrets:**
- GRAFANA_OTLP_AUTH: `glc_...:1475708` ✅ (working for metric ingestion)
- GRAFANA_PROMETHEUS_AUTH: Same as OTLP ❌ (needs separate service account token)

**Your Grafana Cloud Details:**
- Organization ID: `1623488`
- Instance ID: `1475708`
- Region: `prod-us-east-3` (OTLP)
- Prometheus Region: Likely `prod-us-east-0` (to be confirmed in Step 2)

## Testing Results

| Endpoint Format | Region | Auth Format | Result |
|----------------|--------|-------------|---------|
| prometheus-1623488.grafana.net | N/A | org:key | DNS Error 1016 |
| prometheus-1475708.grafana.net | N/A | instance:token | DNS Error 1016 |
| prometheus-prod-13-prod-us-east-0 | us-east-0 | instance:token | ❌ Invalid scope |
| prometheus-prod-13-prod-us-east-3 | us-east-3 | instance:token | DNS Error 1016 |

**Conclusion**: The correct endpoint exists in `us-east-0` region (not `us-east-3`). Authentication format is correct (`instance:token`) but the OTLP token doesn't have read permissions.

## Alternative: Query via Grafana UI (Temporary Workaround)

While waiting for proper API access, you can view metrics in Grafana Cloud directly:

1. Go to https://grafana.com
2. Navigate to **Explore**
3. Select your Prometheus/Mimir datasource
4. Query: `http_server_requests_seconds_count`
5. This will show the metrics that are already being ingested successfully

## Expected Outcome

Once the service account token is configured:

1. **Backend**: AdminObservabilityController endpoints will return real metrics
   - `/v1/console/observability/execution/health`
   - `/v1/console/observability/execution/latency`
   - `/v1/console/observability/execution/throughput`
   - `/v1/console/observability/execution/cache`
   - `/v1/console/observability/execution/errors`

2. **Frontend**: Console dashboard will display charts
   - Success rate, requests/sec, p99 latency
   - Latency percentiles over time (p50, p95, p99)
   - Request throughput by status
   - Cache hit/miss rates
   - Error rates by type

3. **Access**: https://console.strategiz.io/observability

## Next Steps

1. ⏳ **User Action Required**: Follow Steps 1-6 above
2. ✅ Backend is ready to query metrics (code already deployed)
3. ✅ Frontend is ready to display metrics (already deployed)
4. ⏳ Waiting for proper Prometheus credentials

---

**Questions?**
- Grafana Cloud Docs: https://grafana.com/docs/grafana-cloud/
- Prometheus API: https://prometheus.io/docs/prometheus/latest/querying/api/
- Service Accounts: https://grafana.com/docs/grafana/latest/administration/service-accounts/
