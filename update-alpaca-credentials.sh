#!/bin/bash

# Quick script to update Alpaca OAuth credentials
echo "Enter your Alpaca Client ID:"
read CLIENT_ID

echo "Enter your Alpaca Client Secret:"
read -s CLIENT_SECRET

VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv put secret/strategiz/oauth/alpaca \
    client-id="$CLIENT_ID" \
    client-secret="$CLIENT_SECRET" \
    redirect-uri="https://localhost:8443/v1/providers/callback/alpaca" \
    auth-url="https://app.alpaca.markets/oauth/authorize" \
    token-url="https://api.alpaca.markets/oauth/token" \
    api-url="https://api.alpaca.markets" \
    scope="account:read trading:write data:read"

echo ""
echo "✅ Alpaca credentials updated in Vault"
echo "Restart your backend for changes to take effect"