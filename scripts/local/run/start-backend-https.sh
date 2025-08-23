#!/bin/bash

# Start backend with HTTPS configuration (port 8443)
echo "🚀 Starting Strategiz Backend with HTTPS on port 8443..."
echo "📋 Profile: dev-https"
echo "🔗 URL: https://localhost:8443"
echo "🔒 Note: Using self-signed certificate. Accept browser warning."
echo ""

# Ensure Vault is configured
if [ -z "$VAULT_TOKEN" ]; then
    echo "⚠️  VAULT_TOKEN not set. Using default 'root' token for development."
    export VAULT_TOKEN=root
fi

export VAULT_ADDR=http://localhost:8200

# Start with HTTPS profile
java -jar application/target/application-1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev-https \
    "$@"