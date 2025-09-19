#!/usr/bin/env python3
"""
Test script to verify portfolio data retrieval with demo mode off.
Tests the new SOLID-compliant Kraken portfolio controller.
"""

import requests
import json
import sys

# Configuration
BASE_URL = "http://localhost:8080"
API_URL = f"{BASE_URL}/api/v1"

def get_auth_token(email, password):
    """Get authentication token"""
    try:
        response = requests.post(
            f"{API_URL}/auth/login",
            json={"email": email, "password": password}
        )
        if response.status_code == 200:
            data = response.json()
            return data.get("token")
        else:
            print(f"Login failed: {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print(f"Error during login: {e}")
        return None

def check_demo_mode(token):
    """Check current demo mode status"""
    try:
        response = requests.get(
            f"{API_URL}/profile",
            headers={"Authorization": f"Bearer {token}"}
        )
        if response.status_code == 200:
            data = response.json()
            return data.get("profile", {}).get("demoMode", True)
        return True
    except Exception as e:
        print(f"Error checking demo mode: {e}")
        return True

def set_demo_mode(token, demo_mode):
    """Set demo mode on or off"""
    try:
        response = requests.put(
            f"{API_URL}/profile/demo-mode",
            json={"demoMode": demo_mode},
            headers={"Authorization": f"Bearer {token}"}
        )
        if response.status_code == 200:
            print(f"Demo mode set to: {demo_mode}")
            return True
        else:
            print(f"Failed to set demo mode: {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print(f"Error setting demo mode: {e}")
        return False

def test_generic_portfolio_endpoint(token):
    """Test the generic portfolio provider endpoint"""
    print("\n=== Testing Generic Portfolio Provider Endpoint ===")
    try:
        response = requests.get(
            f"{API_URL}/portfolio/providers/kraken",
            headers={"Authorization": f"Bearer {token}"}
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"Provider: {data.get('providerId')}")
            print(f"Connected: {data.get('connected')}")
            print(f"Sync Status: {data.get('syncStatus')}")
            print(f"Total Value: {data.get('totalValue')}")
            print(f"Positions Count: {len(data.get('positions', []))}")
            return True
        else:
            print(f"Response: {response.text}")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_kraken_specific_endpoint(token):
    """Test the Kraken-specific portfolio endpoint"""
    print("\n=== Testing Kraken-Specific Portfolio Endpoint ===")
    try:
        response = requests.get(
            f"{API_URL}/portfolio/providers/kraken",
            headers={"Authorization": f"Bearer {token}"}
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"Provider: {data.get('providerId')}")
            print(f"Provider Name: {data.get('providerName')}")
            print(f"Account Type: {data.get('accountType')}")
            print(f"Connected: {data.get('connected')}")
            print(f"Sync Status: {data.get('syncStatus')}")
            print(f"Total Value: {data.get('totalValue')}")
            print(f"Cash Balance: {data.get('cashBalance')}")
            
            # Show positions
            positions = data.get('positions', [])
            if positions:
                print(f"\nPositions ({len(positions)}):")
                for pos in positions[:5]:  # Show first 5 positions
                    print(f"  - {pos.get('symbol')}: {pos.get('quantity')} @ ${pos.get('currentPrice')}")
            
            # Show balances if available
            balances = data.get('balances', {})
            if balances:
                print(f"\nRaw Balances ({len(balances)} assets):")
                for asset, balance in list(balances.items())[:5]:  # Show first 5 balances
                    print(f"  - {asset}: {balance}")
            
            return True
        else:
            print(f"Response: {response.text}")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_kraken_balances(token):
    """Test the Kraken balances endpoint"""
    print("\n=== Testing Kraken Balances Endpoint ===")
    try:
        response = requests.get(
            f"{API_URL}/portfolio/providers/kraken/balances",
            headers={"Authorization": f"Bearer {token}"}
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            balances = data.get('balances', {})
            print(f"Balances for {len(balances)} assets:")
            for asset, balance in list(balances.items())[:10]:  # Show first 10
                print(f"  - {asset}: {balance}")
            return True
        else:
            print(f"Response: {response.text}")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_portfolio_aggregator(token):
    """Test the portfolio aggregator endpoint"""
    print("\n=== Testing Portfolio Aggregator ===")
    try:
        response = requests.get(
            f"{API_URL}/portfolio/overview",
            headers={"Authorization": f"Bearer {token}"}
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"Total Portfolio Value: ${data.get('totalValue')}")
            print(f"Day Change: ${data.get('dayChange')} ({data.get('dayChangePercent')}%)")
            print(f"Total P&L: ${data.get('totalProfitLoss')} ({data.get('totalProfitLossPercent')}%)")
            print(f"Cash Balance: ${data.get('totalCashBalance')}")
            
            providers = data.get('providers', [])
            print(f"\nConnected Providers ({len(providers)}):")
            for provider in providers:
                print(f"  - {provider.get('providerName')}: ${provider.get('totalValue')} ({provider.get('positionCount')} positions)")
            
            allocation = data.get('assetAllocation', {})
            if allocation:
                print(f"\nAsset Allocation:")
                print(f"  - Crypto: {allocation.get('cryptoPercent')}%")
                print(f"  - Stocks: {allocation.get('stockPercent')}%")
                print(f"  - Cash: {allocation.get('cashPercent')}%")
            
            return True
        else:
            print(f"Response: {response.text}")
            return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def main():
    print("Portfolio Real Data Test Script")
    print("================================")
    
    # Get credentials
    email = input("Enter email: ").strip()
    password = input("Enter password: ").strip()
    
    # Login
    print("\nLogging in...")
    token = get_auth_token(email, password)
    if not token:
        print("Failed to get auth token")
        sys.exit(1)
    
    print("Successfully authenticated")
    
    # Check current demo mode
    print("\nChecking demo mode status...")
    is_demo = check_demo_mode(token)
    print(f"Current demo mode: {is_demo}")
    
    if is_demo:
        print("\nDisabling demo mode to fetch real data...")
        if not set_demo_mode(token, False):
            print("Failed to disable demo mode")
            sys.exit(1)
    
    # Run tests
    print("\n" + "="*50)
    print("TESTING PORTFOLIO ENDPOINTS WITH REAL DATA")
    print("="*50)
    
    # Test generic endpoint
    test_generic_portfolio_endpoint(token)
    
    # Test Kraken-specific endpoint
    test_kraken_specific_endpoint(token)
    
    # Test Kraken balances
    test_kraken_balances(token)
    
    # Test portfolio aggregator
    test_portfolio_aggregator(token)
    
    # Restore demo mode if it was on
    if is_demo:
        print("\n\nRestoring demo mode...")
        set_demo_mode(token, True)
    
    print("\n" + "="*50)
    print("Test completed!")

if __name__ == "__main__":
    main()