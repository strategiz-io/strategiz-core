#!/bin/bash

# Production Vault Deployment Script for Strategiz
# This script deploys HashiCorp Vault on Google Cloud Run for production use

set -e

echo "🔐 Deploying HashiCorp Vault to Google Cloud Run..."

# Configuration
PROJECT_ID="strategiz-io"
REGION="us-central1"
VAULT_SERVICE_NAME="strategiz-vault"
VAULT_ROOT_TOKEN="strategiz-prod-$(openssl rand -hex 16)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}[1/6] Setting up Google Cloud project...${NC}"
gcloud config set project $PROJECT_ID

echo -e "${BLUE}[2/6] Enabling required APIs...${NC}"
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable storage-api.googleapis.com
gcloud services enable secretmanager.googleapis.com

echo -e "${BLUE}[3/6] Creating Cloud Storage bucket for Vault backend...${NC}"
BUCKET_NAME="$PROJECT_ID-vault-storage-$(date +%s)"
gsutil mb -p $PROJECT_ID -c STANDARD -l $REGION gs://$BUCKET_NAME || echo "Bucket already exists"

echo -e "${BLUE}[4/6] Creating Vault configuration...${NC}"
cat > vault-config.hcl << EOF
ui = true

listener "tcp" {
  address = "0.0.0.0:8200"
  tls_disable = "true"
}

storage "gcs" {
  bucket = "$BUCKET_NAME"
  ha_enabled = "true"
}

api_addr = "https://$VAULT_SERVICE_NAME-$(gcloud config get-value project | tr ':' '-')-$REGION.a.run.app"
cluster_addr = "https://$VAULT_SERVICE_NAME-$(gcloud config get-value project | tr ':' '-')-$REGION.a.run.app:8201"

log_level = "INFO"
default_lease_ttl = "168h"
max_lease_ttl = "720h"
EOF

echo -e "${BLUE}[5/6] Creating Vault Dockerfile...${NC}"
cat > Dockerfile.vault << EOF
FROM hashicorp/vault:1.15

# Copy configuration file
COPY vault-config.hcl /vault/config/vault-config.hcl

# Set environment variables
ENV VAULT_CONFIG_PATH=/vault/config/vault-config.hcl
ENV VAULT_API_ADDR=\$VAULT_API_ADDR
ENV VAULT_CLUSTER_ADDR=\$VAULT_CLUSTER_ADDR

# Expose port
EXPOSE 8200

# Run Vault
CMD ["vault", "server", "-config=/vault/config/vault-config.hcl"]
EOF

echo -e "${BLUE}[6/6] Building and deploying Vault to Cloud Run...${NC}"

# Build the Vault container
gcloud builds submit --tag gcr.io/$PROJECT_ID/vault:latest --file Dockerfile.vault .

# Deploy to Cloud Run
gcloud run deploy $VAULT_SERVICE_NAME \
    --image gcr.io/$PROJECT_ID/vault:latest \
    --region $REGION \
    --platform managed \
    --allow-unauthenticated \
    --memory 1Gi \
    --cpu 1 \
    --min-instances 1 \
    --max-instances 3 \
    --port 8200 \
    --set-env-vars "VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:8200" \
    --timeout 300

# Get the Vault URL
VAULT_URL=$(gcloud run services describe $VAULT_SERVICE_NAME --region $REGION --format 'value(status.url)')

echo ""
echo -e "${GREEN}🎉 Vault deployment completed!${NC}"
echo -e "${YELLOW}Vault URL: $VAULT_URL${NC}"
echo -e "${YELLOW}Root Token: $VAULT_ROOT_TOKEN${NC}"
echo ""
echo -e "${RED}⚠️  IMPORTANT: Save the root token securely!${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. Initialize Vault: ./initialize-vault-production.sh"
echo "2. Configure secrets: ./setup-vault-secrets.sh"
echo "3. Update application environment variables"

# Save the configuration
cat > vault-production-config.env << EOF
VAULT_URL=$VAULT_URL
VAULT_ROOT_TOKEN=$VAULT_ROOT_TOKEN
BUCKET_NAME=$BUCKET_NAME
EOF

echo ""
echo -e "${GREEN}Configuration saved to vault-production-config.env${NC}"

# Clean up temporary files
rm -f vault-config.hcl Dockerfile.vault