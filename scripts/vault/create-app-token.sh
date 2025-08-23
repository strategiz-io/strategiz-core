#!/bin/bash

# Script to create a single Vault token for Strategiz that works in both dev and prod
# This token will have appropriate permissions for OAuth secrets

echo "🔐 Creating Strategiz App Token for Vault"
echo "=========================================="

# Check if Vault is accessible
if ! vault status > /dev/null 2>&1; then
    echo "❌ Vault is not accessible. Please ensure:"
    echo "   1. Vault is running"
    echo "   2. VAULT_ADDR is set correctly"
    echo "   3. You have a root token or admin access"
    exit 1
fi

# Create the policy for Strategiz OAuth access
echo "📝 Creating Vault policy for Strategiz OAuth..."
vault policy write strategiz-oauth - <<EOF
# Read OAuth credentials
path "secret/data/strategiz/oauth/*" {
  capabilities = ["read", "list"]
}

# List OAuth paths
path "secret/metadata/strategiz/oauth/*" {
  capabilities = ["read", "list"]
}

# Allow token renewal
path "auth/token/renew-self" {
  capabilities = ["update"]
}

# Allow token lookup
path "auth/token/lookup-self" {
  capabilities = ["read"]
}
EOF

if [ $? -ne 0 ]; then
    echo "❌ Failed to create policy"
    exit 1
fi

echo "✅ Policy created successfully"

# Create a long-lived token (365 days)
echo ""
echo "🔑 Creating long-lived token (365 days)..."
TOKEN_RESPONSE=$(vault token create \
    -policy=strategiz-oauth \
    -ttl=8760h \
    -renewable \
    -display-name="strategiz-app" \
    -format=json)

if [ $? -ne 0 ]; then
    echo "❌ Failed to create token"
    exit 1
fi

# Extract the token
TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['auth']['client_token'])")

echo "✅ Token created successfully!"
echo ""
echo "=========================================="
echo "🎉 STRATEGIZ VAULT TOKEN CREATED"
echo "=========================================="
echo ""
echo "Your Strategiz Vault Token:"
echo "$TOKEN"
echo ""
echo "This token:"
echo "• Valid for 365 days"
echo "• Can be renewed before expiration"
echo "• Has read access to OAuth secrets"
echo "• Works in both dev and production"
echo ""
echo "To use this token:"
echo ""
echo "1. For local development:"
echo "   export VAULT_TOKEN=$TOKEN"
echo ""
echo "2. For production (Cloud Run):"
echo "   Add as environment variable or secret"
echo ""
echo "3. Save this token securely!"
echo "   Store it in your password manager or secure notes"
echo ""
echo "=========================================="

# Optionally save to a secure file
read -p "Do you want to save this token to a file? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    TOKEN_FILE="$HOME/.strategiz/vault-token"
    mkdir -p "$HOME/.strategiz"
    echo "$TOKEN" > "$TOKEN_FILE"
    chmod 600 "$TOKEN_FILE"
    echo "✅ Token saved to: $TOKEN_FILE"
    echo "   (This file has restricted permissions: 600)"
fi