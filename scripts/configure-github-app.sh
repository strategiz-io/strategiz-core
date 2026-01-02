#!/bin/bash
set -e

# Script to configure GitHub App credentials in Vault
# This script helps store GitHub App ID and private key in Vault for Platform Agents

echo "=========================================="
echo "GitHub App Configuration for Vault"
echo "=========================================="
echo ""

# Check if vault is installed
if ! command -v vault &> /dev/null; then
    echo "Error: vault CLI not found. Please install Vault first."
    echo "  brew install vault  # macOS"
    exit 1
fi

# Prompt for environment
echo "Select environment:"
echo "  1) Local development (http://localhost:8200)"
echo "  2) Production (https://strategiz-vault-43628135674.us-east1.run.app)"
read -p "Enter choice [1-2]: " ENV_CHOICE

if [ "$ENV_CHOICE" = "1" ]; then
    VAULT_ADDR="http://localhost:8200"
    VAULT_TOKEN="${VAULT_TOKEN:-root}"
    ENV_NAME="Local"
elif [ "$ENV_CHOICE" = "2" ]; then
    VAULT_ADDR="https://strategiz-vault-43628135674.us-east1.run.app"
    VAULT_TOKEN="${VAULT_TOKEN:-hvs.q2Lg7uILKNkEs20UA8mbT9Cr}"
    ENV_NAME="Production"
else
    echo "Invalid choice. Exiting."
    exit 1
fi

export VAULT_ADDR
export VAULT_TOKEN

echo ""
echo "Environment: $ENV_NAME"
echo "Vault Address: $VAULT_ADDR"
echo ""

# Test Vault connection
echo "Testing Vault connection..."
if ! vault status > /dev/null 2>&1; then
    echo "Error: Cannot connect to Vault at $VAULT_ADDR"
    echo "Please ensure:"
    echo "  1. Vault is running"
    echo "  2. VAULT_TOKEN is correct"
    exit 1
fi
echo "✓ Connected to Vault"
echo ""

# Prompt for GitHub App ID
echo "=========================================="
echo "Step 1: GitHub App ID"
echo "=========================================="
echo "Find your App ID at: https://github.com/organizations/strategiz-io/settings/apps"
echo "It's displayed at the top of the GitHub App settings page (e.g., App ID: 123456)"
echo ""
read -p "Enter GitHub App ID: " APP_ID

if [ -z "$APP_ID" ]; then
    echo "Error: App ID cannot be empty"
    exit 1
fi

# Prompt for private key file
echo ""
echo "=========================================="
echo "Step 2: Private Key"
echo "=========================================="
echo "Generate a private key from: https://github.com/organizations/strategiz-io/settings/apps"
echo "Scroll to 'Private keys' section and click 'Generate a private key'"
echo "The downloaded file will be named something like: strategiz-platform-agents.2024-01-01.private-key.pem"
echo ""
read -p "Enter path to private key .pem file: " KEY_FILE

# Expand tilde to home directory
KEY_FILE="${KEY_FILE/#\~/$HOME}"

if [ ! -f "$KEY_FILE" ]; then
    echo "Error: File not found: $KEY_FILE"
    exit 1
fi

# Validate PEM format
if ! grep -q "BEGIN RSA PRIVATE KEY" "$KEY_FILE" && ! grep -q "BEGIN PRIVATE KEY" "$KEY_FILE"; then
    echo "Error: File does not appear to be a valid PEM private key"
    echo "Expected to find 'BEGIN RSA PRIVATE KEY' or 'BEGIN PRIVATE KEY'"
    exit 1
fi

echo "✓ Private key file found and validated"
echo ""

# Read private key content
PRIVATE_KEY=$(cat "$KEY_FILE")

# Store in Vault
echo "=========================================="
echo "Step 3: Store in Vault"
echo "=========================================="
echo "Storing credentials in Vault at: secret/strategiz/github-app"
echo ""

# Store both values in a single command
vault kv put secret/strategiz/github-app \
    app-id="$APP_ID" \
    private-key="$PRIVATE_KEY"

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Credentials stored successfully!"
else
    echo ""
    echo "✗ Failed to store credentials"
    exit 1
fi

# Verify
echo ""
echo "=========================================="
echo "Step 4: Verify"
echo "=========================================="
echo "Reading back from Vault to verify..."
echo ""

vault kv get secret/strategiz/github-app | grep -E "app-id|private-key" | head -5

echo ""
echo "=========================================="
echo "Configuration Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Ensure github.app.enabled=true in application-prod.properties"
echo "  2. Deploy the application"
echo "  3. Check logs for: 'GitHub App configuration loaded successfully'"
echo "  4. Test: curl https://api.strategiz.io/v1/console/agents"
echo ""
echo "For more information, see: docs/github-app-setup.md"
