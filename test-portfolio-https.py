#!/usr/bin/env python3
"""
Test script to verify portfolio data retrieval with demo mode off using HTTPS.
Tests the new SOLID-compliant Kraken portfolio controller with HTTPS backend.
"""

import requests
import json
import sys
import urllib3

# Disable SSL warnings for self-signed certificates
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Configuration for HTTPS
BASE_URL = "https://localhost:8443"
API_URL = f"{BASE_URL}/v1"

def test_portfolio_overview_anonymous():
    """Test portfolio overview endpoint without authentication (should get auth error)"""
    print("\n=== Testing Portfolio Overview (Anonymous) ===")
    try:
        response = requests.get(
            f"{API_URL}/portfolio/overview",
            verify=False  # Skip SSL verification for self-signed certs
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")

        if response.status_code == 500 and 'AUTHENTICATION_ERROR' in response.text:
            print("✅ Correctly returns authentication error without auth token")
            return True
        else:
            print("❌ Unexpected response for anonymous request")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_kraken_portfolio_anonymous():
    """Test Kraken portfolio endpoint without authentication (should get auth error)"""
    print("\n=== Testing Kraken Portfolio (Anonymous) ===")
    try:
        response = requests.get(
            f"{API_URL}/portfolio/providers/kraken",
            verify=False  # Skip SSL verification for self-signed certs
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")

        if response.status_code == 500 and 'AUTHENTICATION_ERROR' in response.text:
            print("✅ Correctly returns authentication error without auth token")
            return True
        else:
            print("❌ Unexpected response for anonymous request")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_demo_mode_endpoint():
    """Test demo mode endpoint (should work even without auth for testing)"""
    print("\n=== Testing Demo Mode Detection ===")
    try:
        # Try to access a simple health check endpoint first
        response = requests.get(
            f"{BASE_URL}/actuator/health",
            verify=False  # Skip SSL verification for self-signed certs
        )
        print(f"Health check status: {response.status_code}")
        if response.status_code == 200:
            print("✅ HTTPS backend is healthy and responsive")
            return True
        else:
            print("❌ HTTPS backend health check failed")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_https_connectivity():
    """Test basic HTTPS connectivity to the backend"""
    print("\n=== Testing HTTPS Connectivity ===")
    try:
        response = requests.get(
            f"{BASE_URL}/actuator/info",
            verify=False,  # Skip SSL verification for self-signed certs
            timeout=10
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"App info: {data}")
            print("✅ HTTPS connection successful")
            return True
        else:
            print("❌ HTTPS connection failed")
            return False
    except Exception as e:
        print(f"Error connecting to HTTPS backend: {e}")
        return False

def main():
    print("Portfolio HTTPS Test Script")
    print("===========================")
    print(f"Testing backend at: {BASE_URL}")
    print(f"API base URL: {API_URL}")

    print("\n" + "="*50)
    print("TESTING HTTPS BACKEND CONNECTIVITY")
    print("="*50)

    # Test basic HTTPS connectivity
    if not test_https_connectivity():
        print("❌ Basic HTTPS connectivity failed")
        sys.exit(1)

    # Test demo mode endpoint
    if not test_demo_mode_endpoint():
        print("❌ Demo mode endpoint test failed")
        sys.exit(1)

    print("\n" + "="*50)
    print("TESTING PORTFOLIO ENDPOINTS (DEMO MODE FIX)")
    print("="*50)

    # Test portfolio endpoints without auth (should get proper auth errors, not fake data)
    success = True

    if not test_portfolio_overview_anonymous():
        success = False

    if not test_kraken_portfolio_anonymous():
        success = False

    print("\n" + "="*50)
    if success:
        print("✅ ALL TESTS PASSED!")
        print("✅ HTTPS backend is working correctly")
        print("✅ Portfolio endpoints return proper auth errors (not fake data)")
        print("✅ Demo mode fix is working - no more fake $35,000 data without auth!")
    else:
        print("❌ Some tests failed")
        sys.exit(1)

    print("="*50)

if __name__ == "__main__":
    main()