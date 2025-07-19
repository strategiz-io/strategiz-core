#!/bin/bash

# Production deployment script for Google Cloud Run
# Builds in correct dependency order and deploys to Cloud Run

set -e  # Exit on any error

echo "===== Deploying Strategiz Core to Google Cloud Run ====="

# Check if Vault is available and get OAuth credentials
if command -v vault >/dev/null 2>&1; then
    echo "Loading OAuth credentials from Vault..."
    export VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
    
    # Get Google OAuth credentials
    export AUTH_GOOGLE_CLIENT_ID=$(vault kv get -field=client-id secret/strategiz/oauth/google 2>/dev/null)
    export AUTH_GOOGLE_CLIENT_SECRET=$(vault kv get -field=client-secret secret/strategiz/oauth/google 2>/dev/null)
    
    # Get Facebook OAuth credentials
    export AUTH_FACEBOOK_CLIENT_ID=$(vault kv get -field=client-id secret/strategiz/oauth/facebook 2>/dev/null)
    export AUTH_FACEBOOK_CLIENT_SECRET=$(vault kv get -field=client-secret secret/strategiz/oauth/facebook 2>/dev/null)
    
    if [ -z "$AUTH_GOOGLE_CLIENT_ID" ] || [ -z "$AUTH_GOOGLE_CLIENT_SECRET" ] || [ -z "$AUTH_FACEBOOK_CLIENT_ID" ] || [ -z "$AUTH_FACEBOOK_CLIENT_SECRET" ]; then
        echo "ERROR: Could not load OAuth credentials from Vault" >&2
        echo "Please ensure Vault is running and contains the OAuth secrets" >&2
        exit 1
    fi
    
    echo "✅ Successfully loaded OAuth credentials from Vault"
else
    # Fallback to environment variables
    if [ -z "$AUTH_GOOGLE_CLIENT_ID" ] || [ -z "$AUTH_GOOGLE_CLIENT_SECRET" ] || [ -z "$AUTH_FACEBOOK_CLIENT_ID" ] || [ -z "$AUTH_FACEBOOK_CLIENT_SECRET" ]; then
        echo "ERROR: Vault not available and OAuth environment variables not set" >&2
        echo "Please either:" >&2
        echo "1. Start Vault and ensure OAuth secrets are configured, or" >&2
        echo "2. Set environment variables:" >&2
        echo "  AUTH_GOOGLE_CLIENT_ID" >&2
        echo "  AUTH_GOOGLE_CLIENT_SECRET" >&2
        echo "  AUTH_FACEBOOK_CLIENT_ID" >&2
        echo "  AUTH_FACEBOOK_CLIENT_SECRET" >&2
        exit 1
    fi
fi

# Change to project root first
cd "$(dirname "$0")/../../.."

# Check if Firebase credentials exist
if [ ! -f "application/src/main/resources/firebase-service-account.json" ]; then
    echo "ERROR: firebase-service-account.json not found" >&2
    echo "Please ensure Firebase credentials are in application/src/main/resources/" >&2
    exit 1
fi

PROJECT_ID="strategiz-io"
REGION="us-central1"
SERVICE_NAME="strategiz-backend"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Service: $SERVICE_NAME"
echo "Image: $IMAGE_NAME"

echo ""
echo "[1/4] Building application using local build script..."
./scripts/deployment/local/build.sh

echo ""
echo "[2/3] Submitting to Cloud Build for Docker build and deploy..."

# Create a temporary cloudbuild config that includes environment variables
cat > cloudbuild-deploy.yaml << EOF
steps:
# Build the Docker image using the pre-built JAR
- name: 'gcr.io/cloud-builders/docker'
  args: ['build', '-t', 'gcr.io/\$PROJECT_ID/strategiz-backend', '.']

# Push the Docker image to Container Registry
- name: 'gcr.io/cloud-builders/docker'
  args: ['push', 'gcr.io/\$PROJECT_ID/strategiz-backend']

# Deploy to Cloud Run with OAuth environment variables
- name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
  entrypoint: 'gcloud'
  args:
    - 'run'
    - 'deploy'
    - 'strategiz-backend'
    - '--image=gcr.io/\$PROJECT_ID/strategiz-backend'
    - '--platform=managed'
    - '--region=$REGION'
    - '--allow-unauthenticated'
    - '--memory=512Mi'
    - '--cpu=1'
    - '--port=8080'
    - '--min-instances=0'
    - '--max-instances=10'
    - '--set-env-vars=SPRING_PROFILES_ACTIVE=prod,AUTH_GOOGLE_CLIENT_ID=$AUTH_GOOGLE_CLIENT_ID,AUTH_GOOGLE_CLIENT_SECRET=$AUTH_GOOGLE_CLIENT_SECRET,AUTH_FACEBOOK_CLIENT_ID=$AUTH_FACEBOOK_CLIENT_ID,AUTH_FACEBOOK_CLIENT_SECRET=$AUTH_FACEBOOK_CLIENT_SECRET'

images:
- 'gcr.io/\$PROJECT_ID/strategiz-backend'
EOF

# Submit to Cloud Build
gcloud builds submit --config cloudbuild-deploy.yaml --timeout=1200s

# Clean up temporary file
rm cloudbuild-deploy.yaml

echo "===== Deployment Complete ====="
echo "Your service is now available at the Cloud Run URL shown above."
echo "This is running with min-instances=0, so the first request may be slow."
echo "Subsequent requests will be much faster."