#!/bin/bash

# Production deployment script for Strategiz Console App (Admin API + Batch Jobs)
# Deploys to separate Google Cloud Run service from main API

set -e  # Exit on any error

echo "===== Deploying Strategiz Console App to Google Cloud Run ====="

# Production Vault Configuration
# NOTE: Production uses EMBEDDED Vault (runs inside container, not external server)
# The Vault token is stored in GCP Secret Manager and mounted to Cloud Run
# See VAULT_SETUP.md for details
echo ""
echo "Production Vault Setup:"
echo "- Vault runs embedded inside Docker container (localhost:8200)"
echo "- VAULT_TOKEN mounted from GCP Secret Manager: vault-root-token"
echo "- Container startup script handles Vault unsealing"
echo ""

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
# Build the Docker image (using production Dockerfile with embedded Vault)
- name: 'gcr.io/cloud-builders/docker'
  args: ['build', '-f', 'application-console/Dockerfile.production', '-t', 'gcr.io/\$PROJECT_ID/$SERVICE_NAME', '.']

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
    - '--timeout=600'
    - '--cpu-boost'
    - '--set-env-vars=SPRING_PROFILES_ACTIVE=prod^:^scheduler,console.auth.enabled=true,strategiz.clickhouse.enabled=true,strategiz.timescale.enabled=false'
    - '--set-secrets=VAULT_TOKEN=vault-root-token:latest,VAULT_UNSEAL_KEY=vault-unseal-keys:latest'

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
