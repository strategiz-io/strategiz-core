#!/usr/bin/env python3
import hashlib
import hmac
import base64
import time
import urllib.parse
import requests

# Your Kraken API credentials
api_key = "BePVKxVVOtT5u1P5t6ScwgB9BkghR+fR3phFr4FwyZvcHttPbdGcBBBr"
api_secret = "X/6pkGE5Hfy+m9NugJoazSUfLoaZXRP1yrQW8XVUSO/acAcYSmKABtBT7zWZgDzh735nsz1RPexfPqrWlqD6zQ=="

def get_kraken_signature(urlpath, data, secret):
    postdata = urllib.parse.urlencode(data)
    encoded = (str(data['nonce']) + postdata).encode()
    message = urlpath.encode() + hashlib.sha256(encoded).digest()
    
    mac = hmac.new(base64.b64decode(secret), message, hashlib.sha512)
    sigdigest = base64.b64encode(mac.digest())
    return sigdigest.decode()

def kraken_request(uri_path, data, api_key, api_secret):
    api_url = "https://api.kraken.com"
    headers = {
        'API-Key': api_key,
        'API-Sign': get_kraken_signature(uri_path, data, api_secret)
    }
    
    response = requests.post((api_url + uri_path), headers=headers, data=data)
    return response.json()

# Test with balance endpoint
uri_path = "/0/private/Balance"
data = {
    "nonce": str(int(1000*time.time()))
}

try:
    print("Testing Kraken API connection...")
    resp = kraken_request(uri_path, data, api_key, api_secret)
    
    if 'error' in resp and resp['error']:
        print(f"Error: {resp['error']}")
    else:
        print("Success! Connected to Kraken API")
        print(f"Response: {resp}")
except Exception as e:
    print(f"Exception: {e}")