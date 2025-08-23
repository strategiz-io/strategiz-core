#!/usr/bin/env python3
"""
Test script for Alpaca OAuth integration
This script helps you register an OAuth app with Alpaca and configure it properly
"""

import json
import sys
import subprocess
import time
from urllib.parse import urlencode

def check_vault():
    """Check if Vault is running and accessible"""
    try:
        result = subprocess.run(
            ["vault", "status"],
            env={"VAULT_ADDR": "http://localhost:8200"},
            capture_output=True,
            text=True
        )
        return result.returncode == 0
    except:
        return False

def get_current_alpaca_config():
    """Get current Alpaca OAuth configuration from Vault"""
    try:
        result = subprocess.run(
            ["vault", "kv", "get", "-format=json", "secret/strategiz/oauth/alpaca"],
            env={"VAULT_ADDR": "http://localhost:8200", "VAULT_TOKEN": "root"},
            capture_output=True,
            text=True
        )
        if result.returncode == 0:
            data = json.loads(result.stdout)
            return data["data"]["data"]
        return None
    except:
        return None

def update_alpaca_credentials(client_id, client_secret):
    """Update Alpaca OAuth credentials in Vault"""
    try:
        cmd = [
            "vault", "kv", "put", "secret/strategiz/oauth/alpaca",
            f"client-id={client_id}",
            f"client-secret={client_secret}",
            "redirect-uri=http://localhost:3000/auth/providers/alpaca/callback",
            "auth-url=https://app.alpaca.markets/oauth/authorize",
            "token-url=https://api.alpaca.markets/oauth/token",
            "api-url=https://api.alpaca.markets",
            "scope=account:read trading:write data:read"
        ]
        
        result = subprocess.run(
            cmd,
            env={"VAULT_ADDR": "http://localhost:8200", "VAULT_TOKEN": "root"},
            capture_output=True,
            text=True
        )
        return result.returncode == 0
    except Exception as e:
        print(f"Error updating Vault: {e}")
        return False

def generate_test_url(client_id):
    """Generate a test OAuth URL for Alpaca"""
    params = {
        "client_id": client_id,
        "redirect_uri": "http://localhost:3000/auth/providers/alpaca/callback",
        "state": "test-state-123",
        "scope": "account:read trading:write data:read",
        "response_type": "code"
    }
    base_url = "https://app.alpaca.markets/oauth/authorize"
    return f"{base_url}?{urlencode(params)}"

def main():
    print("=" * 60)
    print("Alpaca OAuth Integration Setup & Test")
    print("=" * 60)
    print()
    
    # Check Vault
    if not check_vault():
        print("❌ Vault is not running!")
        print("Please start Vault with: vault server -dev")
        print("Then export the token: export VAULT_TOKEN=<your-token>")
        sys.exit(1)
    
    print("✅ Vault is running")
    print()
    
    # Check current configuration
    current_config = get_current_alpaca_config()
    if current_config:
        print("Current Alpaca OAuth configuration in Vault:")
        print(f"  Client ID: {current_config.get('client-id', 'Not set')}")
        print(f"  Client Secret: {'***' if current_config.get('client-secret') else 'Not set'}")
        print(f"  Redirect URI: {current_config.get('redirect-uri', 'Not set')}")
        print()
        
        if current_config.get('client-id') == 'your-alpaca-client-id':
            print("⚠️  You're using placeholder credentials!")
            print()
    
    print("To set up Alpaca OAuth, you need to:")
    print()
    print("1. Go to: https://app.alpaca.markets/")
    print("2. Sign in or create an account")
    print("3. Navigate to: Apps & API Keys")
    print("4. Click 'Generate OAuth Client'")
    print("5. Fill in the application details:")
    print("   - App Name: Strategiz Trading Platform")
    print("   - App Description: Trading platform integration")
    print("   - Redirect URI: http://localhost:3000/auth/providers/alpaca/callback")
    print("   - Scopes: Select 'account:read', 'trading:write', 'data:read'")
    print("6. Click 'Generate' to get your Client ID and Client Secret")
    print()
    
    update = input("Do you have your Alpaca OAuth credentials? (y/n): ").lower()
    if update == 'y':
        print()
        client_id = input("Enter your Alpaca Client ID: ").strip()
        client_secret = input("Enter your Alpaca Client Secret: ").strip()
        
        if not client_id or not client_secret:
            print("❌ Client ID and Client Secret are required!")
            sys.exit(1)
        
        print()
        print("Updating Vault with new credentials...")
        if update_alpaca_credentials(client_id, client_secret):
            print("✅ Successfully updated Alpaca OAuth credentials!")
            print()
            print("Test OAuth URL:")
            print(generate_test_url(client_id))
            print()
            print("Next steps:")
            print("1. Restart the backend application")
            print("2. Go to the UI and try connecting Alpaca")
            print("3. You should be redirected to Alpaca to authorize")
        else:
            print("❌ Failed to update credentials in Vault")
            sys.exit(1)
    else:
        print()
        print("Please get your credentials from Alpaca first.")
        print("Once you have them, run this script again.")
        print()
        print("NOTE: Alpaca OAuth requires a real Client ID and Secret.")
        print("The placeholder values won't work with Alpaca's OAuth server.")

if __name__ == "__main__":
    main()