#!/usr/bin/env python3
"""Delete all Kraken provider data from Firestore"""

import firebase_admin
from firebase_admin import credentials, firestore
import json

# Initialize Firebase Admin SDK
cred_path = '/Users/cuztomizer/Documents/GitHub/strategiz-core/application/src/main/resources/firebase-service-account.json'
cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred, {
    'projectId': 'strategiz-trading'
})

db = firestore.client()

def delete_kraken_data():
    """Delete all Kraken provider data from all users"""
    print("🔍 Finding and deleting Kraken provider data...")
    
    # Get all users
    users_ref = db.collection('users')
    users = users_ref.stream()
    
    deleted_count = 0
    for user in users:
        user_id = user.id
        
        # Check if user has Kraken provider data
        kraken_ref = users_ref.document(user_id).collection('provider_data').document('kraken')
        kraken_doc = kraken_ref.get()
        
        if kraken_doc.exists:
            print(f"  Deleting Kraken data for user: {user_id}")
            kraken_ref.delete()
            deleted_count += 1
    
    if deleted_count > 0:
        print(f"✅ Deleted Kraken data for {deleted_count} user(s)")
    else:
        print("ℹ️  No Kraken data found to delete")

if __name__ == "__main__":
    delete_kraken_data()