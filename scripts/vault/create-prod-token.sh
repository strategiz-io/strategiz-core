#\!/bin/bash

echo "🔐 Creating Production Vault Token"
echo "===================================="

# This assumes you have a production Vault instance
# and you're authenticated as an admin

# Create a policy with only read access to OAuth secrets
vault policy write strategiz-prod-oauth - <<POLICY
# Read-only access to OAuth secrets
path "secret/data/strategiz/oauth/*" {
  capabilities = ["read"]
}

# Allow token to renew itself
path "auth/token/renew-self" {
  capabilities = ["update"]
}
POLICY

# Create a token with 30-day TTL (renewable)
echo ""
echo "Creating production token..."
TOKEN_RESPONSE=$(vault token create \
    -policy=strategiz-prod-oauth \
    -ttl=720h \
    -renewable \
    -display-name="strategiz-prod-app" \
    -format=json)

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.auth.client_token')

echo ""
echo "✅ Production Token Created\!"
echo "============================"
echo ""
echo "Token: $TOKEN"
echo ""
echo "This token:"
echo "• Valid for 30 days (renewable)"
echo "• Read-only access to OAuth secrets"
echo "• Should be stored in your deployment platform's secret manager"
