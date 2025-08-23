#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}      ngrok Configuration${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if token is provided as argument
if [ -n "$1" ]; then
    AUTH_TOKEN="$1"
    echo -e "${GREEN}Using provided auth token${NC}"
else
    echo "After completing the ngrok welcome flow, find your auth token at:"
    echo -e "${YELLOW}https://dashboard.ngrok.com/get-started/your-authtoken${NC}"
    echo ""
    echo "It will look like: 2nH9z..."
    echo ""
    read -p "Paste your ngrok auth token here: " AUTH_TOKEN
fi

if [ -n "$AUTH_TOKEN" ]; then
    echo -e "${YELLOW}Configuring ngrok...${NC}"
    ngrok config add-authtoken "$AUTH_TOKEN"
    echo -e "${GREEN}✓ ngrok configured successfully!${NC}"
    
    # Verify configuration
    if ngrok version > /dev/null 2>&1; then
        echo -e "${GREEN}✓ ngrok is working!${NC}"
        ngrok version
    fi
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}    Configuration Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${YELLOW}Now you can start the ngrok tunnel:${NC}"
    echo -e "${BLUE}ngrok http https://localhost:8443${NC}"
else
    echo -e "${RED}No token provided. Please get your token from:${NC}"
    echo "https://dashboard.ngrok.com/get-started/your-authtoken"
fi
