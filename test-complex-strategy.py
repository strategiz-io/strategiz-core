#!/usr/bin/env python3
"""
Test Complex Strategy - Multiple Indicators
Tests: RSI, SMA, EMA, MACD, Bollinger Bands on 100 bars
"""

import grpc
import sys
import time
from datetime import datetime, timedelta

sys.path.insert(0, 'application-strategy-execution/strategiz_execution/generated')
import strategy_execution_pb2
import strategy_execution_pb2_grpc


def test_complex_strategy():
    """Test complex multi-indicator strategy"""
    print("\n🔍 Testing Complex Multi-Indicator Strategy...")
    print("   Indicators: RSI, SMA(20,50), EMA(12,26), MACD, Bollinger Bands")
    print("   Data: 100 bars of price data\n")

    # Create 100 bars of market data
    base_time = datetime(2024, 12, 1)
    market_data = []
    price = 180.0

    for i in range(100):
        price += (i % 7 - 3) * 1.5  # More complex price movement
        bar = strategy_execution_pb2.MarketDataBar(
            timestamp=(base_time + timedelta(days=i)).isoformat() + 'Z',
            open=price - 0.5,
            high=price + 1.5,
            low=price - 1.5,
            close=price,
            volume=1000000 + (i * 50000)
        )
        market_data.append(bar)

    # Complex strategy with multiple indicators
    code = """
# Complex multi-indicator strategy
# Uses RSI, SMA, EMA, MACD, Bollinger Bands

def calculate_rsi(prices, period=14):
    delta = prices.diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss
    return 100 - (100 / (1 + rs))

def calculate_macd(prices, fast=12, slow=26, signal=9):
    ema_fast = prices.ewm(span=fast).mean()
    ema_slow = prices.ewm(span=slow).mean()
    macd_line = ema_fast - ema_slow
    signal_line = macd_line.ewm(span=signal).mean()
    return macd_line, signal_line

def calculate_bollinger_bands(prices, period=20, std_dev=2):
    sma = prices.rolling(window=period).mean()
    rolling_std = prices.rolling(window=period).std()
    upper_band = sma + (rolling_std * std_dev)
    lower_band = sma - (rolling_std * std_dev)
    return upper_band, sma, lower_band

def strategy(data):
    close = data['close']

    # Calculate all indicators
    rsi = calculate_rsi(close, 14)
    sma_20 = close.rolling(window=20).mean()
    sma_50 = close.rolling(window=50).mean()
    ema_12 = close.ewm(span=12).mean()
    ema_26 = close.ewm(span=26).mean()
    macd, signal = calculate_macd(close)
    bb_upper, bb_mid, bb_lower = calculate_bollinger_bands(close)

    # Multi-condition trading logic
    latest_idx = -1

    # BUY conditions: RSI oversold + price below lower BB + SMA cross
    buy_signal = (
        rsi.iloc[latest_idx] < 30 and
        close.iloc[latest_idx] < bb_lower.iloc[latest_idx] and
        sma_20.iloc[latest_idx] > sma_50.iloc[latest_idx]
    )

    # SELL conditions: RSI overbought + price above upper BB
    sell_signal = (
        rsi.iloc[latest_idx] > 70 and
        close.iloc[latest_idx] > bb_upper.iloc[latest_idx]
    )

    if buy_signal:
        return 'BUY'
    elif sell_signal:
        return 'SELL'
    return 'HOLD'
"""

    channel = grpc.secure_channel(
        'strategiz-execution-43628135674.us-east1.run.app:443',
        grpc.ssl_channel_credentials()
    )
    stub = strategy_execution_pb2_grpc.StrategyExecutionServiceStub(channel)

    try:
        # Run 3 times to test caching
        times = []
        for run in range(3):
            start = time.time()

            request = strategy_execution_pb2.ExecuteStrategyRequest(
                strategy_id=f'complex-test-{run}',
                user_id='benchmark-user',
                code=code,
                language='python',
                market_data=market_data,
                timeout_seconds=30
            )

            response = stub.ExecuteStrategy(request, timeout=35)
            total_time = int((time.time() - start) * 1000)
            times.append(total_time)

            if response.success:
                status = "✅" if response.execution_time_ms < 100 else "⚠️"
                cache_status = "CACHED" if run > 0 else "FIRST"
                print(f'{status} Run {run + 1} ({cache_status}):')
                print(f'   Server: {response.execution_time_ms}ms | Total: {total_time}ms')
            else:
                print(f'❌ Run {run + 1} failed: {response.error}')

        # Summary
        avg_server = response.execution_time_ms if response.success else 0
        print(f'\n📊 Results:')
        print(f'   Best server time: {avg_server}ms')
        print(f'   Target: <100ms')
        print(f'   Status: {"✅ PASS" if avg_server < 100 else "❌ NEEDS OPTIMIZATION"}')

        return response.success and avg_server < 100

    except Exception as e:
        print(f'❌ Test failed: {e}')
        import traceback
        traceback.print_exc()
        return False
    finally:
        channel.close()


if __name__ == '__main__':
    success = test_complex_strategy()
    sys.exit(0 if success else 1)
