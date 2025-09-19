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

print(f"🔍 Inspecting portfolio_data for user: {user_id}\n")

# Check for portfolio_data subcollection
portfolio_data_ref = db.collection('users').document(user_id).collection('portfolio_data')
portfolio_docs = portfolio_data_ref.stream()

print("💼 Portfolio Data Collection:")
for doc in portfolio_docs:
    print(f"\n📄 Document ID: {doc.id}")
    data = doc.to_dict()
    
    # Show structure
    print(f"  Keys: {list(data.keys())}")
    
    # Show holdings if present
    if 'holdings' in data:
        holdings = data['holdings']
        print(f"  Holdings count: {len(holdings)}")
        if len(holdings) > 0:
            print("\n  First 3 holdings:")
            for i, holding in enumerate(holdings[:3]):
                print(f"\n  [{i+1}] Asset: {holding.get('asset', 'N/A')}")
                print(f"      Name: {holding.get('name', 'N/A')}")
                print(f"      Quantity: {holding.get('quantity', 'N/A')}")
                print(f"      Value: {holding.get('currentValue', 'N/A')}")
                print(f"      Price: {holding.get('currentPrice', 'N/A')}")
    
    # Show other important fields
    if 'totalValue' in data:
        print(f"\n  Total Value: {data['totalValue']}")
    if 'lastSyncTime' in data:
        print(f"  Last Sync: {data['lastSyncTime']}")
    if 'enhancementApplied' in data:
        print(f"  Enhancement Applied: {data['enhancementApplied']}")