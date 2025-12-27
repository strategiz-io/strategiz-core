# Grafana Cloud Prometheus Query Setup

## Current Status

### ✅ Working
- **OTLP Metrics Export**: Metrics are being sent to Grafana Cloud
  - Endpoint: `https://otlp-gateway-prod-us-east-3.grafana.net/otlp`
  - Authentication: Configured with `GRAFANA_OTLP_AUTH` secret
  - Organization: 1623488
  - Region: prod-us-east-3

### ⚠️ Needs Configuration
- **Prometheus Query API**: Backend needs to query stored metrics
  - Current URL: `https://prometheus-1623488.grafana.net/api/prom` (updated)
  - Authentication: `GRAFANA_PROMETHEUS_AUTH` secret exists but may need verification
  - Status: HTTP 401/530 errors when querying

## Problem

The Grafana Cloud Access Token (`glc_...`) works for OTLP ingestion, but querying Prometheus metrics requires:
1. The correct Prometheus query endpoint URL for your stack
2. Proper authentication credentials (may be different from OTLP)

## Solution: Get Correct Prometheus Credentials

### Step 1: Log into Grafana Cloud

Visit: https://grafana.com/auth/sign-in

### Step 2: Find Your Prometheus Query Endpoint

1. Go to **Connections** → **Data Sources**
2. Find your **Prometheus** or **Mimir** data source
3. Copy the **URL** - it will look like:
   ```
   https://prometheus-prod-{instance-id}-prod-{region}.grafana.net
   ```

### Step 3: Create a Service Account Token for Prometheus

1. Go to **Administration** → **Service Accounts**
2. Click **Add service account**
3. Name: `strategiz-prometheus-query`
4. Role: **Viewer** (minimum) or **Editor**
5. Click **Add service account token**
6. Copy the token (format: `glsa_...`)

### Step 4: Update GCP Secret Manager

```bash
# Create the correct authentication format
# For Grafana Cloud, use: <instance-id>:<service-account-token>
# Then base64 encode it

# Example (replace with your actual values):
INSTANCE_ID="1623488"  # Your organization/instance ID
SERVICE_TOKEN="glsa_..."  # Token from Step 3

# Create base64 encoded Basic Auth
AUTH_HEADER=$(echo -n "${INSTANCE_ID}:${SERVICE_TOKEN}" | base64)

# Update the secret
echo -n "$AUTH_HEADER" | gcloud secrets versions add GRAFANA_PROMETHEUS_AUTH --data-file=-
```

### Step 5: Update Cloud Run Environment Variable

```bash
# Use the Prometheus URL from Step 2
PROM_URL="https://prometheus-prod-XX-prod-us-east-0.grafana.net/api/prom"

gcloud run services update strategiz-api \
  --region=us-east1 \
  --update-env-vars GRAFANA_PROMETHEUS_URL="$PROM_URL"
```

### Step 6: Verify Connection

```bash
# Test the Prometheus API
AUTH=$(gcloud secrets versions access latest --secret=GRAFANA_PROMETHEUS_AUTH)
PROM_URL="<your-prometheus-url-from-step-2>"

curl -H "Authorization: Basic $AUTH" \
  "${PROM_URL}/api/v1/query?query=up"

# Should return: {"status":"success","data":...}
```

## Alternative: Use Existing Grafana Cloud Token

If you want to use the same `glc_` token for both OTLP and Prometheus queries:

### Option A: Bearer Authentication

Some Grafana Cloud setups support Bearer token authentication:

```bash
# Decode the existing token
TOKEN=$(gcloud secrets versions access latest --secret=GRAFANA_OTLP_AUTH | base64 -d)

# Test with Bearer auth
curl -H "Authorization: Bearer $TOKEN" \
  "${PROM_URL}/api/v1/query?query=up"
```

If this works, update the backend to use Bearer instead of Basic auth.

### Option B: API Key Format

For Grafana Cloud Prometheus/Mimir queries:

```bash
# Format: <instance-id>:<glc-token>
# The glc token can be used as the password
echo -n "1623488:glc_eyJ..." | base64

# Update secret with this value
```

## Verification Script

Use the provided script to test connectivity:

```bash
cd /Users/cuztomizer/Documents/GitHub/strategiz-core
./scripts/verify-grafana-metrics.sh
```

## Expected Result

Once configured correctly, the observability dashboard will show:
- ✅ HTTP request metrics (requests/sec, latency percentiles)
- ✅ JVM metrics (memory, GC)
- ✅ Custom application metrics
- ✅ Cache performance
- ✅ Error rates

Access at: https://console.strategiz.io/observability

## Troubleshooting

### HTTP 401 - Authentication Failed
- Verify the service account token has **Viewer** or **Editor** role
- Check the format is correct: `<instance-id>:<token>` base64 encoded
- Ensure the token hasn't expired

### HTTP 530 - Bad Gateway
- The Prometheus URL is incorrect
- Double-check the URL from Grafana Cloud console
- Try the URL without `/api/prom` suffix first

### No Metrics Appearing
- Verify OTLP is sending metrics: Check Grafana Cloud → Explore → Metrics
- Ensure OpenTelemetry is enabled in `application-prod.properties`
- Check Cloud Run logs for OTLP export errors

## Reference Documentation

- [Grafana Cloud Authentication](https://grafana.com/docs/grafana-cloud/account-management/authentication-and-permissions/)
- [Prometheus Query API](https://prometheus.io/docs/prometheus/latest/querying/api/)
- [Grafana Service Accounts](https://grafana.com/docs/grafana/latest/administration/service-accounts/)
