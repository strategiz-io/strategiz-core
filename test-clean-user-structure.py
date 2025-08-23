#!/usr/bin/env python3
"""
Script to verify that user structure in Firestore is clean after our changes
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

print("Checking user structure after cleanup...")
print("=" * 80)

# Search for a test user
test_email = "test-cleanup@example.com"

users_ref = db.collection('users')
query = users_ref.where('profile.email', '==', test_email).limit(1)
results = query.get()

if results:
    for doc in results:
        print(f"\n✅ Found test user:")
        print(f"  Document ID: {doc.id}")
        
        data = doc.to_dict()
        print(f"\n  User Document Structure:")
        print(json.dumps(data, indent=2, default=str))
        
        # Check for unwanted fields at root level
        unwanted_fields = ['active', 'deleted', 'email', 'emailVerified', 'name', 'profileActive']
        found_unwanted = []
        
        for field in unwanted_fields:
            if field in data:
                found_unwanted.append(field)
        
        if found_unwanted:
            print(f"\n  ❌ WARNING: Found unwanted fields at root level: {found_unwanted}")
        else:
            print(f"\n  ✅ GOOD: No unwanted fields at root level")
        
        # Check profile structure
        if 'profile' in data:
            profile = data['profile']
            print(f"\n  Profile Structure:")
            print(json.dumps(profile, indent=4, default=str))
            
            expected_profile_fields = ['name', 'email', 'isEmailVerified', 'subscriptionTier', 'tradingMode']
            for field in expected_profile_fields:
                if field in profile:
                    print(f"    ✅ {field}: {profile[field]}")
                else:
                    print(f"    ❌ Missing: {field}")
else:
    print(f"\n❌ No test user found with email: {test_email}")
    print("\nListing recent users to check structure:")
    
    # Get most recent users
    recent_users = users_ref.order_by('createdDate', direction=firestore.Query.DESCENDING).limit(3).get()
    
    for doc in recent_users:
        data = doc.to_dict()
        profile = data.get('profile', {})
        print(f"\n  User ID: {doc.id}")
        print(f"    Email: {profile.get('email', 'N/A')}")
        print(f"    Name: {profile.get('name', 'N/A')}")
        print(f"    Created: {data.get('createdDate', 'N/A')}")
        
        # Check for unwanted fields
        unwanted_fields = ['active', 'deleted', 'email', 'emailVerified', 'name', 'profileActive']
        found_unwanted = [field for field in unwanted_fields if field in data]
        
        if found_unwanted:
            print(f"    ❌ Has unwanted root fields: {found_unwanted}")
        else:
            print(f"    ✅ Clean structure (no unwanted root fields)")

print("\n" + "=" * 80)
print("Structure check complete!")