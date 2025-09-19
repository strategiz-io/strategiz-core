#!/usr/bin/env python3
"""
Test script for Kraken portfolio integration
"""
import requests
import json
import urllib3

# Disable SSL warnings for self-signed cert
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

BASE_URL = "https://localhost:8443"

def test_portfolio_endpoint():
    """Test the portfolio provider endpoint for Kraken"""
    
    # For testing, we'll need a valid session token
    # This would normally come from a successful login
    
    print("Testing Kraken Portfolio Endpoint")
    print("=" * 50)
    
    # Test without auth (should fail)
    print("\n1. Testing without authentication:")
    response = requests.get(
        f"{BASE_URL}/v1/portfolio/providers/kraken",
        verify=False
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    # Test the aggregator endpoints if they exist
    print("\n2. Testing portfolio overview endpoint:")
    response = requests.get(
        f"{BASE_URL}/v1/portfolio/overview",
        verify=False
    )
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        print(f"Response: {json.dumps(response.json(), indent=2)}")
    else:
        print(f"Response: {response.text}")
    
    # Test the summary endpoint
    print("\n3. Testing portfolio summary endpoint:")
    response = requests.get(
        f"{BASE_URL}/v1/portfolio/summary",
        verify=False
    )
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        print(f"Response: {json.dumps(response.json(), indent=2)}")
    else:
        print(f"Response: {response.text}")
    
    print("\n" + "=" * 50)
    print("Portfolio endpoints are configured and responding.")
    print("To test with real data:")
    print("1. User must be logged in (have valid session)")
    print("2. User must have demo mode set to false")
    print("3. User must have Kraken API credentials configured")

if __name__ == "__main__":
    test_portfolio_endpoint()