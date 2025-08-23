#!/usr/bin/env python3
"""
Test SMS OTP Registration and Authentication
"""

import requests
import json
import uuid
import time
import sys

BASE_URL = "http://localhost:8080/v1"

def print_response(response, description):
    """Pretty print response with description"""
    print(f"\n{description}:")
    print(f"   Status: {response.status_code}")
    if response.status_code >= 200 and response.status_code < 300:
        try:
            data = response.json()
            print(f"   Response: {json.dumps(data, indent=2)}")
            return data
        except:
            print(f"   Response: {response.text}")
            return None
    else:
        print(f"   Error: {response.text}")
        return None

def test_sms_otp_registration():
    """Test SMS OTP registration flow for signup"""
    print("\n" + "="*60)
    print("🔧 Testing SMS OTP Registration Flow...")
    print("="*60)
    
    # Step 1: Create a test user first
    print("\n1. Creating test user...")
    user_id = str(uuid.uuid4())
    email = f"test-{user_id[:8]}@example.com"
    
    signup_response = requests.post(
        f"{BASE_URL}/signup/profile",
        json={
            "name": f"Test User {user_id[:8]}",
            "email": email,
            "photoURL": "",
            "temporaryToken": "temp-" + user_id
        }
    )
    
    signup_data = print_response(signup_response, "User creation")
    if not signup_data:
        print("❌ Failed to create user")
        return False
    
    user_id = signup_data.get('userId')
    session_token = signup_data.get('sessionToken')
    print(f"✅ User created: {user_id}")
    
    # Step 2: Register phone number
    print("\n2. Registering phone number...")
    phone_number = f"+1555{str(uuid.uuid4().int)[:7]}"  # Generate unique phone
    
    register_response = requests.post(
        f"{BASE_URL}/auth/sms-otp/registrations",
        json={
            "userId": user_id,
            "phoneNumber": phone_number,
            "countryCode": "US"
        }
    )
    
    register_data = print_response(register_response, "Phone registration")
    if not register_data or not register_data.get('success'):
        print("❌ Failed to register phone number")
        return False
    
    registration_id = register_data.get('registrationId')
    print(f"✅ Phone registration started for: {phone_number}")
    print(f"   Registration ID: {registration_id}")
    
    # Step 3: In dev mode, OTP should be logged. For testing, we'll use a fixed OTP
    print("\n3. Verifying phone number with OTP...")
    print("   NOTE: In dev mode, check logs for actual OTP code")
    print("   Using mock OTP: 123456")
    
    verify_response = requests.put(
        f"{BASE_URL}/auth/sms-otp/registrations/{registration_id}",
        json={
            "userId": user_id,
            "sessionToken": session_token,
            "phoneNumber": phone_number,
            "otpCode": "123456"  # Mock OTP for testing
        }
    )
    
    verify_data = print_response(verify_response, "Phone verification")
    if verify_data and verify_data.get('success'):
        print(f"✅ Phone number verified successfully!")
        if verify_data.get('identityToken'):
            print(f"   Updated tokens received")
        return True
    else:
        print("❌ Phone verification failed")
        print("   Check server logs for the actual OTP code")
        return False

def test_sms_otp_authentication():
    """Test SMS OTP authentication flow for signin"""
    print("\n" + "="*60)
    print("🔧 Testing SMS OTP Authentication Flow...")
    print("="*60)
    
    # For this test, we need a phone number that's already registered
    # In a real scenario, you'd use an existing verified phone number
    print("\n1. Requesting authentication OTP...")
    phone_number = "+15551234567"  # Use a pre-registered phone number
    
    auth_request_response = requests.post(
        f"{BASE_URL}/auth/sms-otp/authentications",
        json={
            "phoneNumber": phone_number,
            "countryCode": "US"
        }
    )
    
    if auth_request_response.status_code != 200:
        print(f"⚠️  No verified phone number found: {phone_number}")
        print("   Run registration test first or use a registered phone number")
        return False
    
    auth_request_data = print_response(auth_request_response, "Authentication OTP request")
    if not auth_request_data or not auth_request_data.get('success'):
        print("❌ Failed to request authentication OTP")
        return False
    
    session_id = auth_request_data.get('sessionId')
    print(f"✅ Authentication OTP sent")
    print(f"   Session ID: {session_id}")
    
    # Step 2: Verify OTP
    print("\n2. Verifying authentication OTP...")
    print("   NOTE: Check server logs for actual OTP code")
    print("   Using mock OTP: 123456")
    
    auth_verify_response = requests.put(
        f"{BASE_URL}/auth/sms-otp/authentications/{session_id}",
        json={
            "phoneNumber": phone_number,
            "otpCode": "123456"  # Mock OTP for testing
        }
    )
    
    auth_verify_data = print_response(auth_verify_response, "Authentication verification")
    if auth_verify_data and auth_verify_data.get('success'):
        print(f"✅ Authentication successful!")
        if auth_verify_data.get('userId'):
            print(f"   User ID: {auth_verify_data.get('userId')}")
        return True
    else:
        print("❌ Authentication failed")
        print("   Check server logs for the actual OTP code")
        return False

def main():
    """Run all SMS OTP tests"""
    print("\n" + "="*60)
    print("SMS OTP TEST SUITE")
    print("="*60)
    
    # Test registration flow
    registration_success = test_sms_otp_registration()
    
    # Wait a bit before testing authentication
    if registration_success:
        print("\n⏳ Waiting 2 seconds before authentication test...")
        time.sleep(2)
    
    # Test authentication flow
    authentication_success = test_sms_otp_authentication()
    
    # Summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    print(f"Registration Test: {'✅ PASSED' if registration_success else '❌ FAILED'}")
    print(f"Authentication Test: {'✅ PASSED' if authentication_success else '⚠️ SKIPPED/FAILED'}")
    
    if registration_success:
        print("\n✅ SMS OTP is working for registration!")
        print("   - Phone numbers can be registered")
        print("   - OTP codes are sent (check logs in dev mode)")
        print("   - Phone verification updates session tokens")
    
    print("\n💡 NOTES:")
    print("   - In dev mode, OTP codes are logged to console")
    print("   - In production, real SMS would be sent via Firebase")
    print("   - Authentication requires a pre-verified phone number")

if __name__ == "__main__":
    main()