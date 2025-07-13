#!/bin/bash

# OAuth Development Setup Script
# This script helps set up OAuth credentials for local development

echo "🔐 OAuth Development Setup for Strategiz"
echo "========================================"
echo ""

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Please run this script from the strategiz-core root directory"
    exit 1
fi

echo "This script will help you set up OAuth credentials for local development."
echo ""
echo "📋 You will need:"
echo "  1. Google OAuth Client ID and Secret"
echo "  2. Facebook OAuth App ID and Secret (optional)"
echo ""
echo "🔍 If you don't have these yet, follow the setup guide:"
echo "  docs/oauth-setup.md"
echo ""

read -p "Do you want to continue? (y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Setup cancelled."
    exit 1
fi

echo ""
echo "🔧 Setting up Google OAuth..."
echo ""

# Google OAuth setup
read -p "Enter your Google OAuth Client ID: " GOOGLE_CLIENT_ID
read -p "Enter your Google OAuth Client Secret: " GOOGLE_CLIENT_SECRET

if [ -z "$GOOGLE_CLIENT_ID" ] || [ -z "$GOOGLE_CLIENT_SECRET" ]; then
    echo "❌ Google OAuth credentials are required!"
    exit 1
fi

# Optional Facebook setup
echo ""
echo "🔧 Setting up Facebook OAuth (optional)..."
echo ""
read -p "Enter your Facebook App ID (or press Enter to skip): " FACEBOOK_CLIENT_ID
if [ ! -z "$FACEBOOK_CLIENT_ID" ]; then
    read -p "Enter your Facebook App Secret: " FACEBOOK_CLIENT_SECRET
fi

# Create .env file for development
echo ""
echo "📝 Creating development environment file..."

cat > .env << EOF
# OAuth Development Configuration
# Generated on $(date)

# Google OAuth
export AUTH_GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID"
export AUTH_GOOGLE_CLIENT_SECRET="$GOOGLE_CLIENT_SECRET"
export AUTH_GOOGLE_REDIRECT_URI="http://localhost:8080/auth/oauth/google/callback"

EOF

if [ ! -z "$FACEBOOK_CLIENT_ID" ]; then
    cat >> .env << EOF
# Facebook OAuth
export AUTH_FACEBOOK_CLIENT_ID="$FACEBOOK_CLIENT_ID"
export AUTH_FACEBOOK_CLIENT_SECRET="$FACEBOOK_CLIENT_SECRET"
export AUTH_FACEBOOK_REDIRECT_URI="http://localhost:8080/auth/oauth/facebook/callback"

EOF
fi

cat >> .env << EOF
# Frontend URL
export FRONTEND_URL="http://localhost:3000"

# Vault Configuration (for local development)
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="root-token"
EOF

# Make the file executable for sourcing
chmod +x .env

echo ""
echo "✅ Environment file created: .env"
echo ""
echo "🚀 To start the application with OAuth:"
echo "  1. Source the environment variables:"
echo "     source .env"
echo ""
echo "  2. Start Vault (in a separate terminal):"
echo "     vault server -dev"
echo ""
echo "  3. Start the backend:"
echo "     mvn spring-boot:run -pl application"
echo ""
echo "  4. Start the frontend (in strategiz-ui directory):"
echo "     cd ../strategiz-ui && npm start"
echo ""
echo "📖 For more details, see docs/oauth-setup.md"
echo ""
echo "⚠️  Remember:"
echo "   - Never commit .env to version control"
echo "   - Use Vault for production secrets"
echo "   - Keep your OAuth secrets secure"