#!/bin/bash

# Start script for Strategiz Core with Vault integration
# This script ensures Vault is properly configured

echo "🚀 Starting Strategiz Core with Vault integration..."

# Check if Vault is running
if ! vault status > /dev/null 2>&1; then
    echo "⚠️  Vault is not running. Starting Vault in dev mode..."
    vault server -dev &
    sleep 2
    echo "✅ Vault started in dev mode"
fi

# Set Vault address if not already set
if [ -z "$VAULT_ADDR" ]; then
    export VAULT_ADDR=http://localhost:8200
    echo "📝 Setting VAULT_ADDR to: $VAULT_ADDR"
fi

# Check for Vault token
if [ -z "$VAULT_TOKEN" ]; then
    echo ""
    echo "❌ ERROR: VAULT_TOKEN environment variable is not set!"
    echo ""
    echo "For local development with dev server:"
    echo "  export VAULT_TOKEN=root"
    echo ""
    echo "For production or proper token:"
    echo "  export VAULT_TOKEN=<your-vault-token>"
    echo ""
    echo "Then run this script again."
    exit 1
fi

echo "🔐 Vault configured:"
echo "   Address: $VAULT_ADDR"
echo "   Token: [CONFIGURED]"

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/../../.."

# Build if JAR doesn't exist
if [ ! -f "application/target/application-1.0-SNAPSHOT.jar" ]; then
    echo "📦 Building application..."
    ./scripts/local/build/build.sh
fi

# Start the application
echo "🚀 Starting application with profile: $SPRING_PROFILES_ACTIVE"
java -jar application/target/application-1.0-SNAPSHOT.jar