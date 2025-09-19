#!/usr/bin/env python3

import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Firebase Admin SDK
# This assumes you have GOOGLE_APPLICATION_CREDENTIALS environment variable set
firebase_admin.initialize_app()

# Get Firestore client
db = firestore.client()

# User ID
user_id = "fff74730-4a58-45fe-be74-cf38a57dcb0b"

# Toggle demo mode off
user_ref = db.collection('users').document(user_id)

# Update the profile.demoMode field
user_ref.update({
    'profile.demoMode': False
})

print(f"✅ Demo mode disabled for user {user_id}")
print("The change should take effect immediately.")
print("Refresh your portfolio page to see real Kraken data with enhancements applied.")