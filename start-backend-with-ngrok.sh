#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Starting Backend with ngrok Config${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if ngrok URL is provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: ngrok URL not provided!${NC}"
    echo ""
    echo "Usage: ./start-backend-with-ngrok.sh https://YOUR-NGROK-URL.ngrok-free.app"
    echo ""
    echo "Steps:"
    echo "1. Run ./start-ngrok-for-coinbase.sh in another terminal"
    echo "2. Copy the ngrok URL"
    echo "3. Run this script with the URL"
    exit 1
fi

NGROK_URL=$1

echo -e "${GREEN}Using ngrok URL: ${NGROK_URL}${NC}"
echo ""

# Check if Vault is running
if ! curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo -e "${YELLOW}Starting Vault in dev mode...${NC}"
    vault server -dev &
    sleep 2
    export VAULT_TOKEN=root
else
    echo -e "${GREEN}Vault is already running${NC}"
fi

# Update Vault with correct redirect URI
echo -e "${YELLOW}Updating Coinbase OAuth redirect URI in Vault...${NC}"
VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv put secret/strategiz/oauth/coinbase \
    client-id="88bf21ba-e7b5-4b75-8f94-c239046d20a4" \
    client-secret="ISl99wBG8yEDaxPt7TZXa44vv5" \
    redirect-uri="${NGROK_URL}/v1/providers/callback/coinbase"

echo ""
echo -e "${GREEN}Starting backend with ngrok configuration...${NC}"
echo -e "${YELLOW}OAuth redirect URI: ${NGROK_URL}/v1/providers/callback/coinbase${NC}"
echo ""

# Start the backend with ngrok profile
VAULT_ADDR=http://localhost:8200 \
VAULT_TOKEN=root \
NGROK_URL=$NGROK_URL \
java -jar application/target/application-1.0-SNAPSHOT.jar \
    --spring.profiles.active=dev-ngrok \
    --server.port=8443 \
    --oauth.providers.coinbase.redirect-uri=${NGROK_URL}/v1/providers/callback/coinbase
