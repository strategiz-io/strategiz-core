#!/bin/bash

# Test Autonomous Mode in Production
# Usage: ./scripts/test-autonomous-mode.sh <JWT_TOKEN> [SYMBOL]
#
# Example:
#   ./scripts/test-autonomous-mode.sh "eyJhbGciOiJIUz..." AAPL
#   ./scripts/test-autonomous-mode.sh "eyJhbGciOiJIUz..."        # defaults to AAPL

set -e

API_URL="${STRATEGIZ_API_URL:-https://strategiz-api-bflhiwsnmq-ue.a.run.app}"
TOKEN="$1"
SYMBOL="${2:-AAPL}"

if [ -z "$TOKEN" ]; then
    echo "Usage: $0 <JWT_TOKEN> [SYMBOL]"
    echo ""
    echo "To get a token:"
    echo "  1. Login to the UI at https://strategiz.io"
    echo "  2. Open browser DevTools > Application > Cookies"
    echo "  3. Copy the 'access_token' cookie value"
    echo ""
    echo "Or set STRATEGIZ_PROD_TOKEN environment variable"
    exit 1
fi

echo "=== Testing Autonomous Mode for $SYMBOL ==="
echo "API URL: $API_URL"
echo ""

# Make the request
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/v1/labs/ai/generate-strategy" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{
        \"prompt\": \"$SYMBOL\",
        \"autonomousMode\": \"AUTONOMOUS\",
        \"model\": \"gemini-2.5-flash\",
        \"useHistoricalInsights\": true,
        \"historicalInsightsOptions\": {
            \"lookbackDays\": 750,
            \"fastMode\": true
        }
    }")

# Extract HTTP status code and body
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    SUCCESS=$(echo "$BODY" | jq -r '.success // false')
    if [ "$SUCCESS" = "true" ]; then
        echo "SUCCESS!"
        echo ""

        # Extract key metrics from explanation
        EXPLANATION=$(echo "$BODY" | jq -r '.explanation // "N/A"')
        echo "=== Strategy Explanation ==="
        echo "$EXPLANATION" | head -30
        echo ""

        # Check Python code
        CODE_LENGTH=$(echo "$BODY" | jq -r '.pythonCode | length')
        echo "Python code length: $CODE_LENGTH characters"

        # Extract strategy type from code if present
        echo ""
        echo "=== Strategy Type Detection ==="
        echo "$BODY" | jq -r '.pythonCode' | grep -E "^# Strategy|RSI_PERIOD|MACD|BOLLINGER|MA_CROSSOVER" | head -5
    else
        echo "Generation failed!"
        echo "$BODY" | jq '.'
    fi
elif [ "$HTTP_CODE" = "401" ]; then
    echo "Authentication failed - token may be expired"
    echo "$BODY" | jq '.'
else
    echo "Request failed with status $HTTP_CODE"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
