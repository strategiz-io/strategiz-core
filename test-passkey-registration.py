#!/usr/bin/env python3
import requests
import json
import base64
import sys

# Configuration
BASE_URL = "http://localhost:8080"
EMAIL = "test10@gmail.com"

def main():
    if len(sys.argv) < 2:
        print("Usage: python test-passkey-registration.py <temp_token>")
        sys.exit(1)
    
    temp_token = sys.argv[1]
    
    # Step 1: Begin registration
    print("\n=== Step 1: Begin Registration ===")
    begin_url = f"{BASE_URL}/v1/auth/passkeys/registrations"
    headers = {
        "Content-Type": "application/json"
    }
    
    begin_data = {
        "email": EMAIL,
        "identityToken": temp_token
    }
    
    print(f"Request URL: {begin_url}")
    print(f"Request data: {json.dumps(begin_data, indent=2)}")
    
    begin_response = requests.post(begin_url, headers=headers, json=begin_data)
    print(f"Status: {begin_response.status_code}")
    print(f"Response: {json.dumps(begin_response.json(), indent=2)}")
    
    if begin_response.status_code != 200:
        print("Begin registration failed!")
        return
    
    challenge_data = begin_response.json()
    challenge = challenge_data["challenge"]
    
    print(f"\nChallenge received: {challenge}")
    print(f"Challenge length: {len(challenge)}")
    
    # Step 2: Simulate client data creation
    print("\n=== Step 2: Simulating Client Data ===")
    
    # In a real WebAuthn flow, the browser creates this
    client_data = {
        "type": "webauthn.create",
        "challenge": challenge,  # The challenge from the server
        "origin": "http://localhost:3000",
        "crossOrigin": False
    }
    
    # Convert to JSON and then Base64URL encode
    client_data_json = json.dumps(client_data, separators=(',', ':'))
    print(f"Client data JSON: {client_data_json}")
    
    # Base64URL encode (no padding, URL-safe characters)
    client_data_base64 = base64.urlsafe_b64encode(client_data_json.encode()).decode().rstrip('=')
    print(f"Encoded clientDataJSON: {client_data_base64}")
    
    # For testing, let's decode it back to verify
    decoded = base64.urlsafe_b64decode(client_data_base64 + '===')
    print(f"Decoded back: {decoded.decode()}")
    
    # Step 3: Create a fake attestation object (for testing only)
    print("\n=== Step 3: Creating Test Attestation ===")
    
    # This is a simplified test - real attestation is much more complex
    fake_credential_id = base64.urlsafe_b64encode(b"test-credential-id-12345").decode().rstrip('=')
    fake_attestation = base64.urlsafe_b64encode(b"fake-attestation-object").decode().rstrip('=')
    
    # Step 4: Complete registration
    print("\n=== Step 4: Complete Registration ===")
    # For now, we'll use a placeholder registration ID
    registration_id = "test-registration"
    complete_url = f"{BASE_URL}/v1/auth/passkeys/registrations/{registration_id}"
    
    complete_data = {
        "credentialId": fake_credential_id,
        "clientDataJSON": client_data_base64,
        "attestationObject": fake_attestation,
        "email": EMAIL,
        "deviceId": "test-device",
        "identityToken": temp_token
    }
    
    print(f"Complete request URL: {complete_url}")
    print(f"Complete request data: {json.dumps(complete_data, indent=2)}")
    
    complete_response = requests.put(complete_url, headers=headers, json=complete_data)
    print(f"\nStatus: {complete_response.status_code}")
    print(f"Response: {json.dumps(complete_response.json(), indent=2)}")

if __name__ == "__main__":
    main()