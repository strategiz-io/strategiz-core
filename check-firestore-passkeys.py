#!/usr/bin/env python3
"""
Quick script to check what's actually in Firestore for passkey documents
"""
import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Firebase
cred = credentials.ApplicationDefault()
firebase_admin.initialize_app(cred, {
    'projectId': 'strategiz-trading'
})

db = firestore.client()

print("=== Checking Firestore for Passkey Documents ===\n")

# Try collection group query
print("1. Running collection group query:")
print("   collectionGroup('security')")
print("   .where('authentication_method', '==', 'PASSKEY')")
print("   .where('isActive', '==', True)\n")

docs = db.collection_group('security') \
    .where('authentication_method', '==', 'PASSKEY') \
    .where('isActive', '==', True) \
    .limit(10) \
    .stream()

doc_count = 0
for doc in docs:
    doc_count += 1
    data = doc.to_dict()
    print(f"✅ Found document: {doc.reference.path}")
    print(f"   authentication_method: {data.get('authentication_method')}")
    print(f"   isActive: {data.get('isActive')}")
    print(f"   metadata.credentialId: {data.get('metadata', {}).get('credentialId')}")
    print()

if doc_count == 0:
    print("❌ No documents found with collection group query!\n")
    print("2. Checking users collection manually...\n")

    users = db.collection('users').limit(5).stream()
    for user in users:
        print(f"User: {user.id}")
        security_docs = db.collection('users').document(user.id).collection('security').stream()

        for sec_doc in security_docs:
            data = sec_doc.to_dict()
            print(f"  - Security doc {sec_doc.id}:")
            print(f"    authentication_method: {data.get('authentication_method')}")
            print(f"    type: {data.get('type')}")
            print(f"    isActive: {data.get('isActive')}")
            print(f"    metadata.credentialId: {data.get('metadata', {}).get('credentialId')}")
            print()
else:
    print(f"\n✅ Query found {doc_count} documents!")

print("\nDone!")
