#!/bin/bash
# Integration Test Runner for Strategiz AI Strategy Generation
# This script tests GENERATIVE_AI and AUTONOMOUS modes against production

set -e

# Configuration
API_URL="${STRATEGIZ_API_URL:-https://api.strategiz.io}"
SYMBOLS="${STRATEGIZ_TEST_SYMBOLS:-AAPL,MSFT,GOOGL,BTC,ETH}"
TEST_MODE="${1:-all}"  # all, generative-ai, autonomous

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Strategiz Integration Test Runner${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "API URL: $API_URL"
echo "Test Mode: $TEST_MODE"
echo "Symbols: $SYMBOLS"
echo ""

# Check for required environment variables
if [ -z "$STRATEGIZ_CLIENT_ID" ] || [ -z "$STRATEGIZ_CLIENT_SECRET" ]; then
    echo -e "${RED}ERROR: STRATEGIZ_CLIENT_ID and STRATEGIZ_CLIENT_SECRET must be set${NC}"
    echo ""
    echo "Export these environment variables before running:"
    echo "  export STRATEGIZ_CLIENT_ID=your_client_id"
    echo "  export STRATEGIZ_CLIENT_SECRET=your_client_secret"
    exit 1
fi

# Step 1: Get access token
echo -e "${YELLOW}Authenticating...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "$API_URL/v1/auth/service-account/token" \
    -H "Content-Type: application/json" \
    -d '{
        "client_id": "'"$STRATEGIZ_CLIENT_ID"'",
        "client_secret": "'"$STRATEGIZ_CLIENT_SECRET"'",
        "grant_type": "client_credentials"
    }')

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}Authentication failed!${NC}"
    echo "$TOKEN_RESPONSE"
    exit 1
fi
echo -e "${GREEN}Authentication successful${NC}"
echo ""

# Results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to test GENERATIVE_AI mode
test_generative_ai() {
    local SYMBOL=$1
    echo -e "${BLUE}Testing GENERATIVE_AI for $SYMBOL...${NC}"

    # Generate strategy
    GEN_RESPONSE=$(curl -s --max-time 300 -X POST "$API_URL/v1/labs/ai/generate-strategy" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d '{
            "prompt": "Generate a trading strategy for '"$SYMBOL"'",
            "autonomousMode": "GENERATIVE_AI",
            "useHistoricalInsights": true,
            "context": {
                "symbols": ["'"$SYMBOL"'"],
                "timeframe": "1D"
            },
            "historicalInsightsOptions": {
                "lookbackDays": 750,
                "fastMode": true
            }
        }')

    # Save response for debugging
    echo "$GEN_RESPONSE" > "/tmp/gen_result_${SYMBOL}.json"

    SUCCESS=$(echo "$GEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',''))" 2>/dev/null)
    MODE_USED=$(echo "$GEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('autonomousModeUsed',''))" 2>/dev/null)

    if [ "$SUCCESS" != "True" ] && [ "$SUCCESS" != "true" ]; then
        echo -e "${RED}  Strategy generation failed!${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        return 1
    fi

    if [ "$MODE_USED" != "GENERATIVE_AI" ]; then
        echo -e "${RED}  Wrong mode used: $MODE_USED (expected GENERATIVE_AI)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        return 1
    fi

    # Extract Python code and execute
    PYTHON_CODE=$(echo "$GEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('pythonCode',''))" 2>/dev/null)
    CODE_LENGTH=${#PYTHON_CODE}
    echo "  Strategy generated ($CODE_LENGTH chars)"

    # Execute strategy
    EXEC_RESPONSE=$(curl -s --max-time 120 -X POST "$API_URL/v1/strategies/execute-code" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d '{
            "code": '"$(echo "$PYTHON_CODE" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read()))')"',
            "language": "python",
            "symbol": "'"$SYMBOL"'",
            "timeframe": "1D",
            "period": "3y"
        }')

    # Save execution response
    echo "$EXEC_RESPONSE" > "/tmp/exec_result_${SYMBOL}.json"

    # Extract metrics
    STRATEGY_RETURN=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('totalReturn',0))" 2>/dev/null)
    BUY_HOLD_RETURN=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('buyAndHoldReturn',0))" 2>/dev/null)
    OUTPERFORMANCE=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('outperformance',0))" 2>/dev/null)
    TOTAL_TRADES=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('totalTrades',0))" 2>/dev/null)

    echo "  Strategy Return: ${STRATEGY_RETURN}%"
    echo "  Buy & Hold Return: ${BUY_HOLD_RETURN}%"
    echo "  Outperformance: ${OUTPERFORMANCE}%"
    echo "  Total Trades: $TOTAL_TRADES"

    # Check if beats buy-and-hold
    BEATS_BH=$(echo "$OUTPERFORMANCE >= 0" | bc -l 2>/dev/null || echo "0")

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$BEATS_BH" == "1" ]; then
        echo -e "${GREEN}  PASS: $SYMBOL GENERATIVE_AI beats buy-and-hold${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}  FAIL: $SYMBOL GENERATIVE_AI underperformed${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Function to test AUTONOMOUS mode
