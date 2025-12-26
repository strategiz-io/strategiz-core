#!/usr/bin/env python3
"""
Test Script for Strategy Execution Service
Tests health check and basic strategy execution
"""

import grpc
import sys
import time
from datetime import datetime, timedelta

# Import generated protobuf code
sys.path.insert(0, 'application-strategy-execution/strategiz_execution/generated')
import strategy_execution_pb2
import strategy_execution_pb2_grpc


def test_health():
    """Test health endpoint"""
    print("\n🔍 Testing Health Endpoint...")

    channel = grpc.secure_channel(
        'strategiz-execution-43628135674.us-east1.run.app:443',
        grpc.ssl_channel_credentials()
    )
    stub = strategy_execution_pb2_grpc.StrategyExecutionServiceStub(channel)

    try:
        response = stub.GetHealth(strategy_execution_pb2.HealthRequest(), timeout=10)
        print(f'✅ Service Status: {response.status}')
        print(f'   Supported Languages: {list(response.supported_languages)}')
        print(f'   Max Timeout: {response.max_timeout_seconds}s')
        print(f'   Max Memory: {response.max_memory_mb}MB')
        print(f'   Metadata: {dict(response.metadata)}')
        return True
    except Exception as e:
        print(f'❌ Health check failed: {e}')
        return False
    finally:
        channel.close()


def test_simple_strategy():
    """Test simple RSI strategy execution"""
    print("\n🔍 Testing Simple Strategy Execution...")

    # Create sample market data (10 days of AAPL)
    base_time = datetime(2024, 12, 1)
    market_data = []
    price = 180.0

    for i in range(30):
        # Simulate price movement
        price += (i % 5 - 2) * 2  # Simple price oscillation

        bar = strategy_execution_pb2.MarketDataBar(
            timestamp=(base_time + timedelta(days=i)).isoformat() + 'Z',
            open=price - 1,
            high=price + 2,
            low=price - 2,
            close=price,
            volume=1000000 + (i * 10000)
        )
        market_data.append(bar)

    # Simple RSI strategy code
    # Note: pd, pandas, ta, etc. are already available in the execution environment
    code = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02  # 2%
TAKE_PROFIT = 0.05  # 5%
POSITION_SIZE = 1

def calculate_rsi(data, period=14):
    delta = data['close'].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss
    return 100 - (100 / (1 + rs))

def strategy(data):
    # pd is already available in the execution environment
    df = pd.DataFrame(data)
    df['rsi'] = calculate_rsi(df)

    # Buy when RSI < 30 (oversold)
    # Sell when RSI > 70 (overbought)
    if df['rsi'].iloc[-1] < 30:
        return 'BUY'
    elif df['rsi'].iloc[-1] > 70:
        return 'SELL'
    return 'HOLD'
"""

    channel = grpc.secure_channel(
        'strategiz-execution-43628135674.us-east1.run.app:443',
        grpc.ssl_channel_credentials()
    )
    stub = strategy_execution_pb2_grpc.StrategyExecutionServiceStub(channel)

    try:
        start = time.time()

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            strategy_id='test-rsi-001',
            user_id='test-user',
            code=code,
            language='python',
            market_data=market_data,
            timeout_seconds=30
        )

        response = stub.ExecuteStrategy(request, timeout=35)

        execution_time = int((time.time() - start) * 1000)

        if response.success:
            print(f'✅ Strategy executed successfully in {execution_time}ms')
            print(f'   Server execution time: {response.execution_time_ms}ms')
            print(f'   Signals generated: {len(response.signals)}')
            print(f'   Indicators: {len(response.indicators)}')

            if response.performance:
                perf = response.performance
                print(f'\n📊 Performance Metrics:')
                print(f'   Total Return: {perf.total_return:.2f}%')
                print(f'   Total P&L: ${perf.total_pnl:.2f}')
                print(f'   Win Rate: {perf.win_rate:.2f}%')
                print(f'   Total Trades: {perf.total_trades}')
                print(f'   Sharpe Ratio: {perf.sharpe_ratio:.2f}')
                print(f'   Max Drawdown: {perf.max_drawdown:.2f}%')

            if response.logs:
                print(f'\n📝 Logs:')
                for log in response.logs[:5]:  # Show first 5 logs
                    print(f'   {log}')

            return True
        else:
            print(f'❌ Strategy execution failed: {response.error}')
            if response.logs:
                print('Logs:')
                for log in response.logs:
                    print(f'  {log}')
            return False

    except Exception as e:
        print(f'❌ Execution failed: {e}')
        import traceback
        traceback.print_exc()
        return False
    finally:
        channel.close()


def main():
    """Run all tests"""
    print("=" * 60)
    print("Strategy Execution Service - Test Suite")
    print("=" * 60)

    results = []

    # Test 1: Health Check
    results.append(("Health Check", test_health()))

    # Test 2: Strategy Execution
    results.append(("Strategy Execution", test_simple_strategy()))

    # Summary
    print("\n" + "=" * 60)
    print("Test Summary")
    print("=" * 60)

    for name, passed in results:
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{status} - {name}")

    passed_count = sum(1 for _, p in results if p)
    total_count = len(results)

    print(f"\nTotal: {passed_count}/{total_count} tests passed")

    return 0 if passed_count == total_count else 1


if __name__ == '__main__':
    sys.exit(main())
