#!/bin/bash

# Quick OAuth Environment Setup
echo "🔐 Setting up OAuth environment variables..."
echo ""

# Google OAuth
export AUTH_GOOGLE_CLIENT_ID="YOUR_GOOGLE_CLIENT_ID_HERE"
export AUTH_GOOGLE_CLIENT_SECRET="YOUR_GOOGLE_CLIENT_SECRET_HERE"
export AUTH_GOOGLE_REDIRECT_URI="http://localhost:8080/auth/oauth/google/callback"

# Facebook OAuth (optional)
export AUTH_FACEBOOK_CLIENT_ID="YOUR_FACEBOOK_APP_ID_HERE"
export AUTH_FACEBOOK_CLIENT_SECRET="YOUR_FACEBOOK_APP_SECRET_HERE"
export AUTH_FACEBOOK_REDIRECT_URI="http://localhost:8080/auth/oauth/facebook/callback"

# Frontend URL
export FRONTEND_URL="http://localhost:3000"

echo "✅ OAuth environment variables set!"
echo ""
echo "To verify:"
echo "  AUTH_GOOGLE_CLIENT_ID: ${AUTH_GOOGLE_CLIENT_ID:0:20}..."
echo "  AUTH_GOOGLE_CLIENT_SECRET: ***hidden***"
echo ""
echo "Now run: mvn spring-boot:run -pl application"