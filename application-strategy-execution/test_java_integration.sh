#!/bin/bash
# Test the full Java → Python gRPC integration

echo "Testing Java Backend → Python gRPC Service Integration"
echo "======================================================="

# Sample Python strategy code
STRATEGY_CODE='
SYMBOL = "AAPL"
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05
POSITION_SIZE = 100

def calculate_rsi(data, period=14):
    """Calculate RSI indicator"""
    delta = data["close"].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss
    rsi = 100 - (100 / (1 + rs))
    return rsi

def strategy(data):
    """Main strategy function"""
    # Calculate RSI
    data["rsi"] = calculate_rsi(data)

    # Get latest RSI value
    current_rsi = data["rsi"].iloc[-1]

    # Generate signal
    if current_rsi < 30:
        return "BUY"  # Oversold
    elif current_rsi > 70:
        return "SELL"  # Overbought
    else:
        return "HOLD"
'

# Make the API call
echo "Calling POST https://localhost:8443/v1/strategies/execute-code"
echo ""

curl -X POST "https://localhost:8443/v1/strategies/execute-code" \
  -H "Content-Type: application/json" \
  -k \
  --data @- <<EOF
{
  "code": $(echo "$STRATEGY_CODE" | jq -Rs .),
  "language": "python",
  "symbol": "AAPL"
}
EOF

echo ""
echo ""
echo "Check Python server logs for execution messages..."
