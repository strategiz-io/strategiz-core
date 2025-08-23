#!/usr/bin/env python3
"""
Script to complete Coinbase OAuth flow and get REAL access tokens
"""

import requests
import json
import webbrowser
import time
from urllib.parse import urlparse, parse_qs

print("=" * 60)
print("COINBASE OAUTH SETUP - REAL DATA CONNECTION")
print("=" * 60)
print()

# Configuration
BASE_URL = "http://localhost:8080"
COINBASE_AUTH_URL = "https://www.coinbase.com/oauth/authorize"
COINBASE_TOKEN_URL = "https://api.coinbase.com/oauth/token"
REDIRECT_URI = "http://localhost:8080/v1/providers/callback/coinbase"

# Get OAuth credentials from Vault
import subprocess
result = subprocess.run([
    "sh", "-c",
    "VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=root vault kv get -format=json secret/strategiz/oauth/coinbase"
], capture_output=True, text=True)

if result.returncode != 0:
    print("ERROR: Failed to get OAuth credentials from Vault")
    print("Make sure Vault is running and credentials are configured")
    exit(1)

vault_data = json.loads(result.stdout)
CLIENT_ID = vault_data['data']['data']['client-id']
CLIENT_SECRET = vault_data['data']['data']['client-secret']

print(f"Using Client ID: {CLIENT_ID}")
print(f"Redirect URI: {REDIRECT_URI}")
print()

# Step 1: Generate authorization URL
USER_ID = "dd3ed762-5dbc-4618-9b81-64dff9d633a2"  # test5@gmail.com user
STATE = f"coinbase_{USER_ID}_{int(time.time())}"
SCOPE = "wallet:accounts:read,wallet:transactions:read,wallet:buys:read,wallet:sells:read"

auth_url = f"{COINBASE_AUTH_URL}?response_type=code&client_id={CLIENT_ID}&redirect_uri={REDIRECT_URI}&scope={SCOPE}&state={STATE}"

print("STEP 1: Authorize Coinbase Access")
print("-" * 40)
print("Opening authorization URL in your browser...")
print()
print("Authorization URL:")
print(auth_url)
print()

# Open browser
webbrowser.open(auth_url)

print("After clicking 'Allow access' on Coinbase, you'll be redirected.")
print("The redirect will likely fail (that's OK), but copy the URL from your browser.")
print()
print("It will look like:")
print(f"{REDIRECT_URI}?code=XXXXX&state={STATE}")
print()

# Step 2: Get the authorization code
redirect_url = input("Paste the redirect URL here: ").strip()

if not redirect_url:
    print("No URL provided. Exiting.")
    exit(1)

# Parse the authorization code from the URL
parsed = urlparse(redirect_url)
params = parse_qs(parsed.query)

if 'code' not in params:
    print("ERROR: No authorization code found in the URL")
    print("Make sure you copied the complete URL after clicking 'Allow'")
    exit(1)

auth_code = params['code'][0]
print(f"\nAuthorization code received: {auth_code[:20]}...")

# Step 3: Exchange code for access token
print("\nSTEP 2: Exchange Code for Access Token")
print("-" * 40)

token_data = {
    'grant_type': 'authorization_code',
    'code': auth_code,
    'client_id': CLIENT_ID,
    'client_secret': CLIENT_SECRET,
    'redirect_uri': REDIRECT_URI
}

print("Exchanging authorization code for access token...")
response = requests.post(COINBASE_TOKEN_URL, data=token_data)

