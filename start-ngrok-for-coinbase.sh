#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Starting ngrok for Coinbase OAuth${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if ngrok is installed
if ! command -v ngrok &> /dev/null; then
    echo -e "${RED}ngrok is not installed!${NC}"
    echo "Install it with: brew install ngrok"
    echo "Then configure: ngrok config add-authtoken YOUR_TOKEN"
    exit 1
fi

echo -e "${GREEN}Starting ngrok tunnel to https://localhost:8443...${NC}"
echo ""
echo -e "${YELLOW}IMPORTANT: After ngrok starts:${NC}"
echo "1. Copy the HTTPS URL (e.g., https://abc123.ngrok-free.app)"
echo "2. Update Coinbase OAuth app redirect URI to:"
echo "   https://YOUR-NGROK-URL.ngrok-free.app/v1/providers/callback/coinbase"
echo "3. Keep this terminal open while testing"
echo ""
echo -e "${GREEN}Starting ngrok...${NC}"

# Start ngrok for HTTPS backend
ngrok http https://localhost:8443
