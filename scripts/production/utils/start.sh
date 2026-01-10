#!/bin/bash
set -e

echo "Starting Strategiz application with embedded Vault..."

# Start Vault in the background
echo "Starting Vault server..."
vault server -config=/app/vault-config.hcl &
VAULT_PID=$!

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
        echo "Vault is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "Vault failed to start"
        exit 1
    fi
    sleep 2
done

# Initialize Vault if not already initialized
if ! vault status | grep -q "Initialized.*true"; then
    echo "Initializing Vault..."
    vault operator init -key-shares=1 -key-threshold=1 -format=json > /app/vault-keys.json
    
    # Extract unseal key and root token
    UNSEAL_KEY=$(cat /app/vault-keys.json | jq -r '.unseal_keys_b64[0]')
    ROOT_TOKEN=$(cat /app/vault-keys.json | jq -r '.root_token')
    
    # Unseal Vault
    echo "Unsealing Vault..."
    vault operator unseal $UNSEAL_KEY
    
    # Set root token
    export VAULT_TOKEN=$ROOT_TOKEN
    
    # Enable KV secrets engine
    echo "Enabling KV secrets engine..."
    vault secrets enable -path=secret kv-v2
    
    # Add initial secrets if environment variables are provided
    if [ ! -z "$AUTH_GOOGLE_CLIENT_ID" ] && [ ! -z "$AUTH_GOOGLE_CLIENT_SECRET" ]; then
        echo "Adding Google OAuth secrets to Vault..."
        vault kv put secret/oauth/google client_id="$AUTH_GOOGLE_CLIENT_ID" client_secret="$AUTH_GOOGLE_CLIENT_SECRET"
    fi
    
    if [ ! -z "$AUTH_FACEBOOK_CLIENT_ID" ] && [ ! -z "$AUTH_FACEBOOK_CLIENT_SECRET" ]; then
        echo "Adding Facebook OAuth secrets to Vault..."
        vault kv put secret/oauth/facebook client_id="$AUTH_FACEBOOK_CLIENT_ID" client_secret="$AUTH_FACEBOOK_CLIENT_SECRET"
    fi
    
    # Store the token for the application
    echo "VAULT_TOKEN=$ROOT_TOKEN" > /app/vault-token.env
else
    echo "Vault is already initialized"
    # If already initialized, we still need to unseal it
    if vault status | grep -q "Sealed.*true"; then
        echo "Unsealing Vault..."
        UNSEAL_KEY=$(cat /app/vault-keys.json | jq -r '.unseal_keys_b64[0]')
        vault operator unseal $UNSEAL_KEY
    fi
    
    # Load the token
    if [ -f /app/vault-token.env ]; then
        source /app/vault-token.env
    fi
fi

# Function to cleanup on exit
cleanup() {
    echo "Shutting down..."
    kill $VAULT_PID 2>/dev/null || true
    exit 0
}

# Set up signal handlers
trap cleanup SIGTERM SIGINT

# Export Vault environment variables for the application
export VAULT_ADDR=http://localhost:8200
if [ -z "$VAULT_TOKEN" ]; then
    echo "WARNING: VAULT_TOKEN not set, application may fail to load secrets"
fi
export VAULT_TOKEN

# Start the Spring Boot application
echo "Starting Spring Boot application..."
echo "VAULT_ADDR=$VAULT_ADDR"
echo "VAULT_TOKEN is set: $([ -n "$VAULT_TOKEN" ] && echo 'yes' || echo 'no')"
java -jar app.jar --server.port=${PORT} &
APP_PID=$!

# Wait for either process to exit
wait $APP_PID 