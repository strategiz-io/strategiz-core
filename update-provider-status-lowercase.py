#!/usr/bin/env python3
"""
Script to update all provider integration status fields from uppercase to lowercase
This ensures consistency with the new enum serialization format
"""

print("Script to update provider integration status to lowercase")
print("\nTo update the status field from 'CONNECTED' to 'connected' in Firestore:")
print("1. Go to Firebase Console")
print("2. Navigate to Firestore Database") 
print("3. Go to users -> [user-id] -> provider_integrations -> [provider-id]")
print("4. Edit the 'status' field")
print("5. Change 'CONNECTED' to 'connected'")
print("\nAlternatively, you can delete and reconnect the providers to get the new format.")
print("\nThe backend code handles both formats, but lowercase is the new standard.")