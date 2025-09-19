#!/bin/bash

# Script to toggle demo mode for a user

USER_ID="fff74730-4a58-45fe-be74-cf38a57dcb0b"
API_URL="https://localhost:8443"

# Get current session token (you'll need to be logged in)
echo "Make sure you're logged in to the application first"
echo ""

# Toggle demo mode to OFF (false)
echo "Disabling demo mode for user: $USER_ID"

curl -k -X PUT "$API_URL/v1/profile/demo-mode" \
  -H "Content-Type: application/json" \
  -H "Cookie: $1" \
  -d '{
    "demoMode": false
  }'

echo ""
echo "Demo mode disabled. Your portfolio should now show real Kraken data."
echo "Refresh the portfolio page to see the changes."