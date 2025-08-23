#!/bin/bash

# Quick setup for when Coinbase approves ngrok URL

echo "Quick ngrok Setup for Coinbase OAuth"
echo "====================================="
echo ""

# Start ngrok
echo "Starting ngrok tunnel..."
ngrok http https://localhost:8443 &
NGROK_PID=$!

# Wait for ngrok to start
sleep 3

# Get the URL
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | python3 -c "import sys, json; data = json.load(sys.stdin); print(data['tunnels'][0]['public_url'] if data['tunnels'] else '')")

if [ -z "$NGROK_URL" ]; then
    echo "Failed to get ngrok URL"
    exit 1
fi

echo ""
echo "✅ ngrok URL: $NGROK_URL"
echo ""
echo "📝 Add this to Coinbase OAuth app:"
echo "   $NGROK_URL/v1/providers/callback/coinbase"
echo ""
echo "Then restart backend with:"
echo "   ./start-backend-with-ngrok.sh $NGROK_URL"