test_autonomous() {
    local SYMBOL=$1
    echo -e "${BLUE}Testing AUTONOMOUS for $SYMBOL...${NC}"

    # Generate optimized strategy
    GEN_RESPONSE=$(curl -s --max-time 300 -X POST "$API_URL/v1/labs/ai/generate-strategy" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d '{
            "prompt": "Optimize trading strategy for '"$SYMBOL"'",
            "autonomousMode": "AUTONOMOUS",
            "useHistoricalInsights": true,
            "context": {
                "symbols": ["'"$SYMBOL"'"],
                "timeframe": "1D"
            },
            "historicalInsightsOptions": {
                "lookbackDays": 750,
                "fastMode": true
            }
        }')

    # Save response
    echo "$GEN_RESPONSE" > "/tmp/auto_gen_result_${SYMBOL}.json"

    SUCCESS=$(echo "$GEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',''))" 2>/dev/null)
    MODE_USED=$(echo "$GEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('autonomousModeUsed',''))" 2>/dev/null)

    if [ "$SUCCESS" != "True" ] && [ "$SUCCESS" != "true" ]; then
        echo -e "${RED}  Strategy optimization failed!${NC}"
        echo "$GEN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('error','Unknown error'))" 2>/dev/null
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        return 1
    fi

    if [ "$MODE_USED" != "AUTONOMOUS" ]; then
        echo -e "${RED}  Wrong mode used: $MODE_USED (expected AUTONOMOUS)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        return 1
    fi

    # Extract Python code and execute
    PYTHON_CODE=$(echo "$GEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('pythonCode',''))" 2>/dev/null)
    CODE_LENGTH=${#PYTHON_CODE}
    echo "  Optimized strategy generated ($CODE_LENGTH chars)"

    # Execute strategy
    EXEC_RESPONSE=$(curl -s --max-time 120 -X POST "$API_URL/v1/strategies/execute-code" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d '{
            "code": '"$(echo "$PYTHON_CODE" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read()))')"',
            "language": "python",
            "symbol": "'"$SYMBOL"'",
            "timeframe": "1D",
            "period": "3y"
        }')

    # Save execution response
    echo "$EXEC_RESPONSE" > "/tmp/auto_exec_result_${SYMBOL}.json"

    # Extract metrics
    STRATEGY_RETURN=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('totalReturn',0))" 2>/dev/null)
    BUY_HOLD_RETURN=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('buyAndHoldReturn',0))" 2>/dev/null)
    OUTPERFORMANCE=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('outperformance',0))" 2>/dev/null)
    TOTAL_TRADES=$(echo "$EXEC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('performance',{}).get('totalTrades',0))" 2>/dev/null)

    echo "  Strategy Return: ${STRATEGY_RETURN}%"
    echo "  Buy & Hold Return: ${BUY_HOLD_RETURN}%"
    echo "  Outperformance: ${OUTPERFORMANCE}%"
    echo "  Total Trades: $TOTAL_TRADES"

    # Check if beats buy-and-hold
    BEATS_BH=$(echo "$OUTPERFORMANCE >= 0" | bc -l 2>/dev/null || echo "0")

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$BEATS_BH" == "1" ]; then
        echo -e "${GREEN}  PASS: $SYMBOL AUTONOMOUS beats buy-and-hold${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}  FAIL: $SYMBOL AUTONOMOUS underperformed${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Parse symbols into array
IFS=',' read -ra SYMBOL_ARRAY <<< "$SYMBOLS"

# Run tests based on mode
echo -e "${YELLOW}Starting tests...${NC}"
echo ""

if [ "$TEST_MODE" == "all" ] || [ "$TEST_MODE" == "generative-ai" ]; then
    echo -e "${BLUE}========== GENERATIVE_AI MODE TESTS ==========${NC}"
    for SYMBOL in "${SYMBOL_ARRAY[@]}"; do
        test_generative_ai "$SYMBOL" || true
        echo ""
    done
fi

if [ "$TEST_MODE" == "all" ] || [ "$TEST_MODE" == "autonomous" ]; then
    echo -e "${BLUE}========== AUTONOMOUS MODE TESTS ==========${NC}"
    for SYMBOL in "${SYMBOL_ARRAY[@]}"; do
        test_autonomous "$SYMBOL" || true
        echo ""
    done
fi

# Summary
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}               TEST SUMMARY${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    echo "Check /tmp/*_result_*.json for detailed responses"
    exit 1
fi
