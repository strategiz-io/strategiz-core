#!/bin/bash
# SMS OTP Production API Integration Tests
#
# Run with: ./scripts/test-sms-otp-production.sh
# Or with auth: STRATEGIZ_PROD_TOKEN=xxx ./scripts/test-sms-otp-production.sh

API_URL="${STRATEGIZ_API_URL:-https://strategiz-api-bflhiwsnmq-ue.a.run.app}"
AUTH_HEADER=""

if [ -n "$STRATEGIZ_PROD_TOKEN" ]; then
    AUTH_HEADER="-H \"Authorization: Bearer $STRATEGIZ_PROD_TOKEN\""
fi

echo "========================================"
echo "SMS OTP Production API Integration Tests"
echo "========================================"
echo "API URL: $API_URL"
echo ""

# Test 1: Send SMS OTP (should fail - phone not registered)
echo "Test 1: Send SMS OTP to unregistered phone"
echo "Expected: Error (phone not registered or rate limited)"
curl -s -X POST "$API_URL/v1/auth/sms-otp/authentications/send" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+15551234567", "countryCode": "US"}' | jq .
echo ""

# Test 2: Send SMS OTP with invalid phone format
echo "Test 2: Send SMS OTP with invalid phone format"
echo "Expected: 400 Bad Request"
curl -s -X POST "$API_URL/v1/auth/sms-otp/authentications/send" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "invalid", "countryCode": "US"}' | jq .
echo ""

# Test 3: Verify SMS OTP with wrong code
echo "Test 3: Verify SMS OTP with wrong code"
echo "Expected: Error (invalid or expired code)"
curl -s -X POST "$API_URL/v1/auth/sms-otp/authentications/verify" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+15551234567", "otpCode": "000000"}' | jq .
echo ""

# Test 4: Firebase token verify with invalid token
echo "Test 4: Firebase verify with invalid token"
echo "Expected: Error (invalid Firebase token)"
curl -s -X POST "$API_URL/v1/auth/sms-otp/firebase/verify" \
  -H "Content-Type: application/json" \
  -d '{"firebaseIdToken": "invalid-token", "phoneNumber": "+15551234567", "isRegistration": false}' | jq .
echo ""

# Test 5: Firebase token verify with missing token
echo "Test 5: Firebase verify with missing token"
echo "Expected: 400 Bad Request"
curl -s -X POST "$API_URL/v1/auth/sms-otp/firebase/verify" \
  -H "Content-Type: application/json" \
  -d '{"firebaseIdToken": "", "phoneNumber": "+15551234567", "isRegistration": false}' | jq .
echo ""

# Test 6: Get registration status (requires auth)
if [ -n "$STRATEGIZ_PROD_TOKEN" ]; then
    echo "Test 6: Get SMS OTP registration status"
    echo "Expected: 200 OK with enabled=false for new user"
    curl -s -X GET "$API_URL/v1/auth/sms-otp/registrations/test-user-$(date +%s)" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $STRATEGIZ_PROD_TOKEN" | jq .
    echo ""
else
    echo "Test 6: Skipped (requires STRATEGIZ_PROD_TOKEN)"
    echo ""
fi

# Test 7: Register phone without userId (should fail)
echo "Test 7: Register phone without userId"
echo "Expected: 400 Bad Request"
curl -s -X POST "$API_URL/v1/auth/sms-otp/registrations" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+15551234567", "countryCode": "US"}' | jq .
echo ""

# Test 8: Verify OTP with invalid session ID
echo "Test 8: Verify OTP with invalid session ID"
echo "Expected: Error (session not found)"
curl -s -X PUT "$API_URL/v1/auth/sms-otp/authentications/invalid-session-id" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+15551234567", "otpCode": "123456"}' | jq .
echo ""

echo "========================================"
echo "Tests completed!"
echo "========================================"
