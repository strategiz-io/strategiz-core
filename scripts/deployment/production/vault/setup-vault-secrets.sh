#!/bin/bash

# Setup Production Vault Secrets for Strategiz
# This script configures all required secrets in the production Vault instance

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔐 Setting up Vault secrets for Strategiz production...${NC}"

# Load configuration
if [ ! -f "vault-production-config.env" ]; then
    echo -e "${RED}❌ vault-production-config.env not found. Run initialize-vault-production.sh first.${NC}"
    exit 1
fi

source vault-production-config.env

# Get Vault token from Secret Manager
VAULT_TOKEN=$(gcloud secrets versions access latest --secret="vault-root-token" --project=strategiz-io)
export VAULT_ADDR="$VAULT_URL"
export VAULT_TOKEN="$VAULT_TOKEN"

echo -e "${BLUE}Vault URL: $VAULT_URL${NC}"

# Function to store secret in Vault
store_secret() {
    local path=$1
    local data=$2
    
    echo -e "${BLUE}📝 Storing secret: $path${NC}"
    curl -s -X POST \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$data" \
        "$VAULT_URL/v1/secret/data/$path" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Secret stored: $path${NC}"
    else
        echo -e "${RED}❌ Failed to store: $path${NC}"
    fi
}

echo -e "${BLUE}🔧 Creating authentication secrets...${NC}"

# Firebase configuration (for authentication and database)
echo -e "${YELLOW}Setting up Firebase configuration...${NC}"
read -p "Enter Firebase Project ID: " FIREBASE_PROJECT_ID
echo "Please paste your Firebase service account JSON (all on one line):"
read -r FIREBASE_SERVICE_ACCOUNT

store_secret "strategiz/firebase" '{
    "data": {
        "project-id": "'$FIREBASE_PROJECT_ID'",
        "service-account-key": "'$FIREBASE_SERVICE_ACCOUNT'",
        "database-url": "https://'$FIREBASE_PROJECT_ID'.firebaseio.com"
    }
}'

# Google OAuth configuration
echo -e "${YELLOW}Setting up Google OAuth...${NC}"
read -p "Enter Google OAuth Client ID: " GOOGLE_CLIENT_ID
read -p "Enter Google OAuth Client Secret: " GOOGLE_CLIENT_SECRET

store_secret "strategiz/auth/google" '{
    "data": {
        "client-id": "'$GOOGLE_CLIENT_ID'",
        "client-secret": "'$GOOGLE_CLIENT_SECRET'"
    }
}'

# Facebook OAuth configuration
echo -e "${YELLOW}Setting up Facebook OAuth...${NC}"
read -p "Enter Facebook App ID: " FACEBOOK_APP_ID
read -p "Enter Facebook App Secret: " FACEBOOK_APP_SECRET

store_secret "strategiz/auth/facebook" '{
    "data": {
        "client-id": "'$FACEBOOK_APP_ID'",
        "client-secret": "'$FACEBOOK_APP_SECRET'"
    }
}'

echo -e "${BLUE}🔧 Creating provider OAuth secrets...${NC}"

# Coinbase OAuth (optional)
echo -e "${YELLOW}Setting up Coinbase OAuth (press Enter to skip)...${NC}"
read -p "Enter Coinbase Client ID (optional): " COINBASE_CLIENT_ID
if [ ! -z "$COINBASE_CLIENT_ID" ]; then
    read -p "Enter Coinbase Client Secret: " COINBASE_CLIENT_SECRET
    store_secret "strategiz/provider/coinbase" '{
        "data": {
            "client-id": "'$COINBASE_CLIENT_ID'",
            "client-secret": "'$COINBASE_CLIENT_SECRET'"
        }
    }'
fi

# Kraken OAuth (optional)
echo -e "${YELLOW}Setting up Kraken OAuth (press Enter to skip)...${NC}"
read -p "Enter Kraken Client ID (optional): " KRAKEN_CLIENT_ID
if [ ! -z "$KRAKEN_CLIENT_ID" ]; then
    read -p "Enter Kraken Client Secret: " KRAKEN_CLIENT_SECRET
    store_secret "strategiz/provider/kraken" '{
        "data": {
            "client-id": "'$KRAKEN_CLIENT_ID'",
            "client-secret": "'$KRAKEN_CLIENT_SECRET'"
        }
    }'
fi

echo -e "${BLUE}🔧 Creating API keys...${NC}"

# AlphaVantage API key
echo -e "${YELLOW}Setting up AlphaVantage API...${NC}"
read -p "Enter AlphaVantage API Key: " ALPHAVANTAGE_API_KEY

store_secret "strategiz/api/alphavantage" '{
    "data": {
        "api-key": "'$ALPHAVANTAGE_API_KEY'"
    }
}'

# CoinGecko API key (optional)
echo -e "${YELLOW}Setting up CoinGecko API (press Enter to skip)...${NC}"
read -p "Enter CoinGecko API Key (optional): " COINGECKO_API_KEY
if [ ! -z "$COINGECKO_API_KEY" ]; then
    store_secret "strategiz/api/coingecko" '{
        "data": {
            "api-key": "'$COINGECKO_API_KEY'"
        }
    }'
fi

# SMS Configuration (Firebase-based)
echo -e "${YELLOW}Setting up SMS configuration...${NC}"
read -p "Enter Firebase Web API Key: " FIREBASE_WEB_API_KEY

store_secret "strategiz/sms" '{
    "data": {
        "firebase-web-api-key": "'$FIREBASE_WEB_API_KEY'"
    }
}'

# Email configuration (optional)
echo -e "${YELLOW}Setting up Email configuration (press Enter to skip)...${NC}"
read -p "Enter SMTP Host (optional): " SMTP_HOST
if [ ! -z "$SMTP_HOST" ]; then
    read -p "Enter SMTP Port: " SMTP_PORT
    read -p "Enter SMTP Username: " SMTP_USERNAME
    read -p "Enter SMTP Password: " SMTP_PASSWORD
    
    store_secret "strategiz/email" '{
        "data": {
            "smtp-host": "'$SMTP_HOST'",
            "smtp-port": "'$SMTP_PORT'",
            "smtp-username": "'$SMTP_USERNAME'",
            "smtp-password": "'$SMTP_PASSWORD'"
        }
    }'
fi

echo ""
echo -e "${GREEN}🎉 Vault secrets setup completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary of configured secrets:${NC}"
echo "✅ Firebase configuration"
echo "✅ Google OAuth"
echo "✅ Facebook OAuth"
echo "✅ AlphaVantage API"
echo "✅ SMS configuration"
[ ! -z "$COINBASE_CLIENT_ID" ] && echo "✅ Coinbase OAuth"
[ ! -z "$KRAKEN_CLIENT_ID" ] && echo "✅ Kraken OAuth"
[ ! -z "$COINGECKO_API_KEY" ] && echo "✅ CoinGecko API"
[ ! -z "$SMTP_HOST" ] && echo "✅ Email configuration"

echo ""
echo -e "${BLUE}🔍 To verify secrets:${NC}"
echo "curl -H \"X-Vault-Token: \$VAULT_TOKEN\" \$VAULT_URL/v1/secret/data/strategiz/firebase"

echo ""
echo -e "${BLUE}Next step: Update Cloud Run service with Vault configuration${NC}"