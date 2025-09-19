#!/bin/bash

# ================================================
# STRATEGIZ COMPLETE CLEAN BUILD & DEPLOY SCRIPT
# ================================================
# This script handles everything needed to start the backend:
# - Vault server startup and configuration
# - Secret management setup
# - Clean build of the application
# - Deployment with HTTPS
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
VAULT_DEV_ROOT_TOKEN="hvs.zjEbQaSDy8WmdiW0vEl81tFH"
APP_JAR="application/target/application-1.0-SNAPSHOT.jar"
SPRING_PROFILE="dev-https"
SERVER_PORT="8443"

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

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
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
    
    print_status "Setting up OAuth secrets in Vault..."
    
    # Google OAuth (dummy for dev)
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/google \
        client-id="dummy-google-client-id" \
        client-secret="dummy-google-client-secret" >/dev/null 2>&1 || true
    
    # Facebook OAuth (dummy for dev)
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/facebook \
        client-id="dummy-facebook-client-id" \
        client-secret="dummy-facebook-client-secret" >/dev/null 2>&1 || true
    
    # Coinbase OAuth
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/coinbase \
        client-id="88bf21ba-e7b5-4b75-8f94-c239046d20a4" \
        client-secret="ISl99wBG8yEDaxPt7TZXa44vv5" >/dev/null 2>&1 || true
    
    # Alpaca OAuth
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/oauth/alpaca \
        client-id="d83f0a003530c47ee81112c1406d97b3" \
        client-secret="2361726d43b0a5d1b982ef7409e50d93ed510ff3" \
        redirect-uri="https://localhost:8443/v1/providers/callback/alpaca" \
        auth-url="https://app.alpaca.markets/oauth/authorize" \
        token-url="https://api.alpaca.markets/oauth/token" \
        api-url="https://api.alpaca.markets" \
        scope="account:read trading:write data:read" >/dev/null 2>&1 || true
    
    # Token keys for PASETO
    VAULT_ADDR=$VAULT_ADDR VAULT_TOKEN=$token vault kv put secret/strategiz/tokens/dev \
        identity-key="rH2K8A3h+V9QHqUhAuuZKfLRcdXWiB7R5Al8ayMrMuU=" \
        session-key="yWTOP0BPnyYrPNpphYeTKzhwlURff4hesjP+hcv9Dqc=" >/dev/null 2>&1 || true
    
    print_success "Vault secrets configured"
}

# Header
echo -e "${CYAN}${BOLD}"
echo "============================================="
echo "   STRATEGIZ COMPLETE BUILD & DEPLOY"
echo "============================================="
echo -e "${NC}"

# Step 1: Check prerequisites
print_status "Checking prerequisites..."

if ! command_exists java; then
    print_error "Java is not installed. Please install Java 21 or higher."
    exit 1
fi

if ! command_exists mvn; then
    print_error "Maven is not installed. Please install Maven 3.8 or higher."
    exit 1
fi

if ! command_exists vault; then
    print_error "Vault is not installed. Please install HashiCorp Vault."
    echo "       Visit: https://www.vaultproject.io/downloads"
    exit 1
fi

print_success "Prerequisites check passed"

# Step 2: Check and start Vault if needed
print_status "Checking Vault status..."

if ! is_vault_running; then
    print_warning "Vault is not running. Starting Vault in dev mode..."
    
    # Kill any existing Vault processes
    pkill -f "vault server" 2>/dev/null || true
    sleep 2
    
    # Start Vault in dev mode with a known root token
    vault server -dev -dev-root-token-id="$VAULT_DEV_ROOT_TOKEN" > /tmp/vault.log 2>&1 &
    VAULT_PID=$!
    
    print_status "Waiting for Vault to start"
    if wait_for_vault; then
        print_success "Vault started successfully (PID: $VAULT_PID)"
    else
        print_error "Failed to start Vault. Check /tmp/vault.log for details"
        exit 1
    fi
else
    print_success "Vault is already running"
fi

# Step 3: Setup Vault secrets
setup_vault_secrets "$VAULT_DEV_ROOT_TOKEN"

# Step 4: Clean and build the application
print_status "Starting clean build..."
echo ""

# Run Maven clean install
if mvn clean install -DskipTests; then
    print_success "Build completed successfully"
else
    print_error "Build failed. Please check the Maven output above."
    exit 1
fi

# Step 5: Check if JAR was created
if [ ! -f "$APP_JAR" ]; then
    print_error "Application JAR not found at $APP_JAR"
    print_error "Build may have failed or JAR is in a different location"
    exit 1
fi

print_success "Application JAR found: $APP_JAR"

# Step 6: Kill any existing backend processes
print_status "Stopping any existing backend processes..."
pkill -f "application-1.0-SNAPSHOT.jar" 2>/dev/null || true
sleep 2

# Step 7: Start the backend application
echo ""
echo -e "${GREEN}${BOLD}=============================================${NC}"
echo -e "${GREEN}${BOLD}   STARTING STRATEGIZ BACKEND${NC}"
echo -e "${GREEN}${BOLD}=============================================${NC}"
echo ""

print_status "Starting backend with HTTPS on port $SERVER_PORT..."
print_status "Spring Profile: $SPRING_PROFILE"
print_status "Vault Token: $VAULT_DEV_ROOT_TOKEN"
echo ""

# Export environment variables
export VAULT_ADDR=$VAULT_ADDR
export VAULT_TOKEN=$VAULT_DEV_ROOT_TOKEN

# Start the application
java -jar "$APP_JAR" \
    --spring.profiles.active=$SPRING_PROFILE \
    --server.port=$SERVER_PORT \
    2>&1 | tee application.log &

APP_PID=$!

# Wait for application to start
print_status "Waiting for application to start..."
sleep 10

# Check if application started successfully
if ps -p $APP_PID > /dev/null; then
    # Check health endpoint
    if curl -k -s https://localhost:$SERVER_PORT/actuator/health | grep -q "UP"; then
        echo ""
        echo -e "${GREEN}${BOLD}=============================================${NC}"
        echo -e "${GREEN}${BOLD}   🚀 STRATEGIZ BACKEND STARTED SUCCESSFULLY${NC}"
        echo -e "${GREEN}${BOLD}=============================================${NC}"
        echo ""
        print_success "Backend is running on https://localhost:$SERVER_PORT"
        print_success "Swagger UI: https://localhost:$SERVER_PORT/swagger-ui/index.html"
        print_success "API Docs: https://localhost:$SERVER_PORT/v3/api-docs"
        print_success "Health: https://localhost:$SERVER_PORT/actuator/health"
        echo ""
        print_status "Application PID: $APP_PID"
        print_status "Vault PID: $VAULT_PID (if started by this script)"
        echo ""
        print_warning "To stop the backend: kill $APP_PID"
        print_warning "To view logs: tail -f application.log"
        echo ""
        echo -e "${CYAN}${BOLD}Vault Token for manual operations:${NC}"
        echo "$VAULT_DEV_ROOT_TOKEN"
        echo ""
    else
        print_error "Application started but health check failed"
        print_warning "Check application.log for details"
        exit 1
    fi
else
    print_error "Application failed to start"
    print_warning "Check application.log for details"
    exit 1
fi

echo -e "${PURPLE}${BOLD}Ready for development! 🎉${NC}"