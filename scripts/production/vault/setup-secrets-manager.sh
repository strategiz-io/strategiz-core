#!/bin/bash

# Setup Google Secret Manager for Strategiz Production
# This script creates all required secrets using Google Secret Manager instead of Vault

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔐 Setting up Google Secret Manager for Strategiz production...${NC}"

# Configuration
PROJECT_ID="strategiz-io"

echo -e "${BLUE}Project: $PROJECT_ID${NC}"

echo -e "${BLUE}[1/5] Enabling Secret Manager API...${NC}"
gcloud config set project $PROJECT_ID
gcloud services enable secretmanager.googleapis.com

# Function to create or update secret
create_secret() {
    local secret_name=$1
    local secret_value=$2
    local description=$3
    
    echo -e "${BLUE}📝 Creating secret: $secret_name${NC}"
    
    # Check if secret already exists
    if gcloud secrets describe "$secret_name" --project="$PROJECT_ID" >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  Secret $secret_name already exists, adding new version${NC}"
        echo "$secret_value" | gcloud secrets versions add "$secret_name" --data-file=- --project="$PROJECT_ID"
    else
        echo "$secret_value" | gcloud secrets create "$secret_name" --data-file=- --project="$PROJECT_ID"
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Secret created/updated: $secret_name${NC}"
    else
        echo -e "${RED}❌ Failed to create: $secret_name${NC}"
    fi
}

echo -e "${BLUE}[2/5] Setting up Firebase configuration...${NC}"
echo -e "${YELLOW}Please provide your Firebase configuration:${NC}"
read -p "Enter Firebase Project ID: " FIREBASE_PROJECT_ID
echo "Please paste your Firebase service account JSON (paste the entire JSON object):"
read -r FIREBASE_SERVICE_ACCOUNT_JSON

create_secret "strategiz-firebase-project-id" "$FIREBASE_PROJECT_ID"
create_secret "strategiz-firebase-service-account" "$FIREBASE_SERVICE_ACCOUNT_JSON"
create_secret "strategiz-firebase-database-url" "https://$FIREBASE_PROJECT_ID.firebaseio.com"

echo -e "${BLUE}[3/5] Setting up OAuth configurations...${NC}"

# Google OAuth
echo -e "${YELLOW}Setting up Google OAuth...${NC}"
read -p "Enter Google OAuth Client ID: " GOOGLE_CLIENT_ID
read -s -p "Enter Google OAuth Client Secret: " GOOGLE_CLIENT_SECRET
echo

create_secret "strategiz-google-oauth-client-id" "$GOOGLE_CLIENT_ID"
create_secret "strategiz-google-oauth-client-secret" "$GOOGLE_CLIENT_SECRET"

# Facebook OAuth
echo -e "${YELLOW}Setting up Facebook OAuth...${NC}"
read -p "Enter Facebook App ID: " FACEBOOK_APP_ID
read -s -p "Enter Facebook App Secret: " FACEBOOK_APP_SECRET
echo

create_secret "strategiz-facebook-oauth-client-id" "$FACEBOOK_APP_ID"
create_secret "strategiz-facebook-oauth-client-secret" "$FACEBOOK_APP_SECRET"

echo -e "${BLUE}[4/5] Setting up API keys...${NC}"

# AlphaVantage API
echo -e "${YELLOW}Setting up AlphaVantage API...${NC}"
read -s -p "Enter AlphaVantage API Key: " ALPHAVANTAGE_API_KEY
echo

create_secret "strategiz-alphavantage-api-key" "$ALPHAVANTAGE_API_KEY"

# CoinGecko API (optional)
echo -e "${YELLOW}Setting up CoinGecko API (press Enter to skip)...${NC}"
read -s -p "Enter CoinGecko API Key (optional): " COINGECKO_API_KEY
echo

if [ ! -z "$COINGECKO_API_KEY" ]; then
    create_secret "strategiz-coingecko-api-key" "$COINGECKO_API_KEY"
fi

# Firebase Web API Key for SMS
echo -e "${YELLOW}Setting up Firebase Web API Key for SMS...${NC}"
read -s -p "Enter Firebase Web API Key: " FIREBASE_WEB_API_KEY
echo

create_secret "strategiz-firebase-web-api-key" "$FIREBASE_WEB_API_KEY"

echo -e "${BLUE}[5/5] Setting up provider OAuth (optional)...${NC}"

# Coinbase OAuth (optional)
echo -e "${YELLOW}Setting up Coinbase OAuth (press Enter to skip)...${NC}"
read -p "Enter Coinbase Client ID (optional): " COINBASE_CLIENT_ID
if [ ! -z "$COINBASE_CLIENT_ID" ]; then
    read -s -p "Enter Coinbase Client Secret: " COINBASE_CLIENT_SECRET
    echo
    create_secret "strategiz-coinbase-oauth-client-id" "$COINBASE_CLIENT_ID"
    create_secret "strategiz-coinbase-oauth-client-secret" "$COINBASE_CLIENT_SECRET"
fi

# Email configuration (optional)
echo -e "${YELLOW}Setting up Email configuration (press Enter to skip)...${NC}"
read -p "Enter SMTP Host (optional): " SMTP_HOST
if [ ! -z "$SMTP_HOST" ]; then
    read -p "Enter SMTP Port: " SMTP_PORT
    read -p "Enter SMTP Username: " SMTP_USERNAME
    read -s -p "Enter SMTP Password: " SMTP_PASSWORD
    echo
    
    create_secret "strategiz-email-smtp-host" "$SMTP_HOST"
    create_secret "strategiz-email-smtp-port" "$SMTP_PORT"
    create_secret "strategiz-email-smtp-username" "$SMTP_USERNAME"
    create_secret "strategiz-email-smtp-password" "$SMTP_PASSWORD"
fi

echo ""
echo -e "${GREEN}🎉 Secret Manager setup completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary of configured secrets:${NC}"
echo "✅ Firebase configuration"
echo "✅ Google OAuth"
echo "✅ Facebook OAuth"
echo "✅ AlphaVantage API"
echo "✅ Firebase Web API Key"
[ ! -z "$COINBASE_CLIENT_ID" ] && echo "✅ Coinbase OAuth"
[ ! -z "$COINGECKO_API_KEY" ] && echo "✅ CoinGecko API"
[ ! -z "$SMTP_HOST" ] && echo "✅ Email configuration"

echo ""
echo -e "${BLUE}🔍 To verify secrets:${NC}"
echo "gcloud secrets list --project=$PROJECT_ID"
echo "gcloud secrets versions access latest --secret=\"strategiz-firebase-project-id\" --project=$PROJECT_ID"

echo ""
echo -e "${BLUE}Next step: Update Cloud Run service with Secret Manager configuration${NC}"