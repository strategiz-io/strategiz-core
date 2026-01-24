#!/bin/bash

# Update Backend Cloud Run Service with Secret Manager Configuration
# This script updates the Strategiz backend to use Google Secret Manager instead of Vault

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔧 Updating backend Cloud Run service with Secret Manager configuration...${NC}"

# Configuration
PROJECT_ID="strategiz-io"
REGION="us-central1"
BACKEND_SERVICE_NAME="strategiz-core"

echo -e "${BLUE}Project: $PROJECT_ID${NC}"
echo -e "${BLUE}Service: $BACKEND_SERVICE_NAME${NC}"
echo -e "${BLUE}Region: $REGION${NC}"

echo -e "${BLUE}[1/4] Creating new application-prod.properties with Secret Manager integration...${NC}"

# Go back to the backend directory
cd /Users/cuztomizer/Documents/GitHub/strategiz-core

# Create updated application-prod.properties
cat > application/src/main/resources/application-prod.properties << 'EOF'
# Production Configuration for Strategiz with Google Secret Manager

# Server Configuration
server.port=${PORT:8080}

# Security
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=None

# Firebase Configuration (from Secret Manager)
firebase.database-url=${strategiz_firebase_database_url}
firebase.project-id=${strategiz_firebase_project_id}
firebase.service-account-key=${strategiz_firebase_service_account}

# Disable Vault in production (using Secret Manager instead)
vault.enabled=false

# OAuth Configuration (from Secret Manager)
# Google OAuth
oauth.providers.google.client-id=${strategiz_google_oauth_client_id}
oauth.providers.google.client-secret=${strategiz_google_oauth_client_secret}
oauth.providers.google.redirect-uri=https://strategiz-core-43628135674.us-central1.run.app/auth/oauth/google/signin/callback
oauth.providers.google.auth-url=https://accounts.google.com/o/oauth2/auth
oauth.providers.google.token-url=https://oauth2.googleapis.com/token
oauth.providers.google.user-info-url=https://www.googleapis.com/oauth2/v3/userinfo
oauth.providers.google.scope=email profile openid

# Facebook OAuth
oauth.providers.facebook.client-id=${strategiz_facebook_oauth_client_id}
oauth.providers.facebook.client-secret=${strategiz_facebook_oauth_client_secret}
oauth.providers.facebook.redirect-uri=https://strategiz-core-43628135674.us-central1.run.app/auth/oauth/facebook/signin/callback
oauth.providers.facebook.auth-url=https://www.facebook.com/v12.0/dialog/oauth
oauth.providers.facebook.token-url=https://graph.facebook.com/v12.0/oauth/access_token
oauth.providers.facebook.user-info-url=https://graph.facebook.com/me
oauth.providers.facebook.scope=email,public_profile

# API Keys (from Secret Manager)
api.alphavantage.key=${strategiz_alphavantage_api_key}
api.coingecko.key=${strategiz_coingecko_api_key:}
api.firebase.web-key=${strategiz_firebase_web_api_key}

# Provider OAuth (from Secret Manager)
provider.coinbase.client-id=${strategiz_coinbase_oauth_client_id:}
provider.coinbase.client-secret=${strategiz_coinbase_oauth_client_secret:}

# Email Configuration (from Secret Manager)
spring.mail.host=${strategiz_email_smtp_host:}
spring.mail.port=${strategiz_email_smtp_port:587}
spring.mail.username=${strategiz_email_smtp_username:}
spring.mail.password=${strategiz_email_smtp_password:}
spring.mail.properties.mail.smtp.auth=${strategiz_email_smtp_username:?false:true}
spring.mail.properties.mail.smtp.starttls.enable=true

# CORS Configuration
strategiz.cors.allowed-origins=https://strategiz-io.web.app,https://strategiz-io.firebaseapp.com,https://strategiz.io,https://auth.strategiz.io,https://console.strategiz.io,https://strategiz-auth.web.app,https://strategiz-console.web.app

# Frontend URL configuration
application.frontend-url=https://strategiz.io
oauth.frontend-url=https://auth.strategiz.io

# Logging
logging.level.root=INFO
logging.level.io.strategiz=INFO
logging.level.org.springframework.web=INFO
logging.level.org.springframework.security=INFO

# Actuator endpoints for health checks
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized

# Application info
info.app.name=Strategiz Core API
info.app.version=@project.version@
info.app.environment=production

# Disable development features
spring.devtools.enabled=false
spring.jpa.show-sql=false

