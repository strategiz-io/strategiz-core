#!/bin/bash
set -e

echo "Starting Vault for local development..."

# Check if Vault is installed
if ! command -v vault &> /dev/null; then
    echo "Error: Vault is not installed. Please install Vault first."
    echo "Visit: https://www.vaultproject.io/downloads"
    exit 1
fi

# Set Vault address
export VAULT_ADDR='http://localhost:8200'

# Check if Vault is already running
if curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo "Vault is already running at http://localhost:8200"
else
    echo "Starting Vault in development mode..."
    # Start Vault in dev mode with a fixed root token for local development
    vault server -dev -dev-root-token-id="strategiz-local-token" &
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
        sleep 1
    done
fi

# Set the token for this session
export VAULT_TOKEN="strategiz-local-token"

# Create the token file that Spring Cloud Vault expects
echo "strategiz-local-token" > ~/.vault-token

echo "Vault setup complete!"
echo "VAULT_ADDR: $VAULT_ADDR"
echo "VAULT_TOKEN: $VAULT_TOKEN"
echo "Token file created at: ~/.vault-token"

# Enable KV secrets engine if needed
echo "Setting up secrets engine..."
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV secrets engine already enabled"

# Add some sample secrets if environment variables are provided
if [ ! -z "$AUTH_GOOGLE_CLIENT_ID" ] && [ ! -z "$AUTH_GOOGLE_CLIENT_SECRET" ]; then
    echo "Adding Google OAuth secrets to Vault..."
    vault kv put secret/strategiz/oauth/google client_id="$AUTH_GOOGLE_CLIENT_ID" client_secret="$AUTH_GOOGLE_CLIENT_SECRET"
fi

if [ ! -z "$AUTH_FACEBOOK_CLIENT_ID" ] && [ ! -z "$AUTH_FACEBOOK_CLIENT_SECRET" ]; then
    echo "Adding Facebook OAuth secrets to Vault..."
    vault kv put secret/strategiz/oauth/facebook client_id="$AUTH_FACEBOOK_CLIENT_ID" client_secret="$AUTH_FACEBOOK_CLIENT_SECRET"
fi

echo "Vault is ready for local development!"
echo "You can now start the Spring Boot application."
echo ""
echo "To stop Vault later, run: pkill vault" 