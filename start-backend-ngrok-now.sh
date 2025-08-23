#!/bin/bash

NGROK_URL="https://db5b6fbe7171.ngrok-free.app"

echo "Starting backend with ngrok URL: $NGROK_URL"
echo ""

# Update Vault with the ngrok redirect URI
echo "Updating Coinbase OAuth settings in Vault..."
VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv put secret/strategiz/oauth/coinbase \
    client-id="88bf21ba-e7b5-4b75-8f94-c239046d20a4" \
    client-secret="ISl99wBG8yEDaxPt7TZXa44vv5" \
    redirect-uri="${NGROK_URL}/v1/providers/callback/coinbase"

echo ""
echo "Starting backend..."
echo "OAuth redirect URI: ${NGROK_URL}/v1/providers/callback/coinbase"
echo ""

# Start the backend
VAULT_ADDR=http://localhost:8200 \
VAULT_TOKEN=root \
NGROK_URL=$NGROK_URL \
java -jar application/target/application-1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev-ngrok \
    --server.port=8443 \
    --oauth.providers.coinbase.redirect-uri=${NGROK_URL}/v1/providers/callback/coinbase
