#!/usr/bin/env python3

import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime
import os

# Set credentials
os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = './application/target/classes/firebase-service-account.json'

# Initialize Firebase Admin SDK
firebase_admin.initialize_app()

# Get Firestore client
db = firestore.client()

# User IDs to clean
user_ids = [
    "fff74730-4a58-45fe-be74-cf38a57dcb0b",  # Your main user
]

# Symbol mappings from Kraken to standard
symbol_mappings = {
    "PEPE": "PEPE",  # Keep PEPE as is but fix the value calculation
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
    "DYM.F": "DYM",
    "GALA": "GALA",
    "GAL": "GALA",  # Possible variation
}

# Real market prices (approximate)
market_prices = {
    "PEPE": 0.000001,  # PEPE is worth much less than $1
    "DOGE": 0.06,
    "BTC": 45000,
    "ETH": 2300,
    "TRX": 0.10,
    "GALA": 0.015,
    "MATIC": 0.50,
    # Add more as needed
}

def clean_portfolio_data(user_id):
    """Clean portfolio data for a specific user"""
    print(f"\nProcessing user: {user_id}")
    
    # Get provider_data collection
    provider_data_ref = db.collection('users').document(user_id).collection('provider_data').document('kraken')
    provider_doc = provider_data_ref.get()
    
    if not provider_doc.exists:
        print(f"  No Kraken provider data found")
        return
    
    data = provider_doc.to_dict()
    print(f"  Found Kraken data with {len(data.get('holdings', []))} holdings")
    
    if 'holdings' in data:
        holdings = data['holdings']
        cleaned_holdings = []
        total_value = 0
        
        for holding in holdings:
            asset = holding.get('asset', '')
            name = holding.get('name', '')
            quantity = float(holding.get('quantity', 0))
            
            # Apply symbol mapping
            original_asset = asset
            if asset in symbol_mappings:
                asset = symbol_mappings[asset]
                name = asset
            elif name in symbol_mappings:
                asset = symbol_mappings[name]
                name = asset
            
            # Skip tiny holdings
            if quantity < 0.00001:
                continue
            
            # Calculate real value based on market prices
            if asset in market_prices:
                price = market_prices[asset]
                value = quantity * price
            else:
                # For unknown assets, use a conservative estimate
                price = 0.01
                value = quantity * price
            
            # Update holding data
            holding['asset'] = asset
            holding['name'] = asset
            holding['currentPrice'] = str(price)
            holding['currentValue'] = str(value)
            holding['quantity'] = str(quantity)
            
            if original_asset != asset:
                print(f"    Mapped {original_asset} → {asset}: {quantity:.4f} @ ${price:.6f} = ${value:.2f}")
            else:
                print(f"    {asset}: {quantity:.4f} @ ${price:.6f} = ${value:.2f}")
            
            cleaned_holdings.append(holding)
            total_value += value
        
        # Update the data
        data['holdings'] = cleaned_holdings
        data['totalValue'] = total_value
        data['lastCleanup'] = datetime.now().isoformat()
        data['enhancementApplied'] = True
        
        # Write back to Firestore
        provider_data_ref.update(data)
        print(f"  ✅ Updated portfolio with {len(cleaned_holdings)} holdings")
        print(f"  💰 Total portfolio value: ${total_value:,.2f}")
    else:
        print(f"  No holdings found")

def main():
    print("🧹 Cleaning Kraken portfolio data in Firebase...")
    
    for user_id in user_ids:
        try:
            clean_portfolio_data(user_id)
        except Exception as e:
            print(f"  ❌ Error processing user {user_id}: {e}")
    
    print("\n✅ Cleanup complete!")
    print("Refresh your portfolio page to see the corrected data.")

if __name__ == "__main__":
    main()