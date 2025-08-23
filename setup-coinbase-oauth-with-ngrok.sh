#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Setting up Coinbase OAuth with ngrok${NC}"

# Step 1: Check if ngrok is installed
if ! command -v ngrok &> /dev/null; then
    echo "Installing ngrok..."
    brew install ngrok
fi

# Step 2: Start ngrok
echo -e "${YELLOW}Starting ngrok tunnel to https://localhost:8443...${NC}"
echo "Copy the HTTPS URL from ngrok output (e.g., https://abc123.ngrok-free.app)"
echo ""
echo "Then update your Coinbase OAuth app redirect URI to:"
echo "https://YOUR-NGROK-URL.ngrok-free.app/v1/providers/callback/coinbase"
echo ""
echo "Press Ctrl+C to stop the tunnel when done"

ngrok http https://localhost:8443
