#\!/bin/bash

echo "🔐 Setting up Development Vault"
echo "================================"

# Start Vault in dev mode if not running
if \! vault status > /dev/null 2>&1; then
    echo "Starting Vault in dev mode..."
    vault server -dev &
    sleep 2
fi

# Dev mode uses "root" token by default
echo ""
echo "✅ Development Setup Complete\!"
echo ""
echo "For local development, use:"
echo "  export VAULT_TOKEN=root"
echo ""
echo "Or add to your shell profile (~/.zshrc or ~/.bashrc):"
echo "  export VAULT_TOKEN=root"
echo "  export VAULT_ADDR=http://localhost:8200"
