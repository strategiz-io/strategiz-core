#!/usr/bin/env python3

import requests
import json
import time
import pyotp
import sys
import uuid

BASE_URL = "http://localhost:8080"

def test_totp_registration():
    print("🔧 Testing TOTP Registration Fix...")
    
    # Step 1: Create a test user (signup)
    print("\n1. Creating test user...")
    unique_email = f"totptest+{uuid.uuid4().hex[:8]}@example.com"
    signup_data = {
        "name": "TOTP Test User",
        "email": unique_email, 
        "password": "TestPassword123!"
    }
    
    response = requests.post(f"{BASE_URL}/v1/signup/profile", json=signup_data)
    if response.status_code not in [200, 201]:
        print(f"❌ Signup failed: {response.status_code} - {response.text}")
        return False
        
    signup_result = response.json()
    user_id = signup_result.get("userId")
    session_token = signup_result.get("identityToken")
    
    print(f"✅ User created: {user_id}")
    print(f"   Session token: {session_token[:20]}...")
    
    # Step 2: Begin TOTP registration
    print("\n2. Beginning TOTP registration...")
    totp_begin_data = {
        "userId": user_id,
        "sessionToken": session_token
    }
    
    response = requests.post(f"{BASE_URL}/v1/auth/totp/registrations", json=totp_begin_data)
    if response.status_code != 200:
        print(f"❌ TOTP begin failed: {response.status_code} - {response.text}")
        return False
        
    totp_result = response.json()
    print(f"   Full TOTP begin response: {json.dumps(totp_result, indent=2)}")
    
    registration_id = totp_result.get("registrationId")
    secret = totp_result.get("secret")
    qr_code = totp_result.get("qrCode")
    
    print(f"✅ TOTP registration started")
    print(f"   Registration ID: {registration_id}")
    print(f"   Secret: {secret}")
    if qr_code:
        print(f"   QR Code: {qr_code[:50]}...")
    else:
        print("   QR Code: None")
    
    # Step 3: Generate TOTP code and complete registration
    print("\n3. Generating TOTP code and completing registration...")
    
    # Generate TOTP code using the secret
    totp = pyotp.TOTP(secret)
    totp_code = totp.now()
    print(f"   Generated TOTP code: {totp_code}")
    
    # Complete TOTP registration
    complete_data = {
        "userId": user_id,
        "sessionToken": session_token,
        "totpCode": totp_code
    }
    
    response = requests.put(f"{BASE_URL}/v1/auth/totp/registrations/{registration_id}", json=complete_data)
    print(f"   Complete registration response: {response.status_code}")
    
    if response.status_code != 200:
        print(f"❌ TOTP completion failed: {response.status_code} - {response.text}")
        return False
    
    complete_result = response.json()
    print(f"✅ TOTP registration completed successfully!")
    print(f"   Result: {json.dumps(complete_result, indent=2)}")
    
    return True

if __name__ == "__main__":
    success = test_totp_registration()
    if success:
        print("\n🎉 TOTP registration test PASSED!")
        sys.exit(0)
    else:
        print("\n💥 TOTP registration test FAILED!")
        sys.exit(1)