#!/bin/bash

# Update Backend Cloud Run Service with Vault Configuration
# This script updates the Strategiz backend to connect to the production Vault

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔧 Updating backend Cloud Run service with Vault configuration...${NC}"

# Configuration
PROJECT_ID="strategiz-io"
REGION="us-central1"
BACKEND_SERVICE_NAME="strategiz-core"

# Load Vault configuration
if [ ! -f "vault-production-config.env" ]; then
    echo -e "${RED}❌ vault-production-config.env not found. Run deploy-vault.sh first.${NC}"
    exit 1
fi

source vault-production-config.env

echo -e "${BLUE}Vault URL: $VAULT_URL${NC}"

# Get Vault token from Secret Manager
VAULT_TOKEN=$(gcloud secrets versions access latest --secret="vault-root-token" --project=strategiz-io)

echo -e "${BLUE}🔧 Updating Cloud Run service environment variables...${NC}"

# Update the backend service with Vault configuration
gcloud run services update $BACKEND_SERVICE_NAME \
    --region=$REGION \
    --project=$PROJECT_ID \
    --set-env-vars="VAULT_ADDR=$VAULT_URL" \
    --set-env-vars="VAULT_TOKEN=$VAULT_TOKEN" \
    --set-env-vars="VAULT_PATH=secret/strategiz" \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod"

# Wait for deployment to complete
echo -e "${BLUE}⏳ Waiting for deployment to complete...${NC}"
gcloud run services wait $BACKEND_SERVICE_NAME --region=$REGION --project=$PROJECT_ID

# Get the backend URL
BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE_NAME --region $REGION --format 'value(status.url)')

echo -e "${GREEN}✅ Backend service updated successfully!${NC}"
echo -e "${BLUE}Backend URL: $BACKEND_URL${NC}"

# Test the backend health with Vault configuration
echo -e "${BLUE}🔍 Testing backend health...${NC}"
sleep 10  # Wait for the service to restart

HEALTH_RESPONSE=$(curl -s "$BACKEND_URL/actuator/health" || echo '{"status":"ERROR"}')
HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.status // "UNKNOWN"')

echo -e "${BLUE}Health check response:${NC}"
echo "$HEALTH_RESPONSE" | jq '.' || echo "$HEALTH_RESPONSE"

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo -e "${GREEN}🎉 Backend is healthy and connected to Vault!${NC}"
elif [ "$HEALTH_STATUS" = "DOWN" ]; then
    echo -e "${YELLOW}⚠️  Backend is partially healthy. Some components may still be initializing.${NC}"
else
    echo -e "${RED}❌ Backend health check failed. Check the logs.${NC}"
fi

# Test a specific API endpoint
echo -e "${BLUE}🔍 Testing passkey registration endpoint...${NC}"
PASSKEY_TEST=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/auth/passkeys/register/start" -X POST -H "Content-Type: application/json" -d '{"username":"test","userDisplayName":"Test User"}' || echo "000")

if [ "$PASSKEY_TEST" = "200" ] || [ "$PASSKEY_TEST" = "400" ]; then
    echo -e "${GREEN}✅ Passkey endpoint is responding (HTTP $PASSKEY_TEST)${NC}"
else
    echo -e "${YELLOW}⚠️  Passkey endpoint returned HTTP $PASSKEY_TEST${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Backend update completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "✅ Vault configuration applied to backend"
echo "✅ Environment variables updated"
echo "✅ Service redeployed"
echo ""
echo -e "${BLUE}🌐 URLs:${NC}"
echo "Backend: $BACKEND_URL"
echo "Frontend: https://strategiz-io.web.app"
echo "Vault: $VAULT_URL"
echo ""
echo -e "${BLUE}📝 To check logs:${NC}"
echo "gcloud logging read \"resource.type=cloud_run_revision AND resource.labels.service_name=$BACKEND_SERVICE_NAME\" --limit=50 --project=$PROJECT_ID"