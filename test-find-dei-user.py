#!/usr/bin/env python3
"""
Script to find DEI user in Firestore
"""

import json
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime

# Initialize Firebase Admin SDK
cred = credentials.Certificate('application/src/main/resources/firebase-service-account.json')
firebase_admin.initialize_app(cred)

# Get Firestore client
db = firestore.client()

print("Searching for DEI user in Firestore...")
print("=" * 60)

# Search in users collection
users_ref = db.collection('users')

# Try to find by email
print("\n1. Searching by email (DEI@gmail.com)...")
query = users_ref.where('profile.email', '==', 'DEI@gmail.com')
results = query.get()

for doc in results:
    print(f"\nFound user by email:")
    print(f"  Document ID: {doc.id}")
    data = doc.to_dict()
    print(f"  Data: {json.dumps(data, indent=2, default=str)}")
    
    # Check subcollections
    print(f"\n  Subcollections for user {doc.id}:")
    
    # Check auth-methods
    auth_methods = doc.reference.collection('auth-methods').get()
    print(f"    auth-methods: {len(list(auth_methods))} documents")
    for auth_doc in auth_methods:
        print(f"      - {auth_doc.id}: {auth_doc.to_dict().get('type', 'unknown')}")
    
    # Check other potential subcollections
    collections_to_check = ['watchlist', 'providers', 'devices', 'preferences', 'sessions']
    for coll_name in collections_to_check:
        coll_docs = doc.reference.collection(coll_name).get()
        if coll_docs:
            print(f"    {coll_name}: {len(list(coll_docs))} documents")

# Try to find by name
print("\n2. Searching by name (DEI)...")
query = users_ref.where('profile.name', '==', 'DEI')
results = query.get()

for doc in results:
    print(f"\nFound user by name:")
    print(f"  Document ID: {doc.id}")
    data = doc.to_dict()
    print(f"  Data: {json.dumps(data, indent=2, default=str)}")

# List all users (limited to 10)
print("\n3. Listing all users (max 10)...")
all_users = users_ref.limit(10).get()
print(f"Total users found: {len(list(all_users))}")

for doc in all_users:
    data = doc.to_dict()
    profile = data.get('profile', {})
    print(f"\n  User ID: {doc.id}")
    print(f"    Name: {profile.get('name', 'N/A')}")
    print(f"    Email: {profile.get('email', 'N/A')}")
    print(f"    Created: {data.get('createdAt', 'N/A')}")

print("\n" + "=" * 60)
print("Search complete!")