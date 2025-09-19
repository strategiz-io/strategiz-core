#!/bin/bash

# Simple Portfolio Endpoint Test
# This script tests the portfolio endpoints with a manual token

API_URL="https://localhost:8443"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Portfolio Endpoint Test"
echo "======================="
echo ""

# Check if token is provided as argument
if [ -z "$1" ]; then
    echo -e "${YELLOW}Usage: $0 <bearer-token>${NC}"
    echo ""
    echo "To get a token, you can:"
    echo "1. Log into the frontend application and copy the token from browser DevTools"
    echo "2. Use the authentication endpoints with a test user"
    echo "3. Check the application logs for any test tokens"
    echo ""
    echo "Example:"
    echo "  $0 'v4.local.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJleHAiOiIyMDI0...'"
    echo ""
    exit 1
fi

TOKEN="$1"

echo -e "${GREEN}Using provided token${NC}"
echo "Token: ${TOKEN:0:30}..."
echo ""

# Test 1: Portfolio Summary
echo -e "${GREEN}1. Testing Portfolio Summary${NC}"
echo "   GET $API_URL/v1/portfolio/summary"
echo ""

RESPONSE=$(curl -k -s -X GET "$API_URL/v1/portfolio/summary" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json")

if echo "$RESPONSE" | grep -q "totalValue"; then
    echo -e "${GREEN}✓ Success!${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
fi

echo ""
echo "---"
echo ""

# Test 2: Portfolio Overview
echo -e "${GREEN}2. Testing Portfolio Overview${NC}"
echo "   GET $API_URL/v1/portfolio/overview"
echo ""

RESPONSE=$(curl -k -s -X GET "$API_URL/v1/portfolio/overview" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json")

if echo "$RESPONSE" | grep -q "providers"; then
    echo -e "${GREEN}✓ Success!${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
fi

echo ""
echo "---"
echo ""

# Test 3: Kraken Provider Data
echo -e "${GREEN}3. Testing Kraken Provider Portfolio${NC}"
echo "   GET $API_URL/v1/portfolio/providers/kraken"
echo ""

RESPONSE=$(curl -k -s -X GET "$API_URL/v1/portfolio/providers/kraken" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json")

if echo "$RESPONSE" | grep -q "kraken"; then
    echo -e "${GREEN}✓ Success!${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
fi

echo ""
echo "---"
echo ""

# Test 4: Refresh Portfolio
echo -e "${GREEN}4. Testing Portfolio Refresh${NC}"
echo "   POST $API_URL/v1/portfolio/refresh"
echo ""

RESPONSE=$(curl -k -s -X POST "$API_URL/v1/portfolio/refresh" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json")

if echo "$RESPONSE" | grep -q "success"; then
    echo -e "${GREEN}✓ Success!${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
fi

echo ""
echo -e "${GREEN}Portfolio tests completed!${NC}"