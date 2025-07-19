#!/bin/bash

# Initialize HashiCorp Vault in Production
# This script initializes and unseals Vault with proper production settings

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔐 Initializing Vault in Production...${NC}"

# Load configuration
if [ ! -f "vault-production-config.env" ]; then
    echo -e "${RED}❌ vault-production-config.env not found. Run deploy-vault.sh first.${NC}"
    exit 1
fi

source vault-production-config.env

echo -e "${BLUE}Vault URL: $VAULT_URL${NC}"

# Wait for Vault to be ready
echo -e "${BLUE}⏳ Waiting for Vault to be ready...${NC}"
for i in {1..30}; do
    if curl -s "$VAULT_URL/v1/sys/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Vault is ready!${NC}"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 10
done

# Check if Vault is already initialized
VAULT_STATUS=$(curl -s "$VAULT_URL/v1/sys/init" | jq -r '.initialized')

if [ "$VAULT_STATUS" = "true" ]; then
    echo -e "${YELLOW}⚠️  Vault is already initialized${NC}"
    echo "If you need to unseal Vault, provide the unseal keys when prompted."
else
    echo -e "${BLUE}🔧 Initializing Vault...${NC}"
    
    # Initialize Vault with 5 key shares and threshold of 3
    INIT_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{"secret_shares": 5, "secret_threshold": 3}' \
        "$VAULT_URL/v1/sys/init")
    
    # Extract keys and root token
    ROOT_TOKEN=$(echo "$INIT_RESPONSE" | jq -r '.root_token')
    UNSEAL_KEYS=$(echo "$INIT_RESPONSE" | jq -r '.keys[]')
    
    echo -e "${GREEN}✅ Vault initialized successfully!${NC}"
    echo ""
    echo -e "${RED}🔑 CRITICAL: Save these keys securely!${NC}"
    echo -e "${YELLOW}Root Token: $ROOT_TOKEN${NC}"
    echo -e "${YELLOW}Unseal Keys:${NC}"
    echo "$UNSEAL_KEYS" | nl -v0 -s': '
    echo ""
    
    # Save to Google Secret Manager for secure storage
    echo -e "${BLUE}💾 Saving keys to Google Secret Manager...${NC}"
    echo "$ROOT_TOKEN" | gcloud secrets create vault-root-token --data-file=- --project=strategiz-io || \
        echo "$ROOT_TOKEN" | gcloud secrets versions add vault-root-token --data-file=- --project=strategiz-io
    
    echo "$UNSEAL_KEYS" | gcloud secrets create vault-unseal-keys --data-file=- --project=strategiz-io || \
        echo "$UNSEAL_KEYS" | gcloud secrets versions add vault-unseal-keys --data-file=- --project=strategiz-io
    
    # Update configuration file
    cat > vault-production-config.env << EOF
VAULT_URL=$VAULT_URL
VAULT_ROOT_TOKEN=$ROOT_TOKEN
BUCKET_NAME=$BUCKET_NAME
EOF
    
    echo -e "${GREEN}✅ Keys saved to Secret Manager${NC}"
fi

# Unseal Vault
echo -e "${BLUE}🔓 Unsealing Vault...${NC}"

# Get unseal keys from Secret Manager
STORED_KEYS=$(gcloud secrets versions access latest --secret="vault-unseal-keys" --project=strategiz-io)

# Unseal with first 3 keys
KEY_COUNT=0
echo "$STORED_KEYS" | while read key && [ $KEY_COUNT -lt 3 ]; do
    if [ ! -z "$key" ]; then
        UNSEAL_RESPONSE=$(curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "{\"key\": \"$key\"}" \
            "$VAULT_URL/v1/sys/unseal")
        
        SEALED=$(echo "$UNSEAL_RESPONSE" | jq -r '.sealed')
        PROGRESS=$(echo "$UNSEAL_RESPONSE" | jq -r '.progress')
        THRESHOLD=$(echo "$UNSEAL_RESPONSE" | jq -r '.t')
        
        echo "Unseal progress: $PROGRESS/$THRESHOLD"
        
        if [ "$SEALED" = "false" ]; then
            echo -e "${GREEN}✅ Vault unsealed successfully!${NC}"
            break
        fi
        
        KEY_COUNT=$((KEY_COUNT + 1))
    fi
done

# Enable KV v2 secrets engine
echo -e "${BLUE}🔧 Enabling KV v2 secrets engine...${NC}"
VAULT_TOKEN=$(gcloud secrets versions access latest --secret="vault-root-token" --project=strategiz-io)

curl -s -X POST \
    -H "X-Vault-Token: $VAULT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"type": "kv", "options": {"version": "2"}}' \
    "$VAULT_URL/v1/sys/mounts/secret" || echo "KV v2 already enabled"

echo ""
echo -e "${GREEN}🎉 Vault initialization completed!${NC}"
echo -e "${BLUE}Next step: Run ./setup-vault-secrets.sh to configure application secrets${NC}"