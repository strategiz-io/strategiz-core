#!/bin/bash

# Vault Setup Script for Strategiz Local Development
# This script initializes HashiCorp Vault with the required secrets and configuration

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="strategiz-local-token"

echo "🔐 Setting up HashiCorp Vault for Strategiz local development..."

# Check if Vault is running
if ! curl -s "$VAULT_ADDR/v1/sys/health" > /dev/null 2>&1; then
    echo "❌ Vault is not running at $VAULT_ADDR"
    echo "Please start Vault first with: vault server -dev"
    exit 1
fi

echo "✅ Vault is running at $VAULT_ADDR"

# Export Vault address and token
export VAULT_ADDR="$VAULT_ADDR"
export VAULT_TOKEN="$VAULT_TOKEN"

echo "🔧 Configuring Vault..."

# Enable KV v2 secrets engine if not already enabled
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV v2 already enabled"

# Create OAuth secrets for authentication
echo "📝 Creating OAuth secrets..."

# Google OAuth secrets
vault kv put secret/strategiz/auth/google \
    client-id="your-google-client-id" \
    client-secret="your-google-client-secret"

# Facebook OAuth secrets  
vault kv put secret/strategiz/auth/facebook \
    client-id="your-facebook-app-id" \
    client-secret="your-facebook-app-secret"

# Provider OAuth secrets (for connecting exchanges)
echo "📝 Creating provider OAuth secrets..."

# Kraken OAuth secrets
vault kv put secret/strategiz/provider/kraken \
    client-id="your-kraken-client-id" \
    client-secret="your-kraken-client-secret"

# Coinbase OAuth secrets
vault kv put secret/strategiz/provider/coinbase \
    client-id="your-coinbase-client-id" \
    client-secret="your-coinbase-client-secret"

# API Keys
echo "📝 Creating API keys..."

# AlphaVantage API key
vault kv put secret/strategiz/api/alphavantage \
    api-key="your-alphavantage-api-key"

# CoinGecko API key (optional)
vault kv put secret/strategiz/api/coingecko \
    api-key="your-coingecko-api-key"

# Etherscan API key
vault kv put secret/strategiz/api/etherscan \
    api-key="your-etherscan-api-key"

# Firebase configuration
echo "📝 Creating Firebase secrets..."
vault kv put secret/strategiz/firebase \
    project-id="your-firebase-project-id" \
    service-account-key='{"type":"service_account","project_id":"your-project"}' \
    storage-bucket="your-storage-bucket"

# Email configuration (if using SMTP)
echo "📝 Creating email configuration..."
vault kv put secret/strategiz/email \
    smtp-host="smtp.gmail.com" \
    smtp-port="587" \
    smtp-username="your-email@gmail.com" \
    smtp-password="your-app-password"

# SMS configuration (if using SMS OTP)
echo "📝 Creating SMS configuration..."
vault kv put secret/strategiz/sms \
    firebase-web-api-key="your-firebase-web-api-key"

echo "✅ Vault setup completed successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Update the secrets in Vault with your actual API keys and credentials"
echo "2. Export the Vault token: export VAULT_TOKEN=$VAULT_TOKEN"
echo "3. Start the application: mvn spring-boot:run -pl application-strategiz"
echo ""
echo "🔍 To view stored secrets:"
echo "   vault kv get secret/strategiz/auth/google"
echo "   vault kv get secret/strategiz/provider/kraken"
echo ""
echo "📝 To update a secret:"
echo "   vault kv put secret/strategiz/auth/google client-id=\"your-real-client-id\""