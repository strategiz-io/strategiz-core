#!/usr/bin/env python3

import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime

# Initialize Firebase Admin SDK
firebase_admin.initialize_app()

# Get Firestore client
db = firestore.client()

# User IDs to clean
user_ids = [
    "fff74730-4a58-45fe-be74-cf38a57dcb0b",  # Your main user
    "edda816d-05a4-491c-a8c1-b49a0f5ee274"   # New user from logs
]

# Symbol mappings from Kraken to standard
symbol_mappings = {
    "XXDG": "DOGE",
    "XDG": "DOGE",
    "XXBT": "BTC",
    "XBT": "BTC",
    "XETH": "ETH",
    "ETH.F": "ETH",
    "TRX.F": "TRX",
    "POL.F": "MATIC",
    "DOT.F": "DOT",
    "ADA.F": "ADA",
    "ATOM.F": "ATOM",
    "KSM.F": "KSM",
    "SOL.F": "SOL",
    "INJ.F": "INJ",
    "SUI.F": "SUI",
    "SEI.F": "SEI",
    "TIA.F": "TIA",
    "DYM.F": "DYM"
}

def clean_portfolio_data(user_id):
    """Clean portfolio data for a specific user"""
    print(f"\nProcessing user: {user_id}")
    
    # Try different possible paths for provider data
    paths_to_check = [
        ('users', user_id, 'providers', 'kraken'),
        ('users', user_id, 'provider_integrations', 'kraken'),
        ('provider_integrations', f"{user_id}_kraken"),
        ('portfolios', user_id, 'providers', 'kraken'),
    ]
    
    provider_doc = None
    provider_ref = None
    
    for path in paths_to_check:
        try:
            if len(path) == 2:
                ref = db.collection(path[0]).document(path[1])
            elif len(path) == 4:
                ref = db.collection(path[0]).document(path[1]).collection(path[2]).document(path[3])
            
            doc = ref.get()
            if doc.exists:
                print(f"  Found data at path: {'/'.join(path)}")
                provider_doc = doc
                provider_ref = ref
                break
        except Exception as e:
            print(f"  Error checking path {'/'.join(path)}: {e}")
    
    if not provider_doc:
        print(f"  No Kraken provider data found for user {user_id}")
        return
    
    data = provider_doc.to_dict()
    print(f"  Found Kraken data, last updated: {data.get('lastSyncTime', 'unknown')}")
    
    if 'portfolioData' in data and 'holdings' in data['portfolioData']:
        holdings = data['portfolioData']['holdings']
        cleaned_holdings = []
        
        for holding in holdings:
            symbol = holding.get('symbol', '')
            
            # Apply symbol mapping
            if symbol in symbol_mappings:
                print(f"    Mapping {symbol} → {symbol_mappings[symbol]}")
                holding['symbol'] = symbol_mappings[symbol]
                holding['originalSymbol'] = symbol  # Keep original for reference
            
            # Only include holdings with meaningful balance
            if holding.get('quantity', 0) > 0.00001:
                cleaned_holdings.append(holding)
        
        # Update the data
        data['portfolioData']['holdings'] = cleaned_holdings
        data['lastCleanup'] = datetime.now().isoformat()
        data['enhancementApplied'] = True
        
        # Write back to Firestore
        provider_ref.update(data)
        print(f"  ✅ Updated portfolio with {len(cleaned_holdings)} holdings")
    else:
        print(f"  No portfolio holdings found")

def main():
    print("🧹 Cleaning Kraken portfolio data in Firebase...")
    
    for user_id in user_ids:
        try:
            clean_portfolio_data(user_id)
        except Exception as e:
            print(f"  ❌ Error processing user {user_id}: {e}")
    
    print("\n✅ Cleanup complete!")
    print("Refresh your portfolio page to see the corrected symbols.")

if __name__ == "__main__":
    main()