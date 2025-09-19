#!/usr/bin/env python3

import requests
import json
from requests.packages.urllib3.exceptions import InsecureRequestWarning

# Disable SSL warnings for self-signed certificates
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# Configuration
BASE_URL = "https://localhost:8443"
USER_ID = "fff74730-4a58-45fe-be74-cf38a57dcb0b"

# You'll need to get the session token from your browser
# In Chrome DevTools: Application -> Cookies -> Copy the session token value
print("Please provide your session token from the browser:")
print("1. Open Chrome DevTools (F12)")
print("2. Go to Application -> Cookies")
print("3. Find and copy the 'session' or 'SESSION' cookie value")
session_token = input("Session token: ").strip()

# Disable demo mode
url = f"{BASE_URL}/v1/profile/demo-mode"
headers = {
    "Content-Type": "application/json",
    "Cookie": f"SESSION={session_token}"
}
data = {
    "demoMode": False
}

print(f"\nDisabling demo mode for user: {USER_ID}")
response = requests.put(url, headers=headers, json=data, verify=False)

if response.status_code == 200:
    print("✅ Demo mode successfully disabled!")
    print("Response:", response.json())
    print("\nYour portfolio should now show real Kraken data.")
    print("Please refresh the portfolio page to see the changes.")
else:
    print(f"❌ Failed to disable demo mode. Status: {response.status_code}")
    print("Response:", response.text)