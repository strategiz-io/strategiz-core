#\!/bin/bash
echo "Enter Coinbase OAuth credentials:"
read -p "Client ID: " CLIENT_ID
read -s -p "Client Secret: " CLIENT_SECRET
echo

export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='root-token'

# Create the secrets in Vault
vault kv put secret/strategiz/oauth/coinbase \
  client-id="$CLIENT_ID" \
  client-secret="$CLIENT_SECRET"

echo "✅ Coinbase OAuth credentials added to local Vault"
