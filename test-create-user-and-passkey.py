#!/usr/bin/env python3
"""
Test script to create a user profile and then register a passkey.
This simulates the complete flow from user signup to passkey registration.
"""
import requests
import json
import base64
import sys

BASE_URL = "http://localhost:8080"

def create_user_profile():
    """Step 1: Create user profile"""
    print("\n=== Step 1: Creating User Profile ===")
    
    profile_data = {
        "name": "Test Passkey",
        "email": "testpasskey@example.com",
        "photoURL": "https://example.com/photo.jpg"
    }
    
    response = requests.post(
        f"{BASE_URL}/v1/signup/profile",
        json=profile_data,
        headers={"Content-Type": "application/json"}
    )
    
    print(f"Status: {response.status_code}")
    if response.status_code in [200, 201]:
        result = response.json()
        print(f"Response: {json.dumps(result, indent=2)}")
        return result.get("identityToken"), result.get("userId")
    else:
        print(f"Error: {response.text}")
        return None, None

def begin_passkey_registration(temp_token, email):
    """Begin passkey registration"""
    print("\n=== Step 2: Beginning Passkey Registration ===")
    
    registration_data = {
        "email": email,
        "identityToken": temp_token
    }
    
    response = requests.post(
        f"{BASE_URL}/v1/auth/passkeys/registrations",
        json=registration_data,
        headers={"Content-Type": "application/json"}
    )
    
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"Response: {json.dumps(result, indent=2)}")
        return result
    else:
        print(f"Error: {response.text}")
        return None

def complete_passkey_registration(temp_token, email, challenge_data):
    """Complete passkey registration with simulated credential"""
    print("\n=== Step 3: Completing Passkey Registration ===")
    
    # Extract challenge
    challenge = challenge_data.get("challenge", "")
    
    # Create simulated client data
    client_data = {
        "type": "webauthn.create",
        "challenge": challenge,
        "origin": "http://localhost:3000",
        "crossOrigin": False
    }
    
    # Encode client data
    client_data_json = json.dumps(client_data, separators=(',', ':'))
    client_data_base64 = base64.urlsafe_b64encode(client_data_json.encode()).decode().rstrip('=')
    
    # Create fake credential for testing
    fake_credential_id = base64.urlsafe_b64encode(b"test-credential-id-12345").decode().rstrip('=')
    fake_attestation = base64.urlsafe_b64encode(b"fake-attestation-object").decode().rstrip('=')
    
    completion_data = {
        "credentialId": fake_credential_id,
        "clientDataJSON": client_data_base64,
        "attestationObject": fake_attestation,
        "email": email,
        "deviceId": "test-device-123",
        "identityToken": temp_token
    }
    
    # Use a placeholder registration ID for now
    registration_id = "test-registration"
    
    response = requests.put(
        f"{BASE_URL}/v1/auth/passkeys/registrations/{registration_id}",
        json=completion_data,
        headers={"Content-Type": "application/json"}
    )
    
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"Response: {json.dumps(result, indent=2)}")
        return result
    else:
        print(f"Error: {response.text}")
        return None

def main():
    print("Passkey Registration Test - Complete Flow")
    print("=========================================")
    
    # Step 1: Create user profile
    temp_token, user_id = create_user_profile()
    if not temp_token:
        print("Failed to create user profile")
        return
    
    print(f"\nReceived temporary token: {temp_token}")
    print(f"User ID: {user_id}")
    
    # Step 2: Begin passkey registration
    email = "testpasskey@example.com"
    challenge_data = begin_passkey_registration(temp_token, email)
    if not challenge_data:
        print("Failed to begin passkey registration")
        return
    
    # Step 3: Complete passkey registration
    auth_tokens = complete_passkey_registration(temp_token, email, challenge_data)
    if auth_tokens:
        print("\n✅ Passkey registration completed successfully!")
        print(f"Access Token: {auth_tokens.get('accessToken', 'N/A')}")
        print(f"Refresh Token: {auth_tokens.get('refreshToken', 'N/A')}")
    else:
        print("\n❌ Passkey registration failed")

if __name__ == "__main__":
    main()