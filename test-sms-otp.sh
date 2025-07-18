#!/bin/bash

# Test SMS OTP Authentication Flow

API_URL="http://localhost:8080"
USER_ID="test-user-123"
PHONE_NUMBER="+1234567890"
DEVICE_ID="test-device-456"

echo "===== Testing SMS OTP Authentication Flow ====="

# Step 1: Register SMS OTP
echo -e "\n1. Registering SMS OTP for user..."
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/auth/sms-otp/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"phoneNumber\": \"$PHONE_NUMBER\"
  }")

echo "Register Response: $REGISTER_RESPONSE"

# Extract session ID
SESSION_ID=$(echo "$REGISTER_RESPONSE" | grep -o '"sessionId":"[^"]*' | cut -d'"' -f4)

if [ -z "$SESSION_ID" ]; then
  echo "Failed to register SMS OTP. Response: $REGISTER_RESPONSE"
  exit 1
fi

echo "Session ID: $SESSION_ID"

# Step 2: Verify SMS OTP (in real scenario, code would be sent via SMS)
echo -e "\n2. Verifying SMS OTP..."
# For testing, we'll use a test code
TEST_CODE="123456"

VERIFY_RESPONSE=$(curl -s -X POST "$API_URL/auth/sms-otp/register/verify" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"code\": \"$TEST_CODE\",
    \"deviceId\": \"$DEVICE_ID\"
  }")

echo "Verify Response: $VERIFY_RESPONSE"

# Step 3: Send OTP for authentication
echo -e "\n3. Sending OTP for authentication..."
SEND_RESPONSE=$(curl -s -X POST "$API_URL/auth/sms-otp/send" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\"
  }")

echo "Send Response: $SEND_RESPONSE"

# Extract new session ID for authentication
AUTH_SESSION_ID=$(echo "$SEND_RESPONSE" | grep -o '"sessionId":"[^"]*' | cut -d'"' -f4)

if [ -z "$AUTH_SESSION_ID" ]; then
  echo "Failed to send SMS OTP. Response: $SEND_RESPONSE"
  exit 1
fi

echo "Auth Session ID: $AUTH_SESSION_ID"

# Step 4: Authenticate with SMS OTP
echo -e "\n4. Authenticating with SMS OTP..."
AUTH_RESPONSE=$(curl -s -X POST "$API_URL/auth/sms-otp/authenticate" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$AUTH_SESSION_ID\",
    \"code\": \"$TEST_CODE\",
    \"deviceId\": \"$DEVICE_ID\"
  }")

echo "Auth Response: $AUTH_RESPONSE"

# Extract tokens if successful
ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
REFRESH_TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ]; then
  echo -e "\n✅ SMS OTP Authentication Successful!"
  echo "Access Token: ${ACCESS_TOKEN:0:50}..."
  echo "Refresh Token: ${REFRESH_TOKEN:0:50}..."
else
  echo -e "\n❌ SMS OTP Authentication Failed"
  echo "Response: $AUTH_RESPONSE"
fi

echo -e "\n===== SMS OTP Test Complete ====="