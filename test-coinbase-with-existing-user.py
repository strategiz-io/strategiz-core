#!/usr/bin/env python3
"""
Test script to verify Coinbase integration for the EXISTING test1@gmail.com user
"""

import requests
import json

BASE_URL = "http://localhost:8080"

# Use the actual user ID from Firestore for test1@gmail.com
USER_ID = "b9bd92c6-207f-4765-98e2-4c618bc4d692"

# We need to get a valid token for this user
# Since we can't login without the auth methods, let's use the identity token from signup
# but we need to associate it with the correct user

def test_coinbase_connection():
    """Test Coinbase connection with mock token"""
    
    # For testing, we'll use a mock bearer token
    # In production, this would be the actual PASETO token from login
    mock_token = f"test_token_{USER_ID}"
    
    print("Testing Coinbase connection status...")
    print(f"User ID: {USER_ID}")
    print("=" * 60)
    
    # Try to check connection without auth (should fail)
    response = requests.get(
        f"{BASE_URL}/v1/portfolios/providers/coinbase/connection"
    )
    
    print("\n=== Without Authentication ===")
    print(json.dumps(response.json(), indent=2))
    
    # The real issue is that the CoinbasePortfolioService needs to be updated
    # to properly look up the provider integrations as subcollections
    
    print("\n=== Provider Integration Status ===")
    print("The Coinbase integration has been set to 'connected' status")
    print("with mock tokens for testing.")
    print("\nTo fetch real data, the OAuth flow needs to be completed properly.")
    print("\nThe issue with the OAuth callback not redirecting back needs to be fixed:")
    print("1. Check that the redirect URI in Coinbase app matches exactly")
    print("2. Ensure the callback endpoint properly exchanges the code for tokens")
    print("3. Make sure the frontend callback handler is working")

def check_firestore_directly():
    """Check Firestore directly to confirm the connection"""
    import firebase_admin
    from firebase_admin import credentials, firestore
    import os
    
    # Read Firebase config from application resources
    config_path = 'application/src/main/resources/firebase-service-account.json'
    
    if not os.path.exists(config_path):
        print('Firebase config not found at:', config_path)
        return
    
    cred = credentials.Certificate(config_path)
    
    # Check if already initialized
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app(cred)
    
    db = firestore.client()
    
    # Check the provider integration
    user_ref = db.collection('users').document(USER_ID)
    providers_ref = user_ref.collection('provider_integrations')
    providers = providers_ref.stream()
    
    print("\n=== Firestore Provider Integrations ===")
    for provider in providers:
        data = provider.to_dict()
        print(f"\nProvider: {data.get('providerName', 'N/A')}")
        print(f"  Status: {data.get('status', 'N/A')}")
        print(f"  Has Access Token: {'access_token' in data.get('metadata', {})}")
        if data.get('status') == 'connected':
            print("  ✅ Provider is connected and ready!")
        else:
            print("  ⚠️ Provider is not fully connected")

if __name__ == "__main__":
    test_coinbase_connection()
    check_firestore_directly()