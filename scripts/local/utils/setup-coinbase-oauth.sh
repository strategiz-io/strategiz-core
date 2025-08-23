#!/bin/bash

#=============================================================================
# Coinbase OAuth Setup Helper
# This script helps configure and test Coinbase OAuth integration
#=============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}===================================================================="
    echo -e "  $1"
    echo -e "====================================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header "Coinbase OAuth Setup Helper"

echo -e "\n${YELLOW}Current Configuration:${NC}"
echo "• Backend running at: https://localhost:8443"
echo "• Redirect URI: https://localhost:8443/v1/providers/callback/coinbase"
echo ""

echo -e "${YELLOW}Known Issue:${NC}"
echo "Coinbase returns error code FPWTPR (500 Internal Server Error) after consent."
echo "This typically means the redirect URI doesn't match what's registered in Coinbase."
echo ""

echo -e "${BLUE}To fix this issue, you have several options:${NC}"
echo ""

echo -e "${GREEN}Option 1: Update Coinbase App Settings${NC}"
echo "1. Go to: https://www.coinbase.com/settings/api"
echo "2. Find your OAuth application"
echo "3. Add this exact redirect URI:"
echo "   ${YELLOW}https://localhost:8443/v1/providers/callback/coinbase${NC}"
echo ""
echo "Note: Coinbase may not accept 'localhost' URLs. If rejected, try Option 2."
echo ""

echo -e "${GREEN}Option 2: Use ngrok Tunnel (Recommended for testing)${NC}"
echo "1. Install ngrok: brew install ngrok"
echo "2. Start ngrok tunnel: ngrok http https://localhost:8443"
echo "3. Copy the ngrok URL (e.g., https://abc123.ngrok.io)"
echo "4. Update redirect URI in Coinbase to:"
echo "   ${YELLOW}https://YOUR-NGROK-URL.ngrok.io/v1/providers/callback/coinbase${NC}"
echo "5. Update application-dev.properties with the ngrok URL"
echo ""

echo -e "${GREEN}Option 3: Use Alternative Testing Method${NC}"
echo "For development, you can test the OAuth flow manually:"
echo ""
echo "1. Get the authorization URL from the create provider response"
echo "2. Complete the OAuth flow in browser"
echo "3. When redirected to localhost (which will fail to load), copy the URL"
echo "4. Extract the 'code' parameter from the URL"
echo "5. Manually call the callback endpoint with the code"
echo ""

echo -e "${BLUE}Testing Steps:${NC}"
echo "1. Ensure Vault is running with OAuth secrets configured"
echo "2. Start the backend application"
echo "3. Create a provider connection via API"
echo "4. Follow the OAuth URL returned"
echo "5. Complete authorization on Coinbase"
echo "6. Check application logs for callback processing"
echo ""

echo -e "${YELLOW}Debug Information:${NC}"
echo "• Check backend logs: Look for 'OAuth callback from provider: coinbase'"
echo "• Check for token exchange errors in logs"
echo "• Verify client ID and secret in Vault:"
echo "  ${BLUE}VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv get secret/strategiz/oauth/coinbase${NC}"
echo ""

# Check current OAuth configuration
echo -e "${BLUE}Current OAuth Configuration in Vault:${NC}"
VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv get secret/strategiz/oauth/coinbase 2>/dev/null || echo "Unable to fetch Vault secrets"

echo ""
echo -e "${GREEN}Quick Test Command:${NC}"
echo "curl -k -X POST https://localhost:8443/v1/providers \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -H 'X-User-Id: test-user-123' \\"
echo "  -d '{"
echo '    "providerId": "coinbase",'
echo '    "connectionType": "oauth",'
echo '    "accountType": "live"'
echo "  }'"
echo ""

print_success "Setup guide complete! Follow the options above to configure Coinbase OAuth."