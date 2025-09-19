#!/bin/bash

# Simple script to turn off demo mode
# You need to provide your session cookie

echo "Turn off demo mode for your account"
echo "Please provide your SESSION cookie value from the browser:"
echo "(Chrome DevTools -> Application -> Cookies -> SESSION)"
read -p "Session cookie: " SESSION_COOKIE

curl -k -X PUT "https://localhost:8443/v1/profile/demo-mode" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=$SESSION_COOKIE" \
  -d '{"demoMode": false}'

echo ""
echo "Demo mode should now be disabled. Refresh your portfolio page."