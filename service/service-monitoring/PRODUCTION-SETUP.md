# Production Observability Setup - Grafana Cloud

This guide covers deploying the Strategiz observability stack to production using Grafana Cloud.

## Overview

**Stack:** Grafana Cloud (Managed LGTM)
- **Metrics:** Mimir (via OTLP)
- **Logs:** Loki (via OTLP)
- **Traces:** Tempo (via OTLP)
- **Dashboards:** Grafana Cloud

**Free Tier Limits:**
- 50GB logs/month
- 10,000 active metrics series
- 50GB traces/month
- 14-day retention

---

## Step 1: Create Grafana Cloud Account

1. Sign up at [grafana.com/products/cloud](https://grafana.com/products/cloud/)
2. Create a new stack (choose region closest to your Cloud Run deployment)
3. Note your stack URL (e.g., `yourorg.grafana.net`)

---

## Step 2: Get OTLP Credentials

1. Go to **Grafana Cloud Portal** → **My Account**
2. Find your stack and click **Details**
3. Note the **OTLP Gateway URL**:
   ```
   https://otlp-gateway-prod-us-central-0.grafana.net/otlp
   ```
4. Note your **Instance ID** (numeric value)

### Create API Token

1. Go to **Security** → **Service Accounts**
2. Create a new service account: `strategiz-otlp`
3. Add role: **MetricsPublisher**
4. Generate a token and save it securely

---

## Step 3: Store Credentials in Vault

Run the setup script:

```bash
# Set Vault address (production)
export VAULT_ADDR=https://strategiz-vault-43628135674.us-central1.run.app
export VAULT_TOKEN=<your-vault-token>

# Run the script
./service/service-monitoring/scripts/load-grafana-cloud-secrets.sh
```

The script will prompt for:
- OTLP Gateway URL
- Instance ID
- API Token

---

## Step 4: Deploy to Cloud Run

### Option A: Using gcloud CLI

```bash
# Build and push image
gcloud builds submit --tag gcr.io/strategiz-io/strategiz-core:latest

# Deploy with observability config
gcloud run deploy strategiz-api \
  --image gcr.io/strategiz-io/strategiz-core:latest \
  --region us-central1 \
  --platform managed \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars "GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-us-central-0.grafana.net/otlp" \
  --set-env-vars "GRAFANA_OTLP_AUTH_HEADER=<base64-auth-header>" \
  --set-env-vars "OTEL_SAMPLING_RATIO=0.1"
```

### Option B: Using Cloud Build (cloudbuild.yaml)

Add to your `cloudbuild.yaml`:

```yaml
substitutions:
  _GRAFANA_OTLP_ENDPOINT: 'https://otlp-gateway-prod-us-central-0.grafana.net/otlp'

steps:
  - name: 'gcr.io/cloud-builders/gcloud'
    args:
      - 'run'
      - 'deploy'
      - 'strategiz-api'
      - '--image=gcr.io/$PROJECT_ID/strategiz-core:$COMMIT_SHA'
      - '--region=us-central1'
      - '--set-env-vars=SPRING_PROFILES_ACTIVE=prod'
      - '--set-env-vars=GRAFANA_OTLP_ENDPOINT=${_GRAFANA_OTLP_ENDPOINT}'
      - '--update-secrets=GRAFANA_OTLP_AUTH_HEADER=grafana-otlp-auth:latest'
```

---

## Step 5: Import Dashboards to Grafana Cloud

1. Go to your Grafana Cloud instance
2. Navigate to **Dashboards** → **Import**
3. Import each dashboard JSON from:
   ```
   service/service-monitoring/dashboards/grafana/dashboards/
   ```

   Dashboards to import:
   - `strategiz-overview.json` - Service overview
   - `auth-identity.json` - Authentication monitoring
   - `api-performance.json` - API latency and throughput
   - `provider-integrations.json` - Provider health
   - `jvm-infrastructure.json` - JVM metrics
   - `security-audit.json` - Security events

4. Update datasource UIDs in each dashboard to match your Grafana Cloud datasources

---

## Step 6: Configure Alerting

1. Go to **Alerting** → **Alert rules**
2. Import alert rules from:
   ```
   service/service-monitoring/infrastructure/prometheus/rules/strategiz-alerts.yml
   ```
3. Configure notification channels (Slack, PagerDuty, Email, etc.)

---

## Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `GRAFANA_OTLP_ENDPOINT` | Grafana Cloud OTLP gateway URL | `https://otlp-gateway-prod-us-central-0.grafana.net/otlp` |
| `GRAFANA_OTLP_AUTH_HEADER` | Base64(instanceId:apiToken) | `MTIzNDU2OmdsY19leUo...` |
| `OTEL_SAMPLING_RATIO` | Trace sampling rate (0.0-1.0) | `0.1` (10%) |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |

---

## Sampling Strategy

| Environment | Sampling Rate | Rationale |
|-------------|---------------|-----------|
| Development | 100% | Full visibility for debugging |
| Staging | 50% | Balance visibility and cost |
| Production | 10% | Cost optimization, sample errors 100% |

Production uses tail-based sampling in OTel Collector to ensure:
- 100% of errors are captured
- 100% of slow requests (>2s) are captured
- 10% probabilistic sampling for normal requests

---

## Cost Estimation (Grafana Cloud Free Tier)

Assuming 1M requests/month:
- **Metrics:** ~500 active series (well under 10K limit)
- **Traces:** ~100K traces at 10% sampling (~5GB, under 50GB limit)
- **Logs:** ~2GB/month (under 50GB limit)

**Result:** Stays within free tier for most startups

---

## Troubleshooting

### No Data in Grafana Cloud

1. Check OTLP endpoint is correct
2. Verify auth header is Base64 encoded correctly
3. Check Cloud Run logs for OTLP export errors:
   ```bash
   gcloud logging read "resource.type=cloud_run_revision AND textPayload:otlp" --limit=50
   ```

### High Cardinality Warnings

Reduce metric cardinality by:
1. Limiting URI labels (use path templates, not actual paths)
2. Reducing custom dimensions
3. Aggregating in OTel Collector before export

### Missing Traces

1. Verify sampling ratio isn't too low
2. Check propagation headers in requests
3. Ensure all services have tracing enabled

---

## Grafana Cloud URLs

After setup, access your observability stack at:

| Tool | URL |
|------|-----|
| Grafana Dashboards | `https://yourorg.grafana.net` |
| Explore (Logs) | `https://yourorg.grafana.net/explore?orgId=1&left=["loki"]` |
| Explore (Traces) | `https://yourorg.grafana.net/explore?orgId=1&left=["tempo"]` |
| Alerting | `https://yourorg.grafana.net/alerting` |
