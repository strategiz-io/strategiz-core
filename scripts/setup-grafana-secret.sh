#!/bin/bash
# Setup script for Grafana Cloud credentials in GCP Secret Manager

set -e

echo "🔧 Grafana Cloud Secret Setup"
echo "========================================"
echo ""
echo "You'll need the following from Grafana Cloud Portal:"
echo "1. Instance ID (numeric value from your stack details)"
echo "2. API Token (create from Security → Service Accounts → MetricsPublisher role)"
echo ""
echo "Get these from: https://grafana.com/orgs/<your-org>/stacks"
echo ""

# Prompt for credentials
read -p "Enter your Grafana Instance ID: " INSTANCE_ID
read -sp "Enter your Grafana API Token: " API_TOKEN
echo ""

if [ -z "$INSTANCE_ID" ] || [ -z "$API_TOKEN" ]; then
    echo "❌ Error: Instance ID and API Token are required"
    exit 1
fi

# Create base64 encoded auth header
AUTH_HEADER=$(echo -n "${INSTANCE_ID}:${API_TOKEN}" | base64)

echo ""
echo "📝 Creating GCP Secret Manager secret..."

# Create or update the secret
if gcloud secrets describe GRAFANA_OTLP_AUTH &>/dev/null; then
    echo "Secret already exists, creating new version..."
    echo -n "${AUTH_HEADER}" | gcloud secrets versions add GRAFANA_OTLP_AUTH --data-file=-
else
    echo "Creating new secret..."
    echo -n "${AUTH_HEADER}" | gcloud secrets create GRAFANA_OTLP_AUTH \
        --data-file=- \
        --replication-policy=automatic
fi

echo ""
echo "🔐 Granting access to Cloud Run service account..."

# Grant access to the default compute service account
SERVICE_ACCOUNT="43628135674-compute@developer.gserviceaccount.com"

gcloud secrets add-iam-policy-binding GRAFANA_OTLP_AUTH \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/secretmanager.secretAccessor" \
    --quiet

echo ""
echo "✅ Grafana Cloud secret configured successfully!"
echo ""
echo "Secret name: GRAFANA_OTLP_AUTH"
echo "Auth header (base64): ${AUTH_HEADER:0:20}..."
echo ""
echo "You can now deploy services with: gcloud builds submit --config=cloudbuild-execution.yaml"
