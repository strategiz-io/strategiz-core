#!/bin/bash

# Initialize and unseal Vault (run this after starting vault with start-vault.sh)
# This only needs to be run once after first starting persistent Vault

export VAULT_ADDR="http://127.0.0.1:8200"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
KEYS_FILE="$SCRIPT_DIR/vault-keys.txt"

# Check if Vault is already initialized
if vault status 2>/dev/null | grep -q "Initialized.*true"; then
    echo "Vault is already initialized."

    if vault status 2>/dev/null | grep -q "Sealed.*true"; then
        echo "Vault is sealed. Attempting to unseal..."

        if [ -f "$KEYS_FILE" ]; then
            UNSEAL_KEY=$(grep "Unseal Key 1:" "$KEYS_FILE" | cut -d: -f2 | tr -d ' ')
            vault operator unseal "$UNSEAL_KEY"
            echo ""
            echo "Vault unsealed!"
        else
            echo "ERROR: Keys file not found at $KEYS_FILE"
            echo "You need to manually unseal with your unseal key."
            exit 1
        fi
    else
        echo "Vault is already unsealed and ready."
    fi
else
    echo "Initializing Vault for the first time..."
    echo ""

    # Initialize with 1 key share and 1 key threshold for simplicity in local dev
    vault operator init -key-shares=1 -key-threshold=1 > "$KEYS_FILE"

    echo "Vault initialized! Keys saved to: $KEYS_FILE"
    echo ""
    cat "$KEYS_FILE"
    echo ""

    # Extract unseal key and root token
    UNSEAL_KEY=$(grep "Unseal Key 1:" "$KEYS_FILE" | cut -d: -f2 | tr -d ' ')
    ROOT_TOKEN=$(grep "Initial Root Token:" "$KEYS_FILE" | cut -d: -f2 | tr -d ' ')

    # Unseal Vault
    echo "Unsealing Vault..."
    vault operator unseal "$UNSEAL_KEY"

    echo ""
    echo "============================================"
    echo "IMPORTANT: Save these values!"
    echo "============================================"
    echo "Unseal Key: $UNSEAL_KEY"
    echo "Root Token: $ROOT_TOKEN"
    echo ""
    echo "Export the token to use Vault:"
    echo "  export VAULT_ADDR=http://127.0.0.1:8200"
    echo "  export VAULT_TOKEN=$ROOT_TOKEN"
    echo ""

    # Enable KV secrets engine
    export VAULT_TOKEN="$ROOT_TOKEN"
    vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV engine already enabled"
fi

echo ""
echo "Vault is ready!"
vault status
