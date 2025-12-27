#!/bin/bash
# Test script for observability metrics API endpoints

set -e

API_URL="${API_URL:-https://strategiz-api-bflhiwsnmq-ue.a.run.app}"
TOKEN="${AUTH_TOKEN:-}"

echo "🧪 Testing Observability Metrics API"
echo "======================================"
echo "API URL: $API_URL"
echo ""

# Function to make authenticated request
api_get() {
    local endpoint=$1
    echo "Testing: GET $endpoint"
    if [ -z "$TOKEN" ]; then
        curl -s -X GET "$API_URL$endpoint" | jq '.' || echo "❌ Failed (may need authentication)"
    else
        curl -s -X GET "$API_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN" | jq '.' || echo "❌ Failed"
    fi
    echo ""
}

echo "1. Testing Execution Health Metrics"
echo "-----------------------------------"
api_get "/v1/console/observability/execution/health"

echo "2. Testing Execution Latency Metrics (15min)"
echo "--------------------------------------------"
api_get "/v1/console/observability/execution/latency?durationMinutes=15"

echo "3. Testing Execution Throughput Metrics"
echo "---------------------------------------"
api_get "/v1/console/observability/execution/throughput?durationMinutes=15"

echo "4. Testing Cache Performance Metrics"
echo "------------------------------------"
api_get "/v1/console/observability/execution/cache?durationMinutes=15"

echo "5. Testing Execution Error Metrics"
echo "----------------------------------"
api_get "/v1/console/observability/execution/errors?durationMinutes=15"

echo ""
echo "✅ Test complete!"
echo ""
echo "Note: If you see authentication errors, set AUTH_TOKEN environment variable:"
echo "export AUTH_TOKEN=your_jwt_token"
