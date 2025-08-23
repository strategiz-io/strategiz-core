#!/usr/bin/env python3
"""
Script to manually complete the Coinbase OAuth connection
This will update the pending integration with mock/test tokens
"""

import firebase_admin
from firebase_admin import credentials, firestore
import os
import json
import uuid

# Read Firebase config from application resources
config_path = 'application/src/main/resources/firebase-service-account.json'

if not os.path.exists(config_path):
    print('Firebase config not found at:', config_path)
    exit(1)

cred = credentials.Certificate(config_path)
firebase_admin.initialize_app(cred)

db = firestore.client()

# The user ID for test1@gmail.com
user_id = 'b9bd92c6-207f-4765-98e2-4c618bc4d692'

print(f'Fixing Coinbase connection for user {user_id}')
print('=' * 60)

# Get the pending provider integrations
user_ref = db.collection('users').document(user_id)
providers_ref = user_ref.collection('provider_integrations')
providers = providers_ref.stream()

pending_providers = []
for provider in providers:
    data = provider.to_dict()
    if data.get('status') == 'pending_oauth' and data.get('providerName') == 'coinbase':
        pending_providers.append((provider.id, data))
        print(f'Found pending Coinbase integration: {provider.id}')

if not pending_providers:
    print('No pending Coinbase integrations found.')
    exit(0)

# Use the first pending provider
provider_doc_id, provider_data = pending_providers[0]
print(f'\nUpdating provider integration: {provider_doc_id}')

# Generate mock tokens for testing
# In production, these would come from Coinbase OAuth
mock_access_token = f"mock_access_token_{uuid.uuid4().hex}"
mock_refresh_token = f"mock_refresh_token_{uuid.uuid4().hex}"

# Update the provider integration to connected status
update_data = {
    'status': 'connected',
    'providerId': 'coinbase',
    'metadata': {
        'access_token': mock_access_token,
        'refresh_token': mock_refresh_token,
        'token_type': 'Bearer',
        'expires_in': 7200,
        'scope': 'wallet:accounts:read,wallet:transactions:read,wallet:buys:read,wallet:sells:read',
        'mock_data': True  # Flag to indicate this is mock data
    },
    'lastSyncAt': firestore.SERVER_TIMESTAMP,
    'modifiedDate': firestore.SERVER_TIMESTAMP
}

provider_ref = providers_ref.document(provider_doc_id)
provider_ref.update(update_data)

print(f'✅ Provider integration updated successfully!')
print(f'\nProvider ID: {provider_doc_id}')
print(f'Status: connected')
print(f'Access Token: {mock_access_token[:30]}...')
print(f'\nNOTE: This is using MOCK tokens for testing.')
print('The integration will return sample portfolio data.')
print('\nTo get REAL data, you need to:')
print('1. Configure the correct OAuth redirect URL in your Coinbase app')
print('2. Ensure the callback endpoint is properly handling the OAuth code exchange')
print('3. Complete the actual OAuth flow through the UI')