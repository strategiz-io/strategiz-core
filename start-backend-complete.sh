#!/bin/bash

# ================================================
# STRATEGIZ QUICK START BACKEND SCRIPT
# ================================================
# Quick script to start the backend with all dependencies
# Assumes the application is already built
# ================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Configuration
VAULT_ADDR="http://localhost:8200"
VAULT_DEV_ROOT_TOKEN="strategiz-local-token"
APP_JAR="application/target/application-1.0-SNAPSHOT.jar"
SPRING_PROFILE="${PROFILE:-dev-https}"  # Can override with PROFILE env var
SERVER_PORT="${PORT:-8443}"              # Can override with PORT env var

# Function to print colored messages
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to check if Vault is running
is_vault_running() {
    VAULT_ADDR=$VAULT_ADDR vault status >/dev/null 2>&1
    return $?
}

# Function to wait for Vault to be ready
wait_for_vault() {
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if is_vault_running; then
            return 0
        fi
        echo -n "."
        sleep 1
        attempt=$((attempt + 1))
    done
    
    return 1
}

# Function to setup Vault secrets
setup_vault_secrets() {
    local token=$1
    
    print_status "Configuring Vault secrets..."
    
    # OAuth Providers
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/google \
        client-id="dummy-google-client-id" \
        client-secret="dummy-google-client-secret" >/dev/null 2>&1 || true
    
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/facebook \
        client-id="dummy-facebook-client-id" \
        client-secret="dummy-facebook-client-secret" >/dev/null 2>&1 || true
    
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/coinbase \
        client-id="88bf21ba-e7b5-4b75-8f94-c239046d20a4" \
        client-secret="ISl99wBG8yEDaxPt7TZXa44vv5" >/dev/null 2>&1 || true
    
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/alpaca \
        client-id="d83f0a003530c47ee81112c1406d97b3" \
        client-secret="2361726d43b0a5d1b982ef7409e50d93ed510ff3" \
        redirect-uri="https://localhost:8443/v1/providers/callback/alpaca" \
        auth-url="https://app.alpaca.markets/oauth/authorize" \
        token-url="https://api.alpaca.markets/oauth/token" \
        api-url="https://api.alpaca.markets" \
        scope="account:read trading:write data:read" >/dev/null 2>&1 || true
    
    # PASETO Token Keys
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/tokens/dev \
        identity-key="rH2K8A3h+V9QHqUhAuuZKfLRcdXWiB7R5Al8ayMrMuU=" \
        session-key="yWTOP0BPnyYrPNpphYeTKzhwlURff4hesjP+hcv9Dqc=" >/dev/null 2>&1 || true
    
    print_success "Vault secrets ready"
}

# Header
echo -e "${CYAN}${BOLD}"
echo "============================================="
echo "   STRATEGIZ BACKEND QUICK START"
echo "============================================="
echo -e "${NC}"

# Check if JAR exists
if [ ! -f "$APP_JAR" ]; then
    print_error "Application JAR not found at $APP_JAR"
    print_warning "Please run './clean-build-deploy-complete.sh' first to build the application"
    exit 1
fi

# Start or verify Vault
print_status "Checking Vault..."

if ! is_vault_running; then
    print_warning "Starting Vault..."
    
    # Kill any existing Vault processes
    pkill -f "vault server" 2>/dev/null || true
    sleep 2
    
    # Start Vault in dev mode
    vault server -dev -dev-root-token-id="$VAULT_DEV_ROOT_TOKEN" > /tmp/vault.log 2>&1 &
    VAULT_PID=$!
    
    if wait_for_vault; then
        print_success "Vault started (PID: $VAULT_PID)"
    else
        print_error "Failed to start Vault"
        exit 1
    fi
else
    print_success "Vault is running"
fi

# Setup Vault secrets
setup_vault_secrets "$VAULT_DEV_ROOT_TOKEN"

# Kill any existing backend
print_status "Stopping any existing backend..."
pkill -f "application-1.0-SNAPSHOT.jar" 2>/dev/null || true
sleep 2

# Start the backend
echo ""
echo -e "${GREEN}${BOLD}=============================================${NC}"
echo -e "${GREEN}${BOLD}   STARTING BACKEND${NC}"
echo -e "${GREEN}${BOLD}=============================================${NC}"
echo ""

export VAULT_ADDR=$VAULT_ADDR
export VAULT_TOKEN=$VAULT_DEV_ROOT_TOKEN

print_status "Profile: $SPRING_PROFILE"
print_status "Port: $SERVER_PORT"
echo ""

# Start the application
java -jar "$APP_JAR" \
    --spring.profiles.active=$SPRING_PROFILE \
    --server.port=$SERVER_PORT