#!/bin/bash

# Setup script for Google Gemini API key in Vault

echo "========================================="
echo "Google Gemini API Key Setup for Strategiz"
echo "========================================="
echo ""

# Check if Vault is running
if ! curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo "❌ Error: Vault is not running on http://localhost:8200"
    echo ""
    echo "Please start Vault first:"
    echo "  vault server -dev"
    echo ""
    exit 1
fi

echo "✅ Vault is running"
echo ""

# Get Gemini API key from user or environment
if [ -z "$GEMINI_API_KEY" ]; then
    echo "📋 How to get your Google Gemini API key:"
    echo ""
    echo "1. Go to: https://makersuite.google.com/app/apikey"
    echo "2. Sign in with your Google account"
    echo "3. Click 'Get API Key' or 'Create API Key'"
    echo "4. Copy the API key"
    echo ""
    echo "Note: Gemini has a generous free tier!"
    echo ""
    read -p "Enter your Gemini API key: " GEMINI_API_KEY

    if [ -z "$GEMINI_API_KEY" ]; then
        echo "❌ No API key provided. Exiting."
        exit 1
    fi
fi

# Set Vault address and token
export VAULT_ADDR=http://localhost:8200

# Check if VAULT_TOKEN is set, otherwise use 'root' for dev
if [ -z "$VAULT_TOKEN" ]; then
    echo "⚠️  VAULT_TOKEN not set, using 'root' for local development"
    export VAULT_TOKEN=root
fi

echo ""
echo "🔐 Storing Gemini API key in Vault..."

# Store in Vault
vault kv put secret/strategiz/gemini api-key="$GEMINI_API_KEY"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Gemini API key successfully stored in Vault!"
    echo ""
    echo "📍 Vault path: secret/strategiz/gemini"
    echo ""
    echo "To verify, run:"
    echo "  vault kv get secret/strategiz/gemini"
    echo ""
    echo "🚀 You're ready to use AI chat in Strategiz!"
else
    echo ""
    echo "❌ Failed to store API key in Vault"
    echo "Please check your Vault connection and try again"
    exit 1
fi
