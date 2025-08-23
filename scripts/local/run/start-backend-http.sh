#!/bin/bash

# Start backend with HTTP configuration (port 8080)
echo "🚀 Starting Strategiz Backend with HTTP on port 8080..."
echo "📋 Profile: dev-http"
echo "🔗 URL: http://localhost:8080"
echo ""

# Ensure Vault is configured
if [ -z "$VAULT_TOKEN" ]; then
    echo "⚠️  VAULT_TOKEN not set. Using default 'root' token for development."
    export VAULT_TOKEN=root
fi

export VAULT_ADDR=http://localhost:8200

# Start with HTTP profile
java -jar application/target/application-1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev-http \
    "$@"