# GCP Billing API Setup Guide

This guide walks you through setting up GCP Billing API access for the Strategiz costs dashboard.

## Prerequisites

- Access to Google Cloud Console with billing admin permissions
- Project: `strategiz-io`
- Vault running and accessible
- Admin access to create service accounts

## Step 1: Enable Required APIs

Navigate to [Google Cloud Console](https://console.cloud.google.com) and enable these APIs:

```bash
# Using gcloud CLI (recommended)
gcloud services enable cloudbilling.googleapis.com
gcloud services enable cloudresourcemanager.googleapis.com
gcloud services enable cloudasset.googleapis.com
gcloud services enable monitoring.googleapis.com
```

Or enable manually:
1. Go to: https://console.cloud.google.com/apis/library
2. Search for and enable:
   - **Cloud Billing API** (cloudbilling.googleapis.com)
   - **Cloud Resource Manager API** (cloudresourcemanager.googleapis.com)
   - **Cloud Asset API** (cloudasset.googleapis.com)
   - **Cloud Monitoring API** (monitoring.googleapis.com)

## Step 2: Set Up Billing Export to BigQuery (Recommended)

For detailed cost analysis, export billing data to BigQuery:

1. **Navigate to Billing Export**:
   - Go to: https://console.cloud.google.com/billing
   - Select your billing account
   - Click "Billing export" in left menu

2. **Enable Standard Usage Cost Export**:
   - Click "EDIT SETTINGS" for "Standard usage cost"
   - Select or create a BigQuery dataset:
     - Dataset ID: `billing_export`
     - Location: `US` (multi-region)
   - Click "SAVE"

3. **Enable Detailed Usage Cost Export** (Optional):
   - Same steps as above
   - More granular data, slightly higher storage costs

## Step 3: Create Service Account

Create a dedicated service account for billing access:

```bash
# Set your project
gcloud config set project strategiz-io

# Create service account
gcloud iam service-accounts create strategiz-billing \
  --display-name "Strategiz Billing API Access" \
  --description "Service account for accessing GCP billing data and infrastructure costs"

# Get the service account email
SERVICE_ACCOUNT_EMAIL=$(gcloud iam service-accounts list \
  --filter="displayName:Strategiz Billing API Access" \
  --format="value(email)")

echo "Service Account Email: $SERVICE_ACCOUNT_EMAIL"
```

## Step 4: Grant IAM Permissions

Grant the service account necessary permissions:

```bash
# Project-level permissions
gcloud projects add-iam-policy-binding strategiz-io \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/billing.viewer"

gcloud projects add-iam-policy-binding strategiz-io \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/cloudasset.viewer"

gcloud projects add-iam-policy-binding strategiz-io \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/monitoring.viewer"

# If using BigQuery export, grant BigQuery permissions
gcloud projects add-iam-policy-binding strategiz-io \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/bigquery.dataViewer"

gcloud projects add-iam-policy-binding strategiz-io \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/bigquery.jobUser"
```

### Required Roles Explained:

- **Billing Viewer** (`roles/billing.viewer`): Read billing data
- **Cloud Asset Viewer** (`roles/cloudasset.viewer`): List GCP resources for infrastructure inventory
- **Monitoring Viewer** (`roles/monitoring.viewer`): Access Firestore and Cloud Run metrics
- **BigQuery Data Viewer** (`roles/bigquery.dataViewer`): Read billing export data (if using BigQuery)
- **BigQuery Job User** (`roles/bigquery.jobUser`): Run queries on billing data

## Step 5: Create and Download Service Account Key

```bash
# Create key and download as JSON
gcloud iam service-accounts keys create ~/strategiz-billing-key.json \
  --iam-account=$SERVICE_ACCOUNT_EMAIL

# Display the key location
echo "Service account key saved to: ~/strategiz-billing-key.json"

# Show key content (you'll need this for Vault)
cat ~/strategiz-billing-key.json
```

**⚠️ Security Warning**: This key grants access to billing data. Keep it secure!

## Step 6: Get Your Billing Account ID

```bash
# List billing accounts
gcloud billing accounts list

# Output example:
# ACCOUNT_ID            NAME                OPEN  MASTER_ACCOUNT_ID
# 01AB23-CD45EF-67890G  My Billing Account  True
```

Copy the `ACCOUNT_ID` (format: `XXXXXX-XXXXXX-XXXXXX`)

## Step 7: Configure Vault Secrets

Store credentials in HashiCorp Vault:

### Production Vault (https://strategiz-vault-43628135674.us-east1.run.app)

```bash
# Set Vault environment
export VAULT_ADDR="https://strategiz-vault-43628135674.us-east1.run.app"
export VAULT_TOKEN="hvs.q2Lg7uILKNkEs20UA8mbT9Cr"  # Your actual token

# Store GCP billing credentials
vault kv put secret/strategiz/gcp-billing \
  project-id="strategiz-io" \
  billing-account-id="YOUR_BILLING_ACCOUNT_ID" \
  credentials="$(cat ~/strategiz-billing-key.json | base64)" \
  bigquery-dataset="billing_export" \
  bigquery-table="gcp_billing_export_v1_XXXXXX_XXXXXX_XXXXXX"

# Verify
vault kv get secret/strategiz/gcp-billing
```

**Important**: Replace:
- `YOUR_BILLING_ACCOUNT_ID` with your actual billing account ID from Step 6
- `gcp_billing_export_v1_XXXXXX...` with your actual BigQuery table name

### Local Development Vault (http://localhost:8200)

```bash
# For local testing with production data
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="root"

vault kv put secret/strategiz/gcp-billing \
  project-id="strategiz-io" \
  billing-account-id="YOUR_BILLING_ACCOUNT_ID" \
  credentials="$(cat ~/strategiz-billing-key.json | base64)" \
  bigquery-dataset="billing_export" \
  bigquery-table="gcp_billing_export_v1_XXXXXX_XXXXXX_XXXXXX"
```

## Step 8: Configure TimescaleDB Billing Credentials

Your TimescaleDB credentials are already in Vault. Verify they're correct:

```bash
export VAULT_ADDR="https://strategiz-vault-43628135674.us-east1.run.app"
export VAULT_TOKEN="hvs.q2Lg7uILKNkEs20UA8mbT9Cr"

# Check TimescaleDB production credentials
vault kv get secret/strategiz/timescale-production
```

Expected fields:
- `jdbc-url`: Connection string
- `username`: Database username
- `password`: Database password

## Step 9: Update Application Configuration

The configuration is already set! Just verify in production:

### Production (`application-prod.properties`)

```properties
# GCP Billing - already enabled in production
gcp.billing.enabled=true
gcp.billing.demo-mode=false

# Vault configuration (already configured)
strategiz.vault.enabled=true
strategiz.vault.address=https://strategiz-vault-43628135674.us-east1.run.app
```

### Verify in Code

The backend already has the Vault integration code. Check these files:

- `client-gcp-billing/src/main/java/io/strategiz/client/gcpbilling/GcpBillingVaultConfig.java`
- `client-timescale-billing/src/main/java/io/strategiz/client/timescalebilling/TimescaleVaultConfig.java`

## Step 10: Deploy and Test

### Local Testing (with production credentials)

```bash
cd /Users/cuztomizer/Documents/GitHub/strategiz-core

# Start local Vault and load production credentials (from Step 7)
vault server -dev

# In another terminal, run the app
export VAULT_TOKEN=root
mvn spring-boot:run -pl application-api -Dspring.profiles.active=dev
```

### Production Deployment

```bash
# Build and deploy to Cloud Run
gcloud builds submit --config cloudbuild.yaml

# The deployed service will automatically:
# 1. Connect to production Vault
# 2. Load GCP billing credentials
# 3. Load TimescaleDB credentials
# 4. Enable costs dashboard
```

### Verify It's Working

1. **Check Backend Logs**:
   ```bash
   gcloud run services logs read strategiz-api --limit=50
   ```

2. **Test Costs Endpoint**:
   ```bash
   # Get a valid session cookie first (login via UI)
   # Then test the endpoint
   curl -X GET "https://api.strategiz.io/v1/console/costs/summary" \
     -H "Cookie: strategiz-access-token=YOUR_TOKEN" \
     -H "Content-Type: application/json"
   ```

3. **Open Console Dashboard**:
   - Navigate to: https://console.strategiz.io/costs
   - Should see real data instead of "View Demo Data" button

## Troubleshooting

### Issue: "Permission denied" errors

**Solution**: Verify IAM roles are correctly assigned:
```bash
gcloud projects get-iam-policy strategiz-io \
  --flatten="bindings[].members" \
  --filter="bindings.members:strategiz-billing@*"
```

### Issue: "Billing API not enabled"

**Solution**: Re-run Step 1 to enable all required APIs

### Issue: "Cannot decode credentials from Vault"

**Solution**: Verify the service account key is base64 encoded:
```bash
# Test decoding
vault kv get -field=credentials secret/strategiz/gcp-billing | base64 -d | jq .
```

Should show valid JSON with `type: "service_account"`

### Issue: "No billing data found"

**Possible causes**:
1. **Billing export not set up**: Wait 24 hours after enabling (data is delayed)
2. **Wrong table name**: Verify BigQuery table exists and matches config
3. **Wrong billing account ID**: Double-check the account ID from Step 6

**Quick fix**: Use Cloud Billing API directly (doesn't require BigQuery):
```bash
vault kv patch secret/strategiz/gcp-billing \
  use-billing-api=true \
  use-bigquery=false
```

### Issue: Service account key rotation

For security, rotate keys every 90 days:
```bash
# Delete old key
gcloud iam service-accounts keys list \
  --iam-account=$SERVICE_ACCOUNT_EMAIL

gcloud iam service-accounts keys delete KEY_ID \
  --iam-account=$SERVICE_ACCOUNT_EMAIL

# Create new key and update Vault (repeat Steps 5-7)
```

## Cost Optimization

The costs dashboard itself incurs minimal costs:

- **Cloud Billing API**: FREE (no quota limits)
- **Cloud Asset API**: FREE up to 60 requests/minute
- **Cloud Monitoring API**: FREE for basic metrics
- **BigQuery Storage**: ~$0.02/GB/month (billing data is typically < 1GB)
- **BigQuery Queries**: ~$5/TB scanned (costs dashboard uses < 1GB/month)

**Estimated monthly cost**: < $1/month

## Security Best Practices

1. **Rotate service account keys** every 90 days
2. **Use least-privilege IAM roles** (only viewer roles needed)
3. **Store credentials in Vault** (never in code or environment variables)
4. **Enable audit logging** for billing data access:
   ```bash
   gcloud logging read "protoPayload.serviceName=cloudbilling.googleapis.com"
   ```
5. **Monitor service account usage**:
   ```bash
   gcloud logging read "protoPayload.authenticationInfo.principalEmail=$SERVICE_ACCOUNT_EMAIL" \
     --limit 50
   ```

## Next Steps

After setup is complete:

1. ✅ Test locally with production credentials
2. ✅ Deploy to Cloud Run production
3. ✅ Verify costs dashboard shows real data
4. ✅ Set up budget alerts in GCP Console (optional)
5. ✅ Configure cost anomaly detection (optional)

## Support

- **GCP Billing Documentation**: https://cloud.google.com/billing/docs
- **Cloud Asset API**: https://cloud.google.com/asset-inventory/docs
- **BigQuery Billing Export**: https://cloud.google.com/billing/docs/how-to/export-data-bigquery

---

**Created**: December 2025
**Last Updated**: December 2025
**Maintainer**: Platform Team
