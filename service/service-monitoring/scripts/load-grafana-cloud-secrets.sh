#!/bin/bash
# =============================================================================
# Load Grafana Cloud Secrets into Vault
# =============================================================================
#
# This script stores Grafana Cloud OTLP credentials in Vault for production use.
#
# Prerequisites:
# 1. Sign up for Grafana Cloud: https://grafana.com/products/cloud/
# 2. Create a service account with "MetricsPublisher" role
# 3. Generate an API token for the service account
# 4. Get your OTLP endpoint from: Grafana Cloud > My Account > OTLP Gateway
#
# Usage:
#   export VAULT_ADDR=https://strategiz-vault-43628135674.us-central1.run.app
#   export VAULT_TOKEN=<your-vault-token>
#   ./load-grafana-cloud-secrets.sh
#
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Grafana Cloud Secret Loader ===${NC}"
echo ""

# Check Vault connection
if [ -z "$VAULT_ADDR" ]; then
    echo -e "${RED}Error: VAULT_ADDR environment variable not set${NC}"
    exit 1
fi

if [ -z "$VAULT_TOKEN" ]; then
    echo -e "${RED}Error: VAULT_TOKEN environment variable not set${NC}"
    exit 1
fi

# Check if vault is accessible
if ! vault status >/dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to Vault at $VAULT_ADDR${NC}"
    exit 1
fi

echo -e "${GREEN}Connected to Vault at $VAULT_ADDR${NC}"
echo ""

# Prompt for Grafana Cloud credentials
echo -e "${YELLOW}Enter your Grafana Cloud OTLP credentials:${NC}"
echo ""
echo "You can find these at: https://grafana.com/docs/grafana-cloud/monitor-infrastructure/otlp/"
echo ""

read -p "OTLP Gateway URL (e.g., https://otlp-gateway-prod-us-central-0.grafana.net/otlp): " OTLP_ENDPOINT
read -p "Instance ID (numeric, from your Grafana Cloud stack): " INSTANCE_ID
read -sp "API Token (from Service Account): " API_TOKEN
echo ""

# Validate inputs
if [ -z "$OTLP_ENDPOINT" ] || [ -z "$INSTANCE_ID" ] || [ -z "$API_TOKEN" ]; then
    echo -e "${RED}Error: All fields are required${NC}"
    exit 1
fi

# Create Base64 encoded auth header (Basic Auth format)
AUTH_HEADER=$(echo -n "${INSTANCE_ID}:${API_TOKEN}" | base64)

echo ""
echo -e "${YELLOW}Storing credentials in Vault...${NC}"

# Store in Vault
vault kv put secret/strategiz/grafana-cloud \
    otlp-endpoint="$OTLP_ENDPOINT" \
    instance-id="$INSTANCE_ID" \
    api-token="$API_TOKEN" \
    auth-header="$AUTH_HEADER"

echo ""
echo -e "${GREEN}Success! Grafana Cloud credentials stored in Vault${NC}"
echo ""
echo -e "${YELLOW}Vault path:${NC} secret/strategiz/grafana-cloud"
echo ""
echo -e "${YELLOW}Keys stored:${NC}"
echo "  - otlp-endpoint: $OTLP_ENDPOINT"
echo "  - instance-id: $INSTANCE_ID"
echo "  - api-token: ******* (hidden)"
echo "  - auth-header: ******* (Base64 encoded)"
echo ""
echo -e "${YELLOW}Cloud Run environment variables to set:${NC}"
echo "  GRAFANA_OTLP_ENDPOINT=$OTLP_ENDPOINT"
echo "  GRAFANA_OTLP_AUTH_HEADER=$AUTH_HEADER"
echo ""
echo -e "${GREEN}Done!${NC}"
