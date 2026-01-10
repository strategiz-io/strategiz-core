#!/bin/bash

# Production deployment script for Strategiz Console App (Admin API + Batch Jobs)
# Deploys to separate Google Cloud Run service from main API

set -e  # Exit on any error

echo "===== Deploying Strategiz Console App to Google Cloud Run ====="

# Production Vault Configuration
echo "===== Configuring Vault for Production ====="

# Check if production Vault token is set
if [ -z "$VAULT_TOKEN_PROD" ]; then
    echo "ERROR: VAULT_TOKEN_PROD environment variable not set" >&2
    echo "" >&2
    echo "For production deployments, you need to:" >&2
    echo "1. Create a production token with: ./scripts/vault/create-prod-token.sh" >&2
    echo "2. Store it in Google Secret Manager:" >&2
    echo "   echo -n 'YOUR_TOKEN' | gcloud secrets create vault-token --data-file=-" >&2
    echo "3. Export for this deployment:" >&2
    echo "   export VAULT_TOKEN_PROD='YOUR_TOKEN'" >&2
    exit 1
fi

# Set production Vault address
export VAULT_ADDR=${VAULT_ADDR_PROD:-https://vault.strategiz.io}
export VAULT_TOKEN=$VAULT_TOKEN_PROD

echo "Using Vault at: $VAULT_ADDR"

# Verify Vault connectivity
if ! vault status > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to Vault at $VAULT_ADDR" >&2
    echo "Please check your Vault server and network connectivity" >&2
    exit 1
fi

echo "✅ Vault connectivity verified"

# Change to project root
cd "$(dirname "$0")/../../.."

PROJECT_ID="strategiz-io"
REGION="us-central1"
SERVICE_NAME="strategiz-console"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Service: $SERVICE_NAME"
echo "Image: $IMAGE_NAME"

echo ""
echo "[1/3] Building Console Application JAR..."
mvn clean install -pl application-console -DskipTests -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dpmd.skip=true

echo ""
echo "[2/3] Building and Pushing Docker Image..."

# Create a temporary cloudbuild config
cat > cloudbuild-console-deploy.yaml << EOF
steps:
# Build the Docker image
- name: 'gcr.io/cloud-builders/docker'
  args: ['build', '-f', 'application-console/Dockerfile', '-t', 'gcr.io/\$PROJECT_ID/$SERVICE_NAME', '.']

# Push the Docker image to Container Registry
- name: 'gcr.io/cloud-builders/docker'
  args: ['push', 'gcr.io/\$PROJECT_ID/$SERVICE_NAME']

# Deploy to Cloud Run
- name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
  entrypoint: 'gcloud'
  args:
    - 'run'
    - 'deploy'
    - '$SERVICE_NAME'
    - '--image=gcr.io/\$PROJECT_ID/$SERVICE_NAME'
    - '--platform=managed'
    - '--region=$REGION'
    - '--allow-unauthenticated'
    - '--memory=4Gi'
    - '--cpu=2'
    - '--port=8080'
    - '--min-instances=0'
    - '--max-instances=5'
    - '--timeout=480'
    - '--set-env-vars=SPRING_PROFILES_ACTIVE=prod,scheduler,VAULT_ADDR=$VAULT_ADDR,VAULT_TOKEN=$VAULT_TOKEN,console.auth.enabled=true,strategiz.clickhouse.enabled=true,strategiz.timescale.enabled=false'

images:
- 'gcr.io/\$PROJECT_ID/$SERVICE_NAME'
EOF

# Submit to Cloud Build
gcloud builds submit --config cloudbuild-console-deploy.yaml --timeout=1200s

# Clean up temporary file
rm cloudbuild-console-deploy.yaml

echo ""
echo "===== Console App Deployment Complete ====="
echo "Your console service is now available at the Cloud Run URL shown above."
echo ""
echo "Key Configuration:"
echo "- Memory: 4Gi (for batch job processing)"
echo "- Timeout: 480s (8 minutes for long-running batch jobs)"
echo "- Profiles: prod,scheduler (enables batch job scheduling)"
echo "- ClickHouse: enabled"
echo "- TimescaleDB: disabled"
echo "- Console Auth: enabled (requires admin authentication)"
echo ""
echo "Note: For local development, use console.auth.enabled=false"
