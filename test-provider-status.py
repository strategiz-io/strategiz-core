#!/usr/bin/env python3
import requests
import json
import urllib3

# Disable SSL warnings for self-signed certificates
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def test_provider_status():
    """Test provider status endpoint"""
    
    # Get the access token for user "gabe"
    # This would normally come from authentication, but for testing we'll use a known token
    # You'll need to replace this with an actual valid token
    
    base_url = "https://localhost:8443"
    
    # First, we need to authenticate - let's use a simple test endpoint
    try:
        # Test the providers endpoint
        headers = {
            "Authorization": "Bearer v2.public.eyJ1aWQiOiJnYWJlIiwiZXhwIjoiMjAyNS0wOS0wNVQwNjoxNzo0OS4wMDlaIn0J-TQAiTCmgD8l7jZE6fSzayy0qbRBGhCl3KaOhGCTK3gk3RnJLKCXGBQ2-HQdVoLsYTQySYJazrRLuWxCG_YG",
            "Content-Type": "application/json"
        }
        
        response = requests.get(
            f"{base_url}/v1/providers",
            headers=headers,
            verify=False
        )
        
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        
        if response.status_code == 200:
            data = response.json()
            if data.get("providers"):
                for provider in data["providers"]:
                    print(f"\nProvider: {provider.get('providerId')}")
                    print(f"  Status: {provider.get('status')}")
                    print(f"  Connection Type: {provider.get('connectionType')}")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_provider_status()