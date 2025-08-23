#!/usr/bin/env python3
"""
Script to find lok@gmail.com user in Firestore and show all collections
"""

import json
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime

# Initialize Firebase Admin SDK if not already initialized
try:
    firebase_admin.get_app()
except ValueError:
    cred = credentials.Certificate('application/src/main/resources/firebase-service-account.json')
    firebase_admin.initialize_app(cred)

# Get Firestore client
db = firestore.client()

print("Searching for lok@gmail.com user in Firestore...")
print("=" * 80)

# Search in users collection
users_ref = db.collection('users')

# Try to find by email
print("\nSearching by email (lok@gmail.com)...")
query = users_ref.where('profile.email', '==', 'lok@gmail.com')
results = query.get()

user_found = False
for doc in results:
    user_found = True
    print(f"\n✅ Found user with email lok@gmail.com:")
    print(f"  Document ID: {doc.id}")
    print(f"\n  User Document Data:")
    data = doc.to_dict()
    print(f"  {json.dumps(data, indent=4, default=str)}")
    
    print(f"\n  📁 Subcollections for user {doc.id}:")
    print("  " + "-" * 60)
    
    # Check auth-methods subcollection
    auth_methods = doc.reference.collection('auth-methods').get()
    print(f"\n  1. auth-methods ({len(list(auth_methods))} documents):")
    for auth_doc in auth_methods:
        auth_data = auth_doc.to_dict()
        print(f"     Document ID: {auth_doc.id}")
        print(f"     Type: {auth_data.get('type', 'unknown')}")
        print(f"     Data: {json.dumps(auth_data, indent=8, default=str)}")
    
    # Check watchlist subcollection
    watchlist = doc.reference.collection('watchlist').get()
    print(f"\n  2. watchlist ({len(list(watchlist))} documents):")
    for watch_doc in watchlist:
        watch_data = watch_doc.to_dict()
        print(f"     Document ID: {watch_doc.id}")
        print(f"     Data: {json.dumps(watch_data, indent=8, default=str)}")
    
    # Check providers subcollection
    providers = doc.reference.collection('providers').get()
    print(f"\n  3. providers ({len(list(providers))} documents):")
    for prov_doc in providers:
        prov_data = prov_doc.to_dict()
        print(f"     Document ID: {prov_doc.id}")
        print(f"     Data: {json.dumps(prov_data, indent=8, default=str)}")
    
    # Check devices subcollection
    devices = doc.reference.collection('devices').get()
    print(f"\n  4. devices ({len(list(devices))} documents):")
    for dev_doc in devices:
        dev_data = dev_doc.to_dict()
        print(f"     Document ID: {dev_doc.id}")
        print(f"     Data: {json.dumps(dev_data, indent=8, default=str)}")
    
    # Check preferences subcollection
    preferences = doc.reference.collection('preferences').get()
    print(f"\n  5. preferences ({len(list(preferences))} documents):")
    for pref_doc in preferences:
        pref_data = pref_doc.to_dict()
        print(f"     Document ID: {pref_doc.id}")
        print(f"     Data: {json.dumps(pref_data, indent=8, default=str)}")
    
    # Check sessions subcollection
    sessions = doc.reference.collection('sessions').get()
    print(f"\n  6. sessions ({len(list(sessions))} documents):")
    for sess_doc in sessions:
        sess_data = sess_doc.to_dict()
        print(f"     Document ID: {sess_doc.id}")
        print(f"     Data: {json.dumps(sess_data, indent=8, default=str)}")
    
    # Check strategies subcollection
    strategies = doc.reference.collection('strategies').get()
    print(f"\n  7. strategies ({len(list(strategies))} documents):")
    for strat_doc in strategies:
        strat_data = strat_doc.to_dict()
        print(f"     Document ID: {strat_doc.id}")
        print(f"     Data: {json.dumps(strat_data, indent=8, default=str)}")
    
    # Check portfolio subcollection
    portfolio = doc.reference.collection('portfolio').get()
    print(f"\n  8. portfolio ({len(list(portfolio))} documents):")
    for port_doc in portfolio:
        port_data = port_doc.to_dict()
        print(f"     Document ID: {port_doc.id}")
        print(f"     Data: {json.dumps(port_data, indent=8, default=str)}")

if not user_found:
    print("\n❌ No user found with email: lok@gmail.com")
    print("\nListing all users to help find the right one...")
    all_users = users_ref.limit(20).get()
    print(f"\nTotal users in database (showing first 20):")
    for doc in all_users:
        data = doc.to_dict()
        profile = data.get('profile', {})
        print(f"  - {profile.get('email', 'N/A')} (Name: {profile.get('name', 'N/A')}, ID: {doc.id})")

print("\n" + "=" * 80)
print("Search complete!")