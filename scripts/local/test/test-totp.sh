#!/bin/bash

# Test TOTP Authentication Flow

API_URL="http://localhost:8080"
USER_ID="test-user-123"
DEVICE_ID="test-device-456"

echo "===== Testing TOTP Authentication Flow ====="

# Step 1: Register TOTP
echo -e "\n1. Registering TOTP for user..."
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/auth/totp/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"deviceId\": \"$DEVICE_ID\"
  }")

echo "Register Response: $REGISTER_RESPONSE"

# Extract QR code URL and secret
SECRET=$(echo "$REGISTER_RESPONSE" | grep -o '"secret":"[^"]*' | cut -d'"' -f4)
QR_CODE_URL=$(echo "$REGISTER_RESPONSE" | grep -o '"qrCodeUri":"[^"]*' | cut -d'"' -f4)

if [ -z "$SECRET" ]; then
  echo "Failed to register TOTP. Response: $REGISTER_RESPONSE"
  exit 1
fi

echo "Secret: $SECRET"
echo "QR Code URL: $QR_CODE_URL"

# Step 2: Complete registration with a test code
echo -e "\n2. Completing TOTP registration..."
# In a real scenario, you'd generate this from an authenticator app
# For testing, we'll use a placeholder
TEST_CODE="123456"

COMPLETE_RESPONSE=$(curl -s -X POST "$API_URL/auth/totp/register/complete" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"deviceId\": \"$DEVICE_ID\",
    \"totpCode\": \"$TEST_CODE\"
  }")

echo "Complete Response: $COMPLETE_RESPONSE"

# Step 3: Authenticate with TOTP
echo -e "\n3. Authenticating with TOTP..."
AUTH_RESPONSE=$(curl -s -X POST "$API_URL/auth/totp/authenticate" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"totpCode\": \"$TEST_CODE\",
    \"deviceId\": \"$DEVICE_ID\"
  }")

echo "Auth Response: $AUTH_RESPONSE"

# Extract tokens if successful
ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
REFRESH_TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ]; then
  echo -e "\n✅ TOTP Authentication Successful!"
  echo "Access Token: ${ACCESS_TOKEN:0:50}..."
  echo "Refresh Token: ${REFRESH_TOKEN:0:50}..."
else
  echo -e "\n❌ TOTP Authentication Failed"
  echo "Response: $AUTH_RESPONSE"
fi

echo -e "\n===== TOTP Test Complete ====="