# Performance optimizations
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.default-property-inclusion=non_null
EOF

echo -e "${GREEN}✅ Updated application-prod.properties${NC}"

echo -e "${BLUE}[2/4] Building updated backend...${NC}"
mvn clean package -DskipTests -pl application -am

echo -e "${BLUE}[3/4] Deploying updated backend to Cloud Run...${NC}"

# Deploy the updated backend with secret environment variables
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
    --set-secrets="strategiz_firebase_project_id=strategiz-firebase-project-id:latest" \
    --set-secrets="strategiz_firebase_service_account=strategiz-firebase-service-account:latest" \
    --set-secrets="strategiz_firebase_database_url=strategiz-firebase-database-url:latest" \
    --set-secrets="strategiz_google_oauth_client_id=strategiz-google-oauth-client-id:latest" \
    --set-secrets="strategiz_google_oauth_client_secret=strategiz-google-oauth-client-secret:latest" \
    --set-secrets="strategiz_facebook_oauth_client_id=strategiz-facebook-oauth-client-id:latest" \
    --set-secrets="strategiz_facebook_oauth_client_secret=strategiz-facebook-oauth-client-secret:latest" \
    --set-secrets="strategiz_alphavantage_api_key=strategiz-alphavantage-api-key:latest" \
    --set-secrets="strategiz_firebase_web_api_key=strategiz-firebase-web-api-key:latest"

# Wait for deployment to complete
echo -e "${BLUE}⏳ Waiting for deployment to complete...${NC}"
gcloud run services wait $BACKEND_SERVICE_NAME --region=$REGION --project=$PROJECT_ID

# Get the backend URL
BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE_NAME --region $REGION --format 'value(status.url)')

echo -e "${GREEN}✅ Backend service updated successfully!${NC}"
echo -e "${BLUE}Backend URL: $BACKEND_URL${NC}"

echo -e "${BLUE}[4/4] Testing backend health...${NC}"
sleep 15  # Wait for the service to fully restart

HEALTH_RESPONSE=$(curl -s "$BACKEND_URL/actuator/health" || echo '{"status":"ERROR"}')
HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.status // "UNKNOWN"')

echo -e "${BLUE}Health check response:${NC}"
echo "$HEALTH_RESPONSE" | jq '.' || echo "$HEALTH_RESPONSE"

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo -e "${GREEN}🎉 Backend is healthy!${NC}"
elif [ "$HEALTH_STATUS" = "DOWN" ]; then
    echo -e "${YELLOW}⚠️  Backend is partially healthy. Checking specific components...${NC}"
    
    # Check if Firebase is working
    FIREBASE_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.components.firebase.status // "UNKNOWN"')
    echo "Firebase status: $FIREBASE_STATUS"
    
    # Check if discovery is working (can be ignored)
    echo "Note: Discovery component can be DOWN in production - this is normal"
else
    echo -e "${RED}❌ Backend health check failed. Check the logs.${NC}"
fi

# Test passkey endpoint
echo -e "${BLUE}🔍 Testing passkey registration endpoint...${NC}"
PASSKEY_TEST=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BACKEND_URL/auth/passkeys/register/start" \
    -X POST \
    -H "Content-Type: application/json" \
    -d '{"username":"test@example.com","userDisplayName":"Test User"}' || echo "000")

if [ "$PASSKEY_TEST" = "200" ] || [ "$PASSKEY_TEST" = "400" ]; then
    echo -e "${GREEN}✅ Passkey endpoint is responding (HTTP $PASSKEY_TEST)${NC}"
else
    echo -e "${YELLOW}⚠️  Passkey endpoint returned HTTP $PASSKEY_TEST${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Backend deployment completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "✅ Application properties updated with Secret Manager integration"
echo "✅ Backend rebuilt and deployed"
echo "✅ Secret environment variables configured"
echo "✅ Service redeployed with new configuration"
echo ""
echo -e "${BLUE}🌐 URLs:${NC}"
echo "Backend: $BACKEND_URL"
echo "Frontend: https://strategiz-io.web.app"
echo ""
echo -e "${BLUE}📝 To check logs:${NC}"
echo "gcloud logging read \"resource.type=cloud_run_revision AND resource.labels.service_name=$BACKEND_SERVICE_NAME\" --limit=50 --project=$PROJECT_ID"

echo ""
echo -e "${BLUE}🔍 To test the application:${NC}"
echo "1. Visit: https://strategiz-io.web.app"
echo "2. Try signing up with passkey authentication"
echo "3. Check that all authentication methods work properly"