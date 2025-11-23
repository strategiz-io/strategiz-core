#!/usr/bin/env python3
"""
Populate Test Strategies in Firestore
Creates sample trading strategies for testing alert deployment functionality
"""

import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime
import uuid
import sys
import os

# Path to Firebase service account credentials
CRED_PATH = os.path.join(os.path.dirname(__file__), '..', 'application', 'src', 'main', 'resources', 'firebase-service-account.json')

# Sample strategies with realistic data
SAMPLE_STRATEGIES = [
    {
        "name": "RSI Oversold Reversal",
        "description": "Identifies oversold conditions using RSI indicator and triggers buy signals when RSI crosses below 30",
        "language": "python",
        "type": "technical",
        "status": "active",
        "tags": ["momentum", "rsi", "reversal"],
        "code": """
# RSI Oversold Reversal Strategy
def generate_signal(data):
    rsi = calculate_rsi(data['close'], period=14)
    if rsi < 30:
        return 'BUY'
    elif rsi > 70:
        return 'SELL'
    return 'HOLD'
""",
        "parameters": {
            "rsi_period": 14,
            "oversold_threshold": 30,
            "overbought_threshold": 70
        },
        "performance": {
            "totalReturn": 23.5,
            "winRate": 65.2,
            "sharpeRatio": 1.8,
            "maxDrawdown": -12.3
        },
        "backtestResults": {
            "totalTrades": 156,
            "winningTrades": 102,
            "losingTrades": 54,
            "avgWin": 2.3,
            "avgLoss": -1.1
        },
        "isPublic": False
    },
    {
        "name": "MACD Crossover",
        "description": "Classic MACD strategy that generates signals when MACD line crosses the signal line",
        "language": "python",
        "type": "technical",
        "status": "active",
        "tags": ["trend-following", "macd", "crossover"],
        "code": """
# MACD Crossover Strategy
def generate_signal(data):
    macd, signal = calculate_macd(data['close'])
    if macd > signal and prev_macd <= prev_signal:
        return 'BUY'
    elif macd < signal and prev_macd >= prev_signal:
        return 'SELL'
    return 'HOLD'
""",
        "parameters": {
            "fast_period": 12,
            "slow_period": 26,
            "signal_period": 9
        },
        "performance": {
            "totalReturn": 31.2,
            "winRate": 58.7,
            "sharpeRatio": 2.1,
            "maxDrawdown": -15.8
        },
        "backtestResults": {
            "totalTrades": 203,
            "winningTrades": 119,
            "losingTrades": 84,
            "avgWin": 2.8,
            "avgLoss": -1.5
        },
        "isPublic": False
    },
    {
        "name": "Bollinger Bands Breakout",
        "description": "Volatility-based strategy that triggers on price breakouts above or below Bollinger Bands",
        "language": "python",
        "type": "technical",
        "status": "active",
        "tags": ["volatility", "bollinger-bands", "breakout"],
        "code": """
# Bollinger Bands Breakout Strategy
def generate_signal(data):
    upper, middle, lower = calculate_bollinger_bands(data['close'], period=20, std_dev=2)
    price = data['close'][-1]
    if price > upper:
        return 'SELL'  # Overbought
    elif price < lower:
        return 'BUY'   # Oversold
    return 'HOLD'
""",
        "parameters": {
            "period": 20,
            "std_dev_multiplier": 2.0
        },
        "performance": {
            "totalReturn": 18.9,
            "winRate": 61.3,
            "sharpeRatio": 1.6,
            "maxDrawdown": -9.7
        },
        "backtestResults": {
            "totalTrades": 142,
            "winningTrades": 87,
            "losingTrades": 55,
            "avgWin": 2.1,
            "avgLoss": -1.3
        },
        "isPublic": False
    }
]

def init_firebase():
    """Initialize Firebase Admin SDK"""
    if not os.path.exists(CRED_PATH):
        print(f"Error: Firebase credentials not found at {CRED_PATH}")
        print("Please ensure firebase-service-account.json exists in application/src/main/resources/")
        sys.exit(1)

    try:
        cred = credentials.Certificate(CRED_PATH)
        firebase_admin.initialize_app(cred)
        print(" Firebase Admin SDK initialized successfully")
    except Exception as e:
        print(f"Error initializing Firebase: {e}")
        sys.exit(1)

def populate_strategies(user_id="test-user-123"):
    """
    Populate Firestore with sample strategies

    Args:
        user_id: The user ID to associate with the strategies (default: test-user-123)
    """
    db = firestore.client()

    print(f"\n Populating {len(SAMPLE_STRATEGIES)} test strategies...")
    print(f" User ID: {user_id}\n")

    created_ids = []

    for idx, strategy in enumerate(SAMPLE_STRATEGIES, 1):
        # Generate a unique ID for the strategy
        strategy_id = str(uuid.uuid4())

        # Add required audit fields
        strategy_data = {
            **strategy,
            "id": strategy_id,
            "userId": user_id,
            "createdAt": firestore.SERVER_TIMESTAMP,
            "modifiedAt": firestore.SERVER_TIMESTAMP,
            "version": 0,
            "isActive": True
        }

        # Create the document
        try:
            db.collection('strategies').document(strategy_id).set(strategy_data)
            print(f" [{idx}/{len(SAMPLE_STRATEGIES)}] Created: {strategy['name']}")
            print(f"     ID: {strategy_id}")
            print(f"     Type: {strategy['type']} | Language: {strategy['language']}")
            print(f"     Performance: {strategy['performance']['totalReturn']}% return, "
                  f"{strategy['performance']['winRate']}% win rate\n")
            created_ids.append(strategy_id)
        except Exception as e:
            print(f" Error creating strategy '{strategy['name']}': {e}")

    print(f"\n Strategy IDs created:")
    for strategy_id in created_ids:
        print(f"   - {strategy_id}")

    print(f"\n Success! {len(created_ids)} strategies populated in Firestore")
    print(f" Navigate to http://localhost:3000/labs to see them in action!")

def main():
    """Main entry point"""
    print("=" * 80)
    print(" STRATEGIZ - Test Data Population Script")
    print("=" * 80)

    # Get user ID from command line argument or use default
    user_id = sys.argv[1] if len(sys.argv) > 1 else "test-user-123"

    try:
        # Initialize Firebase
        init_firebase()

        # Populate strategies
        populate_strategies(user_id)

    except KeyboardInterrupt:
        print("\n\n Interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
