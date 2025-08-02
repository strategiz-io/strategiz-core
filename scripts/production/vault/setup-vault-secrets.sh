#!/bin/bash

# Setup Strategiz Secrets in Vault
# This script configures all application secrets in HashiCorp Vault

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔐 Setting up Strategiz secrets in Vault...${NC}"

# Load configuration
if [ ! -f "vault-production-config.env" ]; then
    echo -e "${RED}❌ vault-production-config.env not found. Run deploy-vault.sh first.${NC}"
    exit 1
fi

source vault-production-config.env

# Get Vault token from Secret Manager
VAULT_TOKEN=$(gcloud secrets versions access latest --secret="vault-root-token" --project=strategiz-io)

if [ -z "$VAULT_TOKEN" ]; then
    echo -e "${RED}❌ Failed to retrieve Vault token from Secret Manager${NC}"
    exit 1
fi

echo -e "${BLUE}Vault URL: $VAULT_URL${NC}"

# Function to create a secret in Vault
create_secret() {
    local path=$1
    local key=$2
    local value=$3
    
    echo -e "${BLUE}Creating secret: $path/$key${NC}"
    
    curl -s -X POST \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"data\": {\"$key\": \"$value\"}}" \
        "$VAULT_URL/v1/secret/data/$path" > /dev/null
}

# Function to create multiple secrets at once
create_secrets() {
    local path=$1
    shift
    local data="{"
    
    while [ $# -gt 0 ]; do
        data="$data\"$1\": \"$2\""
        shift 2
        if [ $# -gt 0 ]; then
            data="$data, "
        fi
    done
    
    data="$data}"
    
    echo -e "${BLUE}Creating secrets at: $path${NC}"
    
    curl -s -X POST \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"data\": $data}" \
        "$VAULT_URL/v1/secret/data/$path" > /dev/null
}

echo -e "${YELLOW}📝 Enter your secrets (press Enter to skip optional ones):${NC}"

# OAuth Secrets - Google
echo -e "${BLUE}\n=== Google OAuth (Authentication) ===${NC}"
read -p "Google OAuth Client ID: " GOOGLE_CLIENT_ID
read -s -p "Google OAuth Client Secret: " GOOGLE_CLIENT_SECRET
echo

if [ ! -z "$GOOGLE_CLIENT_ID" ] && [ ! -z "$GOOGLE_CLIENT_SECRET" ]; then
    create_secrets "strategiz/oauth/google" \
        "client-id" "$GOOGLE_CLIENT_ID" \
        "client-secret" "$GOOGLE_CLIENT_SECRET"
    echo -e "${GREEN}✅ Google OAuth secrets stored${NC}"
fi

# OAuth Secrets - Facebook
echo -e "${BLUE}\n=== Facebook OAuth (Authentication) ===${NC}"
read -p "Facebook OAuth Client ID: " FACEBOOK_CLIENT_ID
read -s -p "Facebook OAuth Client Secret: " FACEBOOK_CLIENT_SECRET
echo

if [ ! -z "$FACEBOOK_CLIENT_ID" ] && [ ! -z "$FACEBOOK_CLIENT_SECRET" ]; then
    create_secrets "strategiz/oauth/facebook" \
        "client-id" "$FACEBOOK_CLIENT_ID" \
        "client-secret" "$FACEBOOK_CLIENT_SECRET"
    echo -e "${GREEN}✅ Facebook OAuth secrets stored${NC}"
fi

# OAuth Secrets - Coinbase (Provider Integration)
echo -e "${BLUE}\n=== Coinbase OAuth (Provider Integration) ===${NC}"
read -p "Coinbase OAuth Client ID: " COINBASE_CLIENT_ID
read -s -p "Coinbase OAuth Client Secret: " COINBASE_CLIENT_SECRET
echo

if [ ! -z "$COINBASE_CLIENT_ID" ] && [ ! -z "$COINBASE_CLIENT_SECRET" ]; then
    create_secrets "strategiz/oauth/coinbase" \
        "client-id" "$COINBASE_CLIENT_ID" \
        "client-secret" "$COINBASE_CLIENT_SECRET"
    echo -e "${GREEN}✅ Coinbase OAuth secrets stored${NC}"
fi

# Firebase Service Account
echo -e "${BLUE}\n=== Firebase Service Account ===${NC}"
echo "Paste your Firebase service account JSON (press Ctrl+D when done):"
FIREBASE_SA=$(cat)

if [ ! -z "$FIREBASE_SA" ]; then
    # Store as base64 encoded
    FIREBASE_SA_B64=$(echo "$FIREBASE_SA" | base64 -w 0)
    create_secret "strategiz/firebase" "service-account-key" "$FIREBASE_SA_B64"
    echo -e "${GREEN}✅ Firebase service account stored${NC}"
fi

# OpenAI API Key
echo -e "${BLUE}\n=== OpenAI API ===${NC}"
read -s -p "OpenAI API Key (optional): " OPENAI_API_KEY
echo

if [ ! -z "$OPENAI_API_KEY" ]; then
    create_secret "strategiz/openai" "api-key" "$OPENAI_API_KEY"
    echo -e "${GREEN}✅ OpenAI API key stored${NC}"
fi

# Twilio Configuration (for SMS)
echo -e "${BLUE}\n=== Twilio (SMS) ===${NC}"
read -p "Twilio Account SID (optional): " TWILIO_ACCOUNT_SID
read -s -p "Twilio Auth Token (optional): " TWILIO_AUTH_TOKEN
echo
read -p "Twilio Phone Number (optional): " TWILIO_PHONE_NUMBER

if [ ! -z "$TWILIO_ACCOUNT_SID" ] && [ ! -z "$TWILIO_AUTH_TOKEN" ]; then
    create_secrets "strategiz/twilio" \
        "account-sid" "$TWILIO_ACCOUNT_SID" \
        "auth-token" "$TWILIO_AUTH_TOKEN" \
        "phone-number" "$TWILIO_PHONE_NUMBER"
    echo -e "${GREEN}✅ Twilio secrets stored${NC}"
fi

# Database encryption key
echo -e "${BLUE}\n=== Database Encryption ===${NC}"
echo -e "${YELLOW}Generating database encryption key...${NC}"
DB_ENCRYPTION_KEY=$(openssl rand -base64 32)
create_secret "strategiz/database" "encryption-key" "$DB_ENCRYPTION_KEY"
echo -e "${GREEN}✅ Database encryption key generated and stored${NC}"

# Application JWT secret
echo -e "${BLUE}\n=== JWT Secret ===${NC}"
echo -e "${YELLOW}Generating JWT secret...${NC}"
JWT_SECRET=$(openssl rand -base64 64)
create_secret "strategiz/auth" "jwt-secret" "$JWT_SECRET"
echo -e "${GREEN}✅ JWT secret generated and stored${NC}"

echo ""
echo -e "${GREEN}🎉 Vault secrets setup completed!${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. Update Cloud Run environment variables:"
echo "   - VAULT_ADDR=$VAULT_URL"
echo "   - VAULT_TOKEN=<app-specific-token>"
echo ""
echo "2. Create an application-specific token (instead of using root token):"
echo "   ./create-app-token.sh"
echo ""
echo "3. Deploy your application with Vault integration enabled"