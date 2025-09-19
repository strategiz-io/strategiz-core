#!/usr/bin/env python3

import firebase_admin
from firebase_admin import credentials, firestore
import sys

def check_user_providers(user_email):
    # Initialize Firebase Admin SDK
    try:
        # Try to get existing app
        app = firebase_admin.get_app()
    except ValueError:
        # Initialize if not already done
        cred = credentials.Certificate('/Users/cuztomizer/Documents/GitHub/strategiz-core/firebase-service-account.json')
        app = firebase_admin.initialize_app(cred)

    db = firestore.client()

    print(f"Checking provider integrations for user: {user_email}")
    print("=" * 60)

    # Query provider integrations for this user
    provider_integrations = db.collection('provider_integrations').where('userId', '==', user_email).get()

    if not provider_integrations:
        print("No provider integrations found for this user.")
        return

    for doc in provider_integrations:
        data = doc.to_dict()
        print(f"Document ID: {doc.id}")
        print(f"Provider ID: {data.get('providerId', 'N/A')}")
        print(f"Status: {data.get('status', 'N/A')}")
        print(f"Enabled: {data.get('enabled', 'N/A')}")
        print(f"Connection Type: {data.get('connectionType', 'N/A')}")
        print(f"Created Date: {data.get('createdDate', 'N/A')}")
        print(f"Updated Date: {data.get('updatedDate', 'N/A')}")
        print("-" * 40)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python check-user-providers.py <user_email>")
        sys.exit(1)

    user_email = sys.argv[1]
    check_user_providers(user_email)