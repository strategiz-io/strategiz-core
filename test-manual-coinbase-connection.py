#!/usr/bin/env python3
"""
Manual script to test Coinbase OAuth token exchange
This simulates what should happen when Coinbase redirects back with an authorization code
"""

import requests
import json
import base64

BASE_URL = "http://localhost:8080"
COINBASE_TOKEN_URL = "https://api.coinbase.com/oauth/token"

def exchange_code_for_token(code, redirect_uri):
    """Exchange authorization code for access token"""
    
    # Get OAuth credentials from Vault
    import subprocess
    result = subprocess.run([
        "sh", "-c",
        "VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv get -format=json secret/strategiz/oauth/coinbase"
    ], capture_output=True, text=True)
    
    vault_data = json.loads(result.stdout)
    client_id = vault_data['data']['data']['client-id']
    client_secret = vault_data['data']['data']['client-secret']
    
    print(f"Using client_id: {client_id}")
    
    # Exchange code for token
    token_data = {
        'grant_type': 'authorization_code',
        'code': code,
        'client_id': client_id,
        'client_secret': client_secret,
        'redirect_uri': redirect_uri
    }
    
    response = requests.post(COINBASE_TOKEN_URL, data=token_data)
    
    if response.status_code == 200:
        token_response = response.json()
        print("Successfully exchanged code for token!")
        print(f"Access token: {token_response.get('access_token', 'N/A')[:50]}...")
        print(f"Refresh token: {token_response.get('refresh_token', 'N/A')[:50]}...")
        return token_response
    else:
        print(f"Failed to exchange code: {response.status_code}")
        print(response.text)
        return None

def save_token_to_firestore(user_id, provider_doc_id, token_data):
    """Save the token to Firestore"""
    
    script = f'''
import firebase_admin
from firebase_admin import credentials, firestore
import os

config_path = 'application/src/main/resources/firebase-service-account.json'
cred = credentials.Certificate(config_path)
firebase_admin.initialize_app(cred)

db = firestore.client()

# Update the provider integration
user_ref = db.collection('users').document('{user_id}')
provider_ref = user_ref.collection('provider_integrations').document('{provider_doc_id}')

# Update with token data
provider_ref.update({{
    'status': 'connected',
    'metadata': {{
        'access_token': '{token_data.get('access_token', '')}',
        'refresh_token': '{token_data.get('refresh_token', '')}',
        'token_type': '{token_data.get('token_type', 'Bearer')}',
        'expires_in': {token_data.get('expires_in', 7200)},
        'scope': '{token_data.get('scope', '')}'
    }}
}})

print("Token saved to Firestore successfully!")
'''
    
    with open('/tmp/save_token.py', 'w') as f:
        f.write(script)
    
    import subprocess
    subprocess.run(['python3', '/tmp/save_token.py'])

def main():
    print("Manual Coinbase OAuth Token Exchange")
    print("=" * 40)
    print("\nIf you have the authorization code from Coinbase, enter it below.")
    print("The code appears in the URL after 'code=' when Coinbase redirects back.")
    print("\nExample URL: http://localhost:8080/v1/providers/callback/coinbase?code=ABC123&state=xyz")
    print()
    
    code = input("Enter the authorization code (or 'skip' to skip): ").strip()
    
    if code.lower() == 'skip':
        print("\nSkipping token exchange. You can connect through the UI instead.")
        return
    
    redirect_uri = "http://localhost:8080/v1/providers/callback/coinbase"
    
    # Exchange the code
    token_data = exchange_code_for_token(code, redirect_uri)
    
    if token_data:
        print("\nDo you want to save this token to Firestore?")
        save = input("Enter 'yes' to save: ").strip().lower()
        
        if save == 'yes':
            user_id = 'b9bd92c6-207f-4765-98e2-4c618bc4d692'  # test1@gmail.com user ID
            
            # We need to pick one of the pending provider docs
            # Let's use the first one
            provider_doc_id = 'II1hDDX1se4n8m0j6c7w'
            
            save_token_to_firestore(user_id, provider_doc_id, token_data)
            print("\nToken saved! You can now fetch real Coinbase data.")
        else:
            print("\nToken not saved. You can manually update Firestore if needed.")

if __name__ == "__main__":
    main()