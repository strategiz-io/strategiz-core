#!/usr/bin/env python3
import requests
import json
import urllib3

# Disable SSL warnings for self-signed certificates
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def move_provider_integration():
    """Move provider integration from one user to another"""
    
    old_user_id = "52dcb5af-0ee1-4d6f-b1e6-528ce6a6a166"
    new_user_id = "5282f372-2be6-462a-a19e-d3ffd570a0ed"
    
    print(f"Moving provider integrations from user {old_user_id}")
    print(f"                            to user {new_user_id}")
    
    # Note: This would need Firebase Admin SDK to actually move the data
    # For now, we'll just output what needs to be done
    
    print("\nTo fix this issue, you need to:")
    print("1. Delete the provider integration from the old user")
    print("2. Re-connect the provider while logged in as the current user")
    print("\nOr alternatively:")
    print("1. Log in with the user that originally connected the provider")
    print(f"   (user ID: {old_user_id})")

if __name__ == "__main__":
    move_provider_integration()