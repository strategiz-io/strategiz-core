#!/bin/bash

# Test script for Kraken OAuth integration in signup step 3
# This script helps test the complete OAuth flow

set -e

BASE_URL="http://localhost:8080"
FRONTEND_URL="http://localhost:3000"

echo "🚀 Testing Kraken OAuth Integration for Signup Step 3"
echo "=================================================="

# Function to check if the server is running
check_server() {
    echo "📡 Checking if server is running..."
    if curl -s -f "${BASE_URL}/actuator/health" > /dev/null; then
        echo "✅ Server is running"
    else
        echo "❌ Server is not running. Please start the application first."
        echo "   Run: mvn spring-boot:run from the application directory"
        exit 1
    fi
}

# Function to check available providers
check_available_providers() {
    echo "🔍 Checking available providers..."
    
    response=$(curl -s -w "%{http_code}" "${BASE_URL}/api/signup/provider/available" -o /tmp/providers_response.json)
    http_code="${response: -3}"
    
    if [ "$http_code" = "200" ]; then
        echo "✅ Available providers endpoint working"
        echo "📋 Response:"
        cat /tmp/providers_response.json | jq '.' 2>/dev/null || cat /tmp/providers_response.json
        echo ""
    else
        echo "❌ Available providers endpoint failed with status: $http_code"
        cat /tmp/providers_response.json 2>/dev/null || echo "No response body"
        return 1
    fi
}

# Function to test provider connection initiation (requires auth token)
test_provider_connection() {
    echo "🔐 Testing provider connection initiation..."
    echo "Note: This requires a valid authentication token"
    echo "You can get one by completing steps 1 and 2 of signup first"
    echo ""
    
    # This would require a real auth token, so we'll just show the curl command
    echo "💡 To test with a real auth token, use this command:"
    echo "curl -X POST '${BASE_URL}/api/signup/provider/kraken/connect' \\"
    echo "  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \\"
    echo "  -H 'Content-Type: application/json'"
    echo ""
}

# Function to check OAuth configuration
check_oauth_config() {
    echo "⚙️  Checking OAuth configuration..."
    
    echo "📝 Required environment variables:"
    echo "   PROVIDER_KRAKEN_CLIENT_ID: ${PROVIDER_KRAKEN_CLIENT_ID:-'❌ Not set'}"
    echo "   PROVIDER_KRAKEN_CLIENT_SECRET: ${PROVIDER_KRAKEN_CLIENT_SECRET:-'❌ Not set'}"
    echo ""
    
    if [ -z "$PROVIDER_KRAKEN_CLIENT_ID" ] || [ -z "$PROVIDER_KRAKEN_CLIENT_SECRET" ]; then
        echo "⚠️  WARNING: Kraken OAuth credentials not configured"
        echo "   To set them up:"
        echo "   1. Register your app at https://www.kraken.com/features/api"
        echo "   2. Set environment variables:"
        echo "      export PROVIDER_KRAKEN_CLIENT_ID='your_client_id'"
        echo "      export PROVIDER_KRAKEN_CLIENT_SECRET='your_client_secret'"
        echo "   3. Restart the application"
        echo ""
    else
        echo "✅ Kraken OAuth credentials are configured"
    fi
}

# Function to show the complete OAuth flow
show_oauth_flow() {
    echo "🔄 Complete OAuth Flow for Signup Step 3:"
    echo "========================================="
    echo ""
    echo "1. 📝 User reaches signup step 3 at: ${FRONTEND_URL}/signup/step3"
    echo ""
    echo "2. 🔗 Frontend calls: POST ${BASE_URL}/api/signup/provider/kraken/connect"
    echo "   - Requires: Valid access token from step 2"
    echo "   - Returns: OAuth authorization URL"
    echo ""
    echo "3. 🌐 User is redirected to Kraken OAuth page"
    echo "   - URL format: https://auth.kraken.com/oauth2/authorize?..."
    echo "   - User logs in and authorizes Strategiz"
    echo ""
    echo "4. ↩️  Kraken redirects back to: ${BASE_URL}/api/v1/provider/callback/kraken"
    echo "   - With authorization code and state"
    echo "   - Our callback controller handles the exchange"
    echo ""
    echo "5. ✅ On success, user is redirected to: ${FRONTEND_URL}/signup/step3?success=true"
    echo "   - Frontend detects success and calls completion endpoint"
    echo ""
    echo "6. 🏁 Frontend calls: POST ${BASE_URL}/api/signup/provider/complete-with-provider"
    echo "   - Completes signup with provider connection"
    echo "   - User proceeds to dashboard"
    echo ""
}

# Function to test callback endpoint (mock)
test_callback_endpoint() {
    echo "🧪 Testing callback endpoint with mock data..."
    
    # Test with missing parameters
    response=$(curl -s -w "%{http_code}" "${BASE_URL}/api/v1/provider/callback/kraken" -o /tmp/callback_response.json)
    http_code="${response: -3}"
    
    if [ "$http_code" = "302" ]; then
        echo "✅ Callback endpoint responding (redirects as expected)"
        location=$(curl -s -I "${BASE_URL}/api/v1/provider/callback/kraken" | grep -i location | cut -d' ' -f2- | tr -d '\r')
        echo "📍 Redirects to: ${location}"
    else
        echo "⚠️  Callback endpoint response: $http_code"
    fi
    echo ""
}

# Run all tests
main() {
    check_server
    echo ""
    
    check_oauth_config
    echo ""
    
    check_available_providers
    echo ""
    
    test_provider_connection
    echo ""
    
    test_callback_endpoint
    echo ""
    
    show_oauth_flow
    
    echo "🎉 Test script completed!"
    echo ""
    echo "📚 Next steps to test the full integration:"
    echo "1. Set up Kraken OAuth credentials (if not done)"
    echo "2. Complete signup steps 1-2 to get an access token"
    echo "3. Test the provider connection from signup step 3"
    echo "4. Verify the complete OAuth flow works end-to-end"
    echo ""
    echo "💡 For debugging, check application logs during OAuth flow"
}

# Cleanup function
cleanup() {
    rm -f /tmp/providers_response.json /tmp/callback_response.json
}

# Set up cleanup on exit
trap cleanup EXIT

# Run the main function
main 