if response.status_code == 200:
    token_response = response.json()
    access_token = token_response.get('access_token')
    refresh_token = token_response.get('refresh_token')
    expires_in = token_response.get('expires_in', 7200)
    scope = token_response.get('scope', '')
    
    print("✅ Successfully obtained access token!")
    print(f"  Access Token: {access_token[:30]}...")
    print(f"  Refresh Token: {refresh_token[:30]}..." if refresh_token else "  No refresh token")
    print(f"  Expires in: {expires_in} seconds")
    print(f"  Scope: {scope}")
    
    # Step 4: Update Firestore with real tokens
    print("\nSTEP 3: Save Real Tokens to Database")
    print("-" * 40)
    
    save_script = f'''
import firebase_admin
from firebase_admin import credentials, firestore
import os

config_path = 'application/src/main/resources/firebase-service-account.json'
cred = credentials.Certificate(config_path)
firebase_admin.initialize_app(cred)

db = firestore.client()

# Update the provider integration with REAL tokens
user_ref = db.collection('users').document('{USER_ID}')
providers_ref = user_ref.collection('provider_integrations')

# Find the first Coinbase integration
providers = providers_ref.where('providerName', '==', 'coinbase').stream()

updated = False
for provider in providers:
    provider_ref = providers_ref.document(provider.id)
    provider_ref.update({{
        'status': 'connected',
        'providerId': 'coinbase',
        'metadata': {{
            'access_token': '{access_token}',
            'refresh_token': '{refresh_token}' if refresh_token else '',
            'token_type': 'Bearer',
            'expires_in': {expires_in},
            'scope': '{scope}'
        }},
        'lastSyncAt': firestore.SERVER_TIMESTAMP,
        'modifiedDate': firestore.SERVER_TIMESTAMP
    }})
    print(f"✅ Updated provider integration: {{provider.id}}")
    updated = True
    break

if not updated:
    print("❌ No Coinbase integration found to update")
'''
    
    with open('/tmp/save_real_tokens.py', 'w') as f:
        f.write(save_script)
    
    subprocess.run(['python3', '/tmp/save_real_tokens.py'])
    
    # Step 5: Test the connection
    print("\nSTEP 4: Test Real Coinbase Connection")
    print("-" * 40)
    
    # Test API call to Coinbase
    headers = {
        'Authorization': f'Bearer {access_token}',
        'CB-VERSION': '2021-04-29'
    }
    
    test_response = requests.get('https://api.coinbase.com/v2/user', headers=headers)
    
    if test_response.status_code == 200:
        user_data = test_response.json()
        print("✅ Successfully connected to Coinbase!")
        print(f"  User: {user_data.get('data', {}).get('name', 'N/A')}")
        print(f"  Email: {user_data.get('data', {}).get('email', 'N/A')}")
        
        # Get accounts
        accounts_response = requests.get('https://api.coinbase.com/v2/accounts', headers=headers)
        if accounts_response.status_code == 200:
            accounts_data = accounts_response.json()
            accounts = accounts_data.get('data', [])
            print(f"\n  Found {len(accounts)} accounts:")
            for account in accounts[:5]:  # Show first 5
                if float(account.get('balance', {}).get('amount', 0)) > 0:
                    print(f"    - {account.get('currency')}: {account.get('balance', {}).get('amount')} ({account.get('native_balance', {}).get('currency')} ${account.get('native_balance', {}).get('amount')})")
    else:
        print(f"❌ Failed to test connection: {test_response.status_code}")
        print(test_response.text)
    
    print("\n" + "=" * 60)
    print("SETUP COMPLETE!")
    print("=" * 60)
    print("\nYour Coinbase account is now connected with REAL data.")
    print("You can now fetch real portfolio data through the API.")
    print("\nTest it in the UI:")
    print("1. Go to http://localhost:3000")
    print("2. Login as test1@gmail.com")
    print("3. Navigate to Dashboard or Portfolio")
    print("4. You should see your REAL Coinbase holdings!")
    
else:
    print(f"❌ Failed to exchange code for token: {response.status_code}")
    print(response.text)
    print("\nPossible issues:")
    print("1. The authorization code may have expired (they're only valid for a few minutes)")
    print("2. The redirect URI might not match exactly what's configured in your Coinbase app")
    print("3. The client ID/secret might be incorrect")
    print("\nTry running this script again and complete the flow quickly.")