#!/bin/bash

echo "Testing Coinbase with API Key Authentication"
echo "============================================"
echo ""
echo "1. Go to: https://www.coinbase.com/settings/api"
echo "2. Create a New API Key (not OAuth app)"
echo "3. Set permissions: wallet:accounts:read"
echo "4. Save the API Key and Secret"
echo ""
echo "Then test with:"
echo 'curl -H "CB-ACCESS-KEY: YOUR_API_KEY" \'
echo '     -H "CB-ACCESS-SIGN: signature" \'
echo '     -H "CB-ACCESS-TIMESTAMP: timestamp" \'
echo '     https://api.coinbase.com/v2/accounts'
