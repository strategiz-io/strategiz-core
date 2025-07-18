#!/bin/bash

echo "Testing Market Ticker API endpoints..."
echo "======================================"

# Test CoinGecko API directly
echo -e "\n1. Testing CoinGecko API (Bitcoin price):"
curl -s "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd&include_24hr_change=true" | jq '.'

# Test Alpha Vantage with demo key
echo -e "\n2. Testing Alpha Vantage API (AAPL with demo key):"
curl -s "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=AAPL&apikey=demo" | jq '.'

# Test local endpoint (if server is running)
echo -e "\n3. Testing local market ticker endpoint (if server is running on port 8080):"
curl -s "http://localhost:8080/v1/market/tickers" | jq '.' 2>/dev/null || echo "Server not running on port 8080"

echo -e "\nNote: To see real data on the landing page:"
echo "1. Start the backend server: cd /path/to/strategiz-core && mvn spring-boot:run -pl application"
echo "2. The endpoint will be available at: http://localhost:8080/v1/market/tickers"
echo "3. CoinGecko data should work immediately (no API key required)"
echo "4. For full Alpha Vantage data, get a free API key from https://www.alphavantage.co/support/#api-key"