#!/bin/bash

# Script to set up Alpaca OAuth credentials in Vault
# Usage: ./setup-alpaca-oauth.sh

echo "=================================="
echo "Alpaca OAuth Setup for Strategiz"
echo "=================================="
echo ""
echo "Before proceeding, you need to:"
echo "1. Go to https://app.alpaca.markets/brokerage/apps"
echo "2. Create a new OAuth application"
echo "3. Set the redirect URI to: http://localhost:3000/auth/providers/alpaca/callback"
echo "4. Copy the Client ID and Client Secret"
echo ""
read -p "Do you have your Alpaca OAuth credentials ready? (y/n): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Please get your credentials first, then run this script again."
    exit 1
fi

# Check if Vault is running
if ! curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo "Error: Vault is not running at http://localhost:8200"
    echo "Please start Vault with: vault server -dev"
    exit 1
fi

# Set Vault address
export VAULT_ADDR=http://localhost:8200

# Check for Vault token
if [ -z "$VAULT_TOKEN" ]; then
    echo "VAULT_TOKEN not set. Using default root token."
    export VAULT_TOKEN=root
fi

echo ""
read -p "Enter your Alpaca OAuth Client ID: " CLIENT_ID
read -s -p "Enter your Alpaca OAuth Client Secret: " CLIENT_SECRET
echo ""

# Validate inputs
if [ -z "$CLIENT_ID" ] || [ -z "$CLIENT_SECRET" ]; then
    echo "Error: Client ID and Client Secret are required"
    exit 1
fi

# Store in Vault
echo ""
echo "Storing Alpaca OAuth credentials in Vault..."

vault kv put secret/strategiz/oauth/alpaca \
    client-id="$CLIENT_ID" \
    client-secret="$CLIENT_SECRET" \
    redirect-uri="http://localhost:3000/auth/providers/alpaca/callback" \
    auth-url="https://app.alpaca.markets/oauth/authorize" \
    token-url="https://api.alpaca.markets/oauth/token" \
    api-url="https://api.alpaca.markets" \
    scope="account:read trading:write data:read"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Successfully stored Alpaca OAuth credentials in Vault!"
    echo ""
    echo "Verifying configuration..."
    vault kv get secret/strategiz/oauth/alpaca
    echo ""
    echo "=================================="
    echo "Setup complete!"
    echo ""
    echo "You can now use Alpaca OAuth integration by:"
    echo "1. Starting the backend: mvn spring-boot:run -pl application"
    echo "2. Starting the frontend: npm start (in strategiz-ui)"
    echo "3. Navigate to provider connections in the UI"
    echo "=================================="
else
    echo ""
    echo "❌ Failed to store credentials in Vault"
    echo "Please check your Vault configuration and try again"
    exit 1
fi