#!/usr/bin/env python3
"""
Test script to verify Coinbase integration for test1@gmail.com
"""

import requests
import json
import time

BASE_URL = "http://localhost:8080"

def login_user(email, name):
    """Login or create user and get auth token"""
    # Step 1: Create/get user profile
    profile_response = requests.post(
        f"{BASE_URL}/v1/signup/profile",
        json={"email": email, "name": name}
    )
    
    if profile_response.status_code not in [200, 201]:
        print(f"Failed to create/get profile: {profile_response.text}")
        return None
    
    profile_data = profile_response.json()
    print(f"Profile created/retrieved: {profile_data}")
    
    # Extract the identity token
    identity_token = profile_data.get('identityToken')
    if not identity_token:
        print("No identity token received")
        return None
    
    return identity_token

def check_coinbase_connection(token):
    """Check if Coinbase is connected"""
    response = requests.get(
        f"{BASE_URL}/v1/portfolios/providers/coinbase/connection",
        headers={"Authorization": f"Bearer {token}"}
    )
    
    print("\n=== Coinbase Connection Status ===")
    print(json.dumps(response.json(), indent=2))
    return response.json()

def fetch_coinbase_accounts(token):
    """Fetch Coinbase account data"""
    response = requests.get(
        f"{BASE_URL}/v1/portfolios/providers/coinbase/accounts",
        headers={"Authorization": f"Bearer {token}"}
    )
    
    print("\n=== Coinbase Accounts ===")
    data = response.json()
    
    if data.get('success'):
        accounts = data.get('accounts', [])
        summary = data.get('summary', {})
        
        print(f"Total accounts: {len(accounts)}")
        print(f"Total value: {summary.get('totalValueFormatted', 'N/A')}")
        
        for account in accounts[:5]:  # Show first 5 accounts
            print(f"\nAccount: {account.get('name', 'Unknown')}")
            print(f"  Currency: {account.get('currency', 'N/A')}")
            print(f"  Balance: {account.get('balance', 'N/A')}")
            
            native_balance = account.get('native_balance', {})
            if native_balance:
                print(f"  USD Value: ${native_balance.get('amount', 'N/A')}")
    else:
        print(f"Failed to fetch accounts: {data.get('message', 'Unknown error')}")
    
    return data

def fetch_coinbase_holdings(token):
    """Fetch Coinbase holdings with P&L"""
    response = requests.get(
        f"{BASE_URL}/v1/portfolios/providers/coinbase/holdings",
        headers={"Authorization": f"Bearer {token}"}
    )
    
    print("\n=== Coinbase Holdings with P&L ===")
    data = response.json()
    
    if data.get('success'):
        holdings = data.get('holdings', [])
        summary = data.get('summary', {})
        
        print(f"Total holdings: {len(holdings)}")
        print(f"Total value: ${summary.get('totalValue', 'N/A')}")
        print(f"Total P&L: ${summary.get('totalProfitLoss', 'N/A')} ({summary.get('totalProfitLossPercent', 'N/A')}%)")
        
        for holding in holdings[:5]:  # Show first 5 holdings
            print(f"\nCurrency: {holding.get('currency', 'Unknown')}")
            print(f"  Current Value: ${holding.get('currentValue', 'N/A')}")
            print(f"  Cost Basis: ${holding.get('costBasis', 'N/A')}")
            print(f"  P&L: ${holding.get('profitLoss', 'N/A')} ({holding.get('profitLossPercent', 'N/A')}%)")
    else:
        print(f"Failed to fetch holdings: {data.get('message', 'Unknown error')}")
    
    return data

def main():
    print("Testing Coinbase integration for test1@gmail.com...")
    
    # Login as test1
    token = login_user("test1@gmail.com", "test1")
    
    if not token:
        print("Failed to get auth token")
        return
    
    print(f"\nAuth token obtained: {token[:20]}...")
    
    # Check Coinbase connection
    connection_data = check_coinbase_connection(token)
    
    if connection_data.get('success'):
        # Fetch account data
        fetch_coinbase_accounts(token)
        
        # Fetch holdings with P&L
        fetch_coinbase_holdings(token)
    else:
        print("\nCoinbase not connected. Please connect your Coinbase account through the UI.")
        print("Go to: http://localhost:3000")
        print("1. Login as test1@gmail.com")
        print("2. Go to Settings -> Connected Accounts")
        print("3. Connect Coinbase")

if __name__ == "__main__":
    main()