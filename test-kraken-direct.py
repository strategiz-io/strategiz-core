import requests
import json

# Disable SSL warnings for self-signed certificate
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Test Kraken API connection directly to backend
url = "https://localhost:8443/v1/providers"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer v1.aeadNXFOYnBHMzhjOFlCcnp0a2F2SVRQcXFQdDB1dUgxMFBscGJRQzRlb0h0.UUlvUXZwSGNlUnJ1OHA1dmoxMjNKTUd1Z092cEl5SVFTam5jREJhWDZKazI4.10.1735011427.KGa1T5vKlSc0vAP7lYWFvE93HahALJBMGnHJD3BO0mU"
}

# Kraken credentials
data = {
    "providerId": "kraken",
    "connectionType": "api_key",
    "credentials": {
        "apiKey": "BePVKxVVOtT5u1P5t6ScwgB9BkghR+fR3phFr4FwyZvcHttPbdGcBBBr",
        "apiSecret": "X/6pkGE5Hfy+m9NugJoazSUfLoaZXRP1yrQW8XVUSO/acAcYSmKABtBT7zWZgDzh735nsz1RPexfPqrWlqD6zQ=="
    }
}

response = requests.post(url, headers=headers, json=data, verify=False)
print(f"Status: {response.status_code}")
print(f"Response: {json.dumps(response.json(), indent=2)}")