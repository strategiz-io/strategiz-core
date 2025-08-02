#!/bin/bash

#=============================================================================
# Strategiz Core - Development Environment Startup Script
# Starts Vault and Backend together for local development
#=============================================================================

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Starting Strategiz Development Environment...${NC}"

# Start Vault if not already running
if ! curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo -e "${YELLOW}Starting Vault...${NC}"
    ./scripts/deployment/local/start-vault-local.sh
    
    # Wait a moment for Vault to fully initialize
    sleep 2
else
    echo -e "${GREEN}Vault is already running${NC}"
fi

# Export Vault credentials
export VAULT_TOKEN="strategiz-local-token"
export VAULT_ADDR='http://localhost:8200'

echo -e "${YELLOW}Starting Backend Application...${NC}"

# Check if we need to build
if [ ! -f "application/target/application-1.0-SNAPSHOT.jar" ]; then
    echo "Building application first..."
    mvn clean install -DskipTests
fi

# Start the backend
cd application
mvn spring-boot:run -Dspring.profiles.active=dev

echo -e "${GREEN}Development environment stopped.${NC}"