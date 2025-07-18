#!/bin/bash

# Minimal Vault Setup Script for Strategiz Development
# Sets up Vault with placeholder secrets for development/testing

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="strategiz-local-token"

echo "🔐 Setting up minimal Vault configuration for development..."

# Check if Vault is running
if ! curl -s "$VAULT_ADDR/v1/sys/health" > /dev/null 2>&1; then
    echo "❌ Vault is not running at $VAULT_ADDR"
    echo "Please start Vault first with: vault server -dev -dev-root-token-id=$VAULT_TOKEN"
    exit 1
fi

echo "✅ Vault is running at $VAULT_ADDR"

# Export Vault address and token
export VAULT_ADDR="$VAULT_ADDR"
export VAULT_TOKEN="$VAULT_TOKEN"

echo "🔧 Configuring Vault with development secrets..."

# Enable KV v2 secrets engine if not already enabled
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV v2 already enabled"

# Create minimal OAuth secrets with placeholder values
echo "📝 Creating placeholder OAuth secrets..."

# Google OAuth secrets (placeholder)
vault kv put secret/strategiz/auth/google \
    client-id="dev-google-client-id" \
    client-secret="dev-google-client-secret"

# Facebook OAuth secrets (placeholder)
vault kv put secret/strategiz/auth/facebook \
    client-id="dev-facebook-app-id" \
    client-secret="dev-facebook-app-secret"

# Provider secrets (placeholder)
vault kv put secret/strategiz/provider/kraken \
    client-id="dev-kraken-client-id" \
    client-secret="dev-kraken-client-secret"

# API Keys (placeholder)
vault kv put secret/strategiz/api/alphavantage \
    api-key="dev-alphavantage-key"

# Firebase configuration (placeholder)
vault kv put secret/strategiz/firebase \
    project-id="dev-firebase-project" \
    service-account-key='{"type":"service_account","project_id":"dev-project","client_email":"dev@example.com"}' \
    storage-bucket="dev-storage-bucket"

# Email configuration (placeholder)
vault kv put secret/strategiz/email \
    smtp-host="dev-smtp-host" \
    smtp-port="587" \
    smtp-username="dev@example.com" \
    smtp-password="dev-password"

# SMS configuration (placeholder)
vault kv put secret/strategiz/sms \
    firebase-web-api-key="dev-firebase-web-api-key"

echo "✅ Development Vault setup completed!"
echo ""
echo "📋 To start the application:"
echo "1. Export the Vault token: export VAULT_TOKEN=$VAULT_TOKEN"
echo "2. Start the application: mvn spring-boot:run -pl application-strategiz"
echo ""
echo "⚠️  Note: This setup uses placeholder secrets for development."
echo "   OAuth and external API integrations will not work with these values."
echo "   Update the secrets with real values when needed."