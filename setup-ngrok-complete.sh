#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}     Complete ngrok Setup Script${NC}"
echo -e "${BLUE}========================================${NC}"

# Step 1: Check/Install ngrok
if ! command -v ngrok &> /dev/null; then
    echo -e "${YELLOW}Installing ngrok via Homebrew...${NC}"
    brew install ngrok
else
    echo -e "${GREEN}✓ ngrok is already installed${NC}"
fi

# Step 2: Configure auth token
echo ""
echo -e "${YELLOW}Step 1: Get your auth token${NC}"
echo "1. Sign up at: https://dashboard.ngrok.com/signup"
echo "2. Get token at: https://dashboard.ngrok.com/get-started/your-authtoken"
echo ""
read -p "Enter your ngrok auth token: " AUTH_TOKEN

if [ -n "$AUTH_TOKEN" ]; then
    echo -e "${YELLOW}Configuring ngrok with your auth token...${NC}"
    ngrok config add-authtoken $AUTH_TOKEN
    echo -e "${GREEN}✓ ngrok configured successfully!${NC}"
else
    echo -e "${RED}No auth token provided. Skipping configuration.${NC}"
    exit 1
fi

# Step 3: Test ngrok
echo ""
echo -e "${BLUE}Testing ngrok configuration...${NC}"
ngrok version

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}     ngrok Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Start ngrok tunnel:"
echo "   ${BLUE}ngrok http https://localhost:8443${NC}"
echo ""
echo "2. Copy the URL (e.g., https://abc123.ngrok-free.app)"
echo ""
echo "3. Update Coinbase OAuth redirect URI to:"
echo "   ${BLUE}https://YOUR-URL.ngrok-free.app/v1/providers/callback/coinbase${NC}"
echo ""
echo "4. Start backend with ngrok:"
echo "   ${BLUE}./start-backend-with-ngrok.sh https://YOUR-URL.ngrok-free.app${NC}"
