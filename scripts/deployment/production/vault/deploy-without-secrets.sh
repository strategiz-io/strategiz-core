#!/bin/bash

# Deploy backend to production without secrets (for testing basic deployment)
# This script deploys the backend with minimal configuration to test the basic setup

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🚀 Deploying backend to production without secrets...${NC}"

# Configuration
PROJECT_ID="strategiz-io"
REGION="us-central1"
BACKEND_SERVICE_NAME="strategiz-core"

echo -e "${BLUE}Project: $PROJECT_ID${NC}"
echo -e "${BLUE}Service: $BACKEND_SERVICE_NAME${NC}"
echo -e "${BLUE}Region: $REGION${NC}"

echo -e "${BLUE}[1/3] Deploying updated backend to Cloud Run...${NC}"

# Deploy the updated backend with basic configuration
gcloud run deploy $BACKEND_SERVICE_NAME \
    --source . \
    --region=$REGION \
    --project=$PROJECT_ID \
    --allow-unauthenticated \
    --memory=2Gi \
    --cpu=2 \
    --min-instances=1 \
    --max-instances=10 \
    --port=8080 \
    --timeout=300 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
    --set-env-vars="FIREBASE_PROJECT_ID=strategiz-io" \
    --set-env-vars="FIREBASE_DATABASE_URL=https://strategiz-io.firebaseio.com"

# Wait for deployment to complete
echo -e "${BLUE}⏳ Waiting for deployment to complete...${NC}"
gcloud run services wait $BACKEND_SERVICE_NAME --region=$REGION --project=$PROJECT_ID

# Get the backend URL
BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE_NAME --region $REGION --format 'value(status.url)')

echo -e "${GREEN}✅ Backend service updated successfully!${NC}"
echo -e "${BLUE}Backend URL: $BACKEND_URL${NC}"

echo -e "${BLUE}[2/3] Testing backend health...${NC}"
sleep 15  # Wait for the service to fully restart

HEALTH_RESPONSE=$(curl -s "$BACKEND_URL/actuator/health" || echo '{"status":"ERROR"}')
HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.status // "UNKNOWN"')

echo -e "${BLUE}Health check response:${NC}"
echo "$HEALTH_RESPONSE" | jq '.' || echo "$HEALTH_RESPONSE"

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo -e "${GREEN}🎉 Backend is healthy!${NC}"
elif [ "$HEALTH_STATUS" = "DOWN" ]; then
    echo -e "${YELLOW}⚠️  Backend is partially healthy. This is expected without secrets.${NC}"
else
    echo -e "${YELLOW}⚠️  Backend health check shows: $HEALTH_STATUS${NC}"
fi

echo -e "${BLUE}[3/3] Testing basic endpoints...${NC}"

# Test a basic endpoint that doesn't require authentication
echo -e "${BLUE}🔍 Testing actuator info endpoint...${NC}"
INFO_RESPONSE=$(curl -s "$BACKEND_URL/actuator/info" || echo '{}')
echo "Info response: $INFO_RESPONSE"

echo ""
echo -e "${GREEN}🎉 Basic backend deployment completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "✅ Backend deployed with basic configuration"
echo "✅ Vault disabled (using environment variables)"
echo "⚠️  Secrets not configured yet (OAuth won't work)"
echo ""
echo -e "${BLUE}🌐 URLs:${NC}"
echo "Backend: $BACKEND_URL"
echo "Frontend: https://strategiz-io.web.app"
echo ""
echo -e "${BLUE}📝 Next steps:${NC}"
echo "1. Configure Google OAuth credentials in Google Cloud Console"
echo "2. Configure Facebook OAuth credentials in Facebook Developer Console"
echo "3. Set up Firebase service account credentials"
echo "4. Update Cloud Run service with actual secret environment variables"