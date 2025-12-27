#!/bin/bash
#
# Grafana Cloud Metrics Verification Script
# Tests connectivity and queries metrics from Grafana Cloud Prometheus
#

set -e

echo "🔍 Grafana Cloud Metrics Verification"
echo "======================================"
echo

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Check if secrets exist
echo "1. Checking GCP secrets..."
if gcloud secrets describe GRAFANA_OTLP_AUTH &>/dev/null; then
    echo -e "${GREEN}✓${NC} GRAFANA_OTLP_AUTH secret exists"
else
    echo -e "${RED}✗${NC} GRAFANA_OTLP_AUTH secret not found"
    exit 1
fi

if gcloud secrets describe GRAFANA_PROMETHEUS_AUTH &>/dev/null; then
    echo -e "${GREEN}✓${NC} GRAFANA_PROMETHEUS_AUTH secret exists"
else
    echo -e "${RED}✗${NC} GRAFANA_PROMETHEUS_AUTH secret not found"
    exit 1
fi
echo

# Step 2: Get the Prometheus auth token
echo "2. Retrieving Prometheus auth token..."
PROM_AUTH=$(gcloud secrets versions access latest --secret=GRAFANA_PROMETHEUS_AUTH 2>/dev/null)
if [ -z "$PROM_AUTH" ]; then
    echo -e "${RED}✗${NC} Failed to retrieve Prometheus auth token"
    exit 1
fi
echo -e "${GREEN}✓${NC} Auth token retrieved (length: ${#PROM_AUTH})"
echo

# Step 3: Test Prometheus API connectivity
echo "3. Testing Prometheus API connectivity..."
PROM_URLS=(
    "https://prometheus-prod-01-eu-west-0.grafana.net/api/prom"
    "https://prometheus-prod-us-east-0.grafana.net/api/prom"
)

for PROM_URL in "${PROM_URLS[@]}"; do
    echo "   Testing: $PROM_URL"
    HTTP_CODE=$(curl -s -w "%{http_code}" -o /tmp/prom_response.json -H "Authorization: Basic $PROM_AUTH" \
        "${PROM_URL}/api/v1/query?query=up" 2>/dev/null || echo "000")

    BODY=$(cat /tmp/prom_response.json 2>/dev/null || echo "{}")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "   ${GREEN}✓${NC} Connected successfully (HTTP $HTTP_CODE)"
        echo "   Active endpoint: $PROM_URL"
        ACTIVE_PROM_URL="$PROM_URL"
        break
    elif [ "$HTTP_CODE" = "401" ]; then
        echo -e "   ${RED}✗${NC} Authentication failed (HTTP $HTTP_CODE)"
    elif [ "$HTTP_CODE" = "000" ]; then
        echo -e "   ${YELLOW}⚠${NC} Connection failed (timeout or network error)"
    else
        echo -e "   ${YELLOW}⚠${NC} Unexpected response (HTTP $HTTP_CODE)"
    fi
done
echo

# Step 4: Query for OpenTelemetry metrics
if [ -n "$ACTIVE_PROM_URL" ]; then
    echo "4. Querying for OpenTelemetry metrics..."

    # Query for HTTP server request metrics
    echo "   Querying: http_server_requests_seconds_count"
    METRICS=$(curl -s -H "Authorization: Basic $PROM_AUTH" \
        "${ACTIVE_PROM_URL}/api/v1/query?query=http_server_requests_seconds_count" \
        | jq -r '.data.result | length' 2>/dev/null || echo "0")

    if [ "$METRICS" != "0" ] && [ "$METRICS" != "null" ]; then
        echo -e "   ${GREEN}✓${NC} Found $METRICS metric series"
    else
        echo -e "   ${YELLOW}⚠${NC} No metrics found (metrics may not be flowing yet)"
    fi
    echo

    # Query for JVM metrics
    echo "   Querying: jvm_memory_used_bytes"
    METRICS=$(curl -s -H "Authorization: Basic $PROM_AUTH" \
        "${ACTIVE_PROM_URL}/api/v1/query?query=jvm_memory_used_bytes" \
        | jq -r '.data.result | length' 2>/dev/null || echo "0")

    if [ "$METRICS" != "0" ] && [ "$METRICS" != "null" ]; then
        echo -e "   ${GREEN}✓${NC} Found $METRICS JVM metric series"
    else
        echo -e "   ${YELLOW}⚠${NC} No JVM metrics found"
    fi
    echo

    # List all available metrics
    echo "   Listing available metrics (first 20)..."
    curl -s -H "Authorization: Basic $PROM_AUTH" \
        "${ACTIVE_PROM_URL}/api/v1/label/__name__/values" \
        | jq -r '.data[] | select(. | test("http_|jvm_|strategiz_"))' 2>/dev/null | head -20 || echo "   Unable to list metrics"
    echo
fi

# Step 5: Check Cloud Run environment variables
echo "5. Checking Cloud Run configuration..."
GRAFANA_ENDPOINT=$(gcloud run services describe strategiz-api --region=us-east1 \
    --format="value(spec.template.spec.containers[0].env)" 2>/dev/null \
    | grep -o "GRAFANA_OTLP_ENDPOINT[^}]*" | head -1)

if [ -n "$GRAFANA_ENDPOINT" ]; then
    echo -e "${GREEN}✓${NC} OTLP endpoint configured: $GRAFANA_ENDPOINT"
else
    echo -e "${YELLOW}⚠${NC} GRAFANA_OTLP_ENDPOINT not found in environment"
fi
echo

# Summary
echo "======================================"
echo "Summary:"
if [ -n "$ACTIVE_PROM_URL" ]; then
    echo -e "${GREEN}✓${NC} Grafana Cloud Prometheus is accessible"
    echo "  URL: $ACTIVE_PROM_URL"
    echo
    echo "To query metrics from the observability dashboard:"
    echo "  1. Navigate to https://console.strategiz.io/observability"
    echo "  2. Log in with admin credentials"
    echo "  3. Metrics will be fetched from Grafana Cloud"
else
    echo -e "${RED}✗${NC} Unable to connect to Grafana Cloud Prometheus"
    echo
    echo "Troubleshooting steps:"
    echo "  1. Verify the Prometheus URL matches your Grafana Cloud stack region"
    echo "  2. Check that GRAFANA_PROMETHEUS_AUTH secret contains valid credentials"
    echo "  3. Ensure the token has prometheus:read permissions"
fi
