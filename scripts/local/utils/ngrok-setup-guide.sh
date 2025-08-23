#!/bin/bash

#=============================================================================
# ngrok Setup Guide for Coinbase OAuth Testing
#=============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

print_header "ngrok Setup Guide for Coinbase OAuth"

echo -e "\n${RED}PROBLEM:${NC} Coinbase has disabled your OAuth app because it doesn't accept 'localhost' redirect URIs."
echo -e "${GREEN}SOLUTION:${NC} Use ngrok to create a public tunnel to your local server.\n"

echo -e "${BLUE}Step 1: Set up ngrok Account${NC}"
echo "1. Sign up for free at: ${CYAN}https://dashboard.ngrok.com/signup${NC}"
echo "2. Get your authtoken at: ${CYAN}https://dashboard.ngrok.com/get-started/your-authtoken${NC}"
echo "3. Configure ngrok with your token:"
echo -e "   ${YELLOW}ngrok config add-authtoken YOUR_TOKEN_HERE${NC}\n"

echo -e "${BLUE}Step 2: Start ngrok Tunnel${NC}"
echo "Run this command to create a tunnel to your local HTTPS server:"
echo -e "   ${YELLOW}ngrok http https://localhost:8443${NC}"
echo ""
echo "You'll see output like:"
echo -e "   ${CYAN}Forwarding  https://abc123xyz.ngrok-free.app -> https://localhost:8443${NC}"
echo "Copy the ngrok URL (e.g., https://abc123xyz.ngrok-free.app)\n"

echo -e "${BLUE}Step 3: Update Coinbase OAuth App${NC}"
echo "1. Go to: ${CYAN}https://www.coinbase.com/settings/api${NC}"
echo "2. Edit your OAuth application"
echo "3. ${RED}REMOVE${NC} the localhost redirect URI: https://localhost:8443/v1/providers/callback/coinbase"
echo "4. ${GREEN}ADD${NC} the ngrok redirect URI:"
echo -e "   ${YELLOW}https://YOUR-NGROK-URL.ngrok-free.app/v1/providers/callback/coinbase${NC}"
echo "5. Save and wait for Coinbase to re-enable your app (usually instant)\n"

echo -e "${BLUE}Step 4: Restart Application with ngrok Configuration${NC}"
echo "1. Stop the current application (Ctrl+C)"
echo "2. Set the ngrok URL environment variable:"
echo -e "   ${YELLOW}export NGROK_URL=https://YOUR-NGROK-URL.ngrok-free.app${NC}"
echo "3. Restart with ngrok profile:"
echo -e "   ${YELLOW}VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root java -jar application/target/application-1.0-SNAPSHOT.jar --spring.profiles.active=dev-ngrok${NC}\n"

echo -e "${BLUE}Step 5: Test Coinbase OAuth${NC}"
echo "Use the frontend or this curl command to test:"
echo -e "${YELLOW}curl -k -X POST https://YOUR-NGROK-URL.ngrok-free.app/v1/providers \\
  -H 'Content-Type: application/json' \\
  -H 'X-User-Id: test-user-123' \\
  -d '{
    \"providerId\": \"coinbase\",
    \"connectionType\": \"oauth\",
    \"accountType\": \"live\"
  }'${NC}\n"

echo -e "${GREEN}Alternative: Use 127.0.0.1 instead of localhost${NC}"
echo "Some OAuth providers accept 127.0.0.1 but not localhost:"
echo "1. Try updating Coinbase redirect URI to:"
echo -e "   ${YELLOW}https://127.0.0.1:8443/v1/providers/callback/coinbase${NC}"
echo "2. Update your hosts file if needed:"
echo -e "   ${YELLOW}sudo echo '127.0.0.1 local.strategiz.io' >> /etc/hosts${NC}"
echo "3. Use https://local.strategiz.io:8443 as redirect URI\n"

echo -e "${CYAN}Current Status:${NC}"
# Check if ngrok is configured
if [ -f "$HOME/.config/ngrok/ngrok.yml" ] || [ -f "$HOME/Library/Application Support/ngrok/ngrok.yml" ]; then
    print_success "ngrok is configured with authtoken"
else
    print_warning "ngrok authtoken not configured yet"
fi

# Check if application is running
if lsof -i:8443 > /dev/null 2>&1; then
    print_success "Application is running on port 8443"
else
    print_warning "Application is not running on port 8443"
fi

echo -e "\n${GREEN}Quick Commands:${NC}"
echo "# Configure ngrok (one-time setup):"
echo "ngrok config add-authtoken YOUR_TOKEN"
echo ""
echo "# Start ngrok tunnel:"
echo "ngrok http https://localhost:8443"
echo ""
echo "# Restart app with ngrok URL:"
echo "export NGROK_URL=https://YOUR-URL.ngrok-free.app"
echo "VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root java -jar application/target/application-1.0-SNAPSHOT.jar --spring.profiles.active=dev-ngrok"
echo ""

print_success "Setup guide complete! Follow the steps above to configure ngrok for Coinbase OAuth."