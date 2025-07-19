#!/bin/bash

# Production deployment script using local Docker
# Builds locally, creates Docker image locally, then deploys to Cloud Run

set -e  # Exit on any error

echo "===== Deploying Strategiz Core to Google Cloud Run (Local Docker) ====="

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

# Check if Docker is available
if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: Docker not found. Please install Docker Desktop." >&2
    echo "Alternatively, use the Cloud Build script: ./build-and-deploy-cloud-run.sh" >&2
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
echo "[2/4] Building Docker image locally..."
docker build -t $IMAGE_NAME .

echo ""
echo "[3/4] Configuring Docker and pushing image..."
gcloud auth configure-docker -q
docker push $IMAGE_NAME

echo ""
echo "[4/4] Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
    --image $IMAGE_NAME \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 512Mi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 10 \
    --port 8080 \
    --set-env-vars "SPRING_PROFILES_ACTIVE=prod,AUTH_GOOGLE_CLIENT_ID=$AUTH_GOOGLE_CLIENT_ID,AUTH_GOOGLE_CLIENT_SECRET=$AUTH_GOOGLE_CLIENT_SECRET,AUTH_FACEBOOK_CLIENT_ID=$AUTH_FACEBOOK_CLIENT_ID,AUTH_FACEBOOK_CLIENT_SECRET=$AUTH_FACEBOOK_CLIENT_SECRET"

echo ""
echo "===== Deployment Complete ====="
echo "Your service is now available at the Cloud Run URL shown above."
echo "This is running with min-instances=0, so the first request may be slow."
echo "Subsequent requests will be much faster."