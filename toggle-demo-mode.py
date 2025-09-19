#!/usr/bin/env python3

import requests
import json
import sys
from requests.packages.urllib3.exceptions import InsecureRequestWarning

# Disable SSL warnings for self-signed certificates
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# Configuration
BASE_URL = "https://localhost:8443"

def toggle_demo_mode(session_token, demo_mode):
    """Toggle demo mode on/off"""
    url = f"{BASE_URL}/v1/profile/demo-mode"
    headers = {
        "Content-Type": "application/json",
        "Cookie": f"SESSION={session_token}"
    }
    data = {
        "demoMode": demo_mode
    }
    
    print(f"\n{'Enabling' if demo_mode else 'Disabling'} demo mode...")
    response = requests.put(url, headers=headers, json=data, verify=False)
    
    if response.status_code == 200:
        print(f"✅ Demo mode successfully {'enabled' if demo_mode else 'disabled'}!")
        print("Response:", response.json())
        if not demo_mode:
            print("\n🎉 Your portfolio should now show real Kraken data with enhancements applied.")
            print("Please refresh the portfolio page to see the changes.")
    else:
        print(f"❌ Failed to toggle demo mode. Status: {response.status_code}")
        print("Response:", response.text)

if __name__ == "__main__":
    print("=== Toggle Demo Mode ===")
    print("\nTo get your session token:")
    print("1. Open Chrome DevTools (F12)")
    print("2. Go to Application -> Cookies -> https://localhost:3000")
    print("3. Find and copy the 'SESSION' cookie value")
    
    session_token = input("\nSession token: ").strip()
    
    print("\nWhat would you like to do?")
    print("1. Disable demo mode (show real data)")
    print("2. Enable demo mode (show test data)")
    
    choice = input("\nChoice (1 or 2): ").strip()
    
    if choice == "1":
        toggle_demo_mode(session_token, False)
    elif choice == "2":
        toggle_demo_mode(session_token, True)
    else:
        print("Invalid choice. Please run the script again.")