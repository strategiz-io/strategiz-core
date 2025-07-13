#!/bin/bash

echo "🔍 Testing OAuth Configuration"
echo "=============================="
echo ""

# Test 1: Check if environment variables are set
echo "1. Checking OAuth environment variables:"
echo "   AUTH_GOOGLE_CLIENT_ID: ${AUTH_GOOGLE_CLIENT_ID:-Not set}"
echo "   AUTH_GOOGLE_CLIENT_SECRET: ${AUTH_GOOGLE_CLIENT_SECRET:-Not set}"
echo ""

# Test 2: Check if Vault is accessible
echo "2. Checking Vault connection:"
if command -v vault &> /dev/null; then
    echo "   Vault CLI: ✅ Installed"
    if curl -s http://localhost:8200/v1/sys/health &> /dev/null; then
        echo "   Vault Server: ✅ Running on http://localhost:8200"
        
        # Try to read from Vault if token is set
        if [ ! -z "$VAULT_TOKEN" ]; then
            echo "   Vault Token: ✅ Set"
            # Try to read Google OAuth from Vault
            if vault kv get secret/strategiz/oauth/google 2>/dev/null; then
                echo "   Google OAuth in Vault: ✅ Found"
            else
                echo "   Google OAuth in Vault: ❌ Not found"
            fi
        else
            echo "   Vault Token: ❌ Not set"
        fi
    else
        echo "   Vault Server: ❌ Not running"
    fi
else
    echo "   Vault CLI: ❌ Not installed"
fi
echo ""

# Test 3: Quick fix suggestion
echo "3. Quick Fix Options:"
echo ""
echo "Option A - Set environment variables directly:"
echo "   export AUTH_GOOGLE_CLIENT_ID=\"your-client-id\""
echo "   export AUTH_GOOGLE_CLIENT_SECRET=\"your-client-secret\""
echo ""
echo "Option B - Use Vault (recommended for production):"
echo "   # Start Vault"
echo "   vault server -dev"
echo "   # In another terminal:"
echo "   export VAULT_TOKEN=root-token"
echo "   vault kv put secret/strategiz/oauth/google \\"
echo "     client-id=\"your-client-id\" \\"
echo "     client-secret=\"your-client-secret\""
echo ""
echo "Option C - Create a development .env file:"
echo "   ./setup-oauth-dev.sh"
echo ""
echo "After setting credentials, restart the backend application."