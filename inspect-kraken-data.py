#!/usr/bin/env python3

import firebase_admin
from firebase_admin import credentials, firestore
import json
import os

# Set credentials
os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = './application/target/classes/firebase-service-account.json'

# Initialize Firebase Admin SDK
firebase_admin.initialize_app()

# Get Firestore client
db = firestore.client()

# User ID
user_id = "fff74730-4a58-45fe-be74-cf38a57dcb0b"

print(f"🔍 Inspecting Kraken data for user: {user_id}\n")

# Check provider_integrations
provider_ref = db.collection('users').document(user_id).collection('provider_integrations').document('kraken')
provider_doc = provider_ref.get()

if provider_doc.exists:
    data = provider_doc.to_dict()
    print("📦 Provider Integration Data:")
    print(json.dumps(data, indent=2, default=str))
else:
    print("❌ No provider integration data found")

print("\n" + "="*50 + "\n")

# Check for portfolio_summaries subcollection
portfolio_summaries_ref = db.collection('users').document(user_id).collection('portfolio_summaries')
portfolio_docs = portfolio_summaries_ref.stream()

print("📊 Portfolio Summaries:")
for doc in portfolio_docs:
    print(f"\nDocument ID: {doc.id}")
    print(json.dumps(doc.to_dict(), indent=2, default=str))

print("\n" + "="*50 + "\n")

# Check for provider_data subcollection  
provider_data_ref = db.collection('users').document(user_id).collection('provider_data')
provider_data_docs = provider_data_ref.stream()

print("💼 Provider Data:")
for doc in provider_data_docs:
    print(f"\nDocument ID: {doc.id}")
    data = doc.to_dict()
    # Truncate large arrays for readability
    if 'holdings' in data:
        print(f"  Holdings count: {len(data['holdings'])}")
        if len(data['holdings']) > 0:
            print("  First holding:", json.dumps(data['holdings'][0], indent=4, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))