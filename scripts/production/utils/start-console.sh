#!/bin/bash
set -e

echo "Starting Strategiz Console with embedded Vault..."

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
        echo "ERROR: Vault failed to start within 60 seconds"
        exit 1
    fi
    sleep 2
done

# Export Vault address
export VAULT_ADDR=http://localhost:8200

# Check if Vault is already initialized
VAULT_STATUS=$(vault status -format=json 2>/dev/null || echo '{"initialized":false}')
IS_INITIALIZED=$(echo $VAULT_STATUS | jq -r '.initialized')

if [ "$IS_INITIALIZED" = "false" ]; then
    echo "Initializing Vault for the first time..."
    vault operator init -key-shares=1 -key-threshold=1 -format=json > /tmp/vault-keys.json

    # Extract unseal key and root token
    UNSEAL_KEY=$(jq -r '.unseal_keys_b64[0]' /tmp/vault-keys.json)
    ROOT_TOKEN=$(jq -r '.root_token' /tmp/vault-keys.json)

    echo "Vault initialized. IMPORTANT: Save these credentials to GCP Secret Manager:"
    echo "  Unseal Key: $UNSEAL_KEY"
    echo "  Root Token: $ROOT_TOKEN"

    # Unseal Vault
    echo "Unsealing Vault..."
    vault operator unseal $UNSEAL_KEY

    export VAULT_TOKEN=$ROOT_TOKEN

    # Enable KV v2 secrets engine
    echo "Enabling KV v2 secrets engine..."
    vault secrets enable -path=secret kv-v2

else
    echo "Vault is already initialized"

    # Vault needs to be unsealed after restart
    IS_SEALED=$(echo $VAULT_STATUS | jq -r '.sealed')
    if [ "$IS_SEALED" = "true" ]; then
        echo "Unsealing Vault with key from GCP Secret Manager..."

        # Try to get unseal key from mounted secret (Cloud Run secret mount)
        if [ -f "/secrets/vault-unseal-key" ]; then
            UNSEAL_KEY=$(cat /secrets/vault-unseal-key)
            vault operator unseal $UNSEAL_KEY
        elif [ ! -z "$VAULT_UNSEAL_KEY" ]; then
            vault operator unseal $VAULT_UNSEAL_KEY
        else
            echo "ERROR: No unseal key found. Set VAULT_UNSEAL_KEY or mount secret."
            exit 1
        fi
    fi

    # Set token from GCP Secret Manager (mounted as file or env var)
    if [ -f "/secrets/vault-root-token" ]; then
        export VAULT_TOKEN=$(cat /secrets/vault-root-token)
    elif [ -z "$VAULT_TOKEN" ]; then
        echo "WARNING: VAULT_TOKEN not set, using initialization token"
        export VAULT_TOKEN=$ROOT_TOKEN
    fi
fi

# Verify Vault token works
if ! vault token lookup > /dev/null 2>&1; then
    echo "ERROR: VAULT_TOKEN is invalid or Vault is not accessible"
    exit 1
fi

echo "Vault is ready and unsealed"
echo "VAULT_ADDR=$VAULT_ADDR"
echo "VAULT_TOKEN is set: yes"

# Function to cleanup on exit
cleanup() {
    echo "Shutting down..."
    kill $VAULT_PID 2>/dev/null || true
    kill $APP_PID 2>/dev/null || true
    exit 0
}

# Set up signal handlers
trap cleanup SIGTERM SIGINT

# Start the Spring Boot application with OpenTelemetry
echo "Starting Spring Boot Console application..."
echo "Server port: ${PORT:-8080}"
echo "Spring profiles: $SPRING_PROFILES_ACTIVE"

java $JAVA_OPTS \
    -javaagent:/app/opentelemetry-javaagent.jar \
    -jar /app/app.jar \
    --server.port=${PORT:-8080} &
APP_PID=$!

echo "Application started (PID: $APP_PID)"
echo "Waiting for application to be ready..."

# Wait for either process to exit
wait $APP_PID
