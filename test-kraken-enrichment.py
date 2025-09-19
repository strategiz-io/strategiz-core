#!/usr/bin/env python3
"""
Test script to verify Kraken data enrichment in Firestore
"""

import firebase_admin
from firebase_admin import credentials, firestore
import json
from datetime import datetime

# Initialize Firebase
cred = credentials.Certificate('application/target/classes/firebase-service-account.json')
firebase_admin.initialize_app(cred)
db = firestore.client()

def check_provider_data(user_id):
    """Check provider_data subcollection for Kraken data"""
    print(f"\n📊 Checking provider_data for user: {user_id}")
    print("=" * 80)
    
    try:
        # Get the provider_data document for Kraken
        doc_ref = db.collection('users').document(user_id).collection('provider_data').document('kraken')
        doc = doc_ref.get()
        
        if doc.exists:
            data = doc.to_dict()
            print(f"✅ Found Kraken data in provider_data subcollection")
            print(f"   Last Updated: {data.get('lastUpdatedAt', 'Unknown')}")
            print(f"   Total Value: ${data.get('totalValue', 0)}")
            print(f"   Cash Balance: ${data.get('cashBalance', 0)}")
            print(f"   Sync Status: {data.get('syncStatus', 'Unknown')}")
            
            # Check holdings for enrichment
            holdings = data.get('holdings', [])
            if holdings:
                print(f"\n📈 Holdings ({len(holdings)} assets):")
                for holding in holdings[:5]:  # Show first 5
                    asset = holding.get('asset', 'Unknown')
                    name = holding.get('name', 'Unknown')
                    quantity = holding.get('quantity', 0)
                    current_value = holding.get('currentValue', 0)
                    
                    # Check if symbols are normalized (enriched)
                    is_enriched = not asset.startswith('X') or asset in ['XRP', 'XLM', 'XTZ']
                    status = "✅ Enriched" if is_enriched else "❌ Raw"
                    
                    print(f"   {status} {asset:10} {name:30} Qty: {quantity:15.8f} Value: ${current_value:10.2f}")
                
                if len(holdings) > 5:
                    print(f"   ... and {len(holdings) - 5} more assets")
            else:
                print("   ❌ No holdings found")
            
            # Check raw balances
            balances = data.get('balances', {})
            if balances:
                print(f"\n💰 Raw Balances ({len(balances)} assets):")
                for symbol, amount in list(balances.items())[:5]:
                    print(f"   {symbol:10} {amount}")
                if len(balances) > 5:
                    print(f"   ... and {len(balances) - 5} more balances")
                    
            # Check if data has enrichment markers
            print("\n🔍 Enrichment Indicators:")
            has_names = any(h.get('name') for h in holdings)
            has_normalized = any(not h.get('asset', '').startswith('X') or h.get('asset') in ['XRP', 'XLM', 'XTZ'] for h in holdings)
            has_prices = any(h.get('currentPrice') for h in holdings)
            has_values = any(h.get('currentValue') for h in holdings)
            
            print(f"   Has asset names: {'✅' if has_names else '❌'}")
            print(f"   Has normalized symbols: {'✅' if has_normalized else '❌'}")
            print(f"   Has current prices: {'✅' if has_prices else '❌'}")
            print(f"   Has calculated values: {'✅' if has_values else '❌'}")
            
            overall_enriched = has_names and has_normalized and has_prices and has_values
            print(f"\n   Overall Status: {'✅ DATA IS ENRICHED' if overall_enriched else '❌ DATA IS NOT ENRICHED'}")
            
        else:
            print("❌ No Kraken data found in provider_data subcollection")
            
    except Exception as e:
        print(f"❌ Error accessing provider_data: {e}")

def check_provider_integrations(user_id):
    """Check provider_integrations subcollection"""
    print(f"\n🔗 Checking provider_integrations for user: {user_id}")
    print("=" * 80)
    
    try:
        # Get all provider integrations
        docs = db.collection('users').document(user_id).collection('provider_integrations').stream()
        
        found_kraken = False
        for doc in docs:
            data = doc.to_dict()
            provider_id = data.get('providerId', 'Unknown')
            
            if provider_id == 'kraken':
                found_kraken = True
                print(f"✅ Found Kraken integration")
                print(f"   Status: {data.get('status', 'Unknown')}")
                print(f"   Auth Type: {data.get('authType', 'Unknown')}")
                print(f"   Created: {data.get('createdAt', 'Unknown')}")
                print(f"   Last Updated: {data.get('lastUpdatedAt', 'Unknown')}")
                
        if not found_kraken:
            print("❌ No Kraken integration found")
            
    except Exception as e:
        print(f"❌ Error accessing provider_integrations: {e}")

# Test with a known user ID
test_user_id = "cuztomizer"  # Replace with actual user ID

print("\n🚀 Kraken Data Enrichment Verification")
print("=" * 80)

check_provider_integrations(test_user_id)
check_provider_data(test_user_id)

print("\n" + "=" * 80)
print("📝 Summary:")
print("   - Check if Kraken integration exists in provider_integrations")
print("   - Check if data exists in provider_data subcollection")
print("   - Verify symbols are normalized (BTC not XXBT)")
print("   - Verify assets have names and current prices")
print("   - If data is not enriched, reconnect Kraken account to trigger enrichment")