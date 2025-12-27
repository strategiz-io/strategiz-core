#!/usr/bin/env python3
"""
Simple test client for the Strategy Execution Service
"""
import grpc
from strategiz_execution.generated import strategy_execution_pb2
from strategiz_execution.generated import strategy_execution_pb2_grpc
from datetime import datetime, timedelta

def test_health():
    """Test health check endpoint"""
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = strategy_execution_pb2_grpc.StrategyExecutionServiceStub(channel)
        request = strategy_execution_pb2.HealthRequest()
        response = stub.GetHealth(request)
        print(f"✅ Health Check: {response.status}")
        print(f"   Supported languages: {list(response.supported_languages)}")
        print(f"   Max timeout: {response.max_timeout_seconds}s")
        return response.status == "SERVING"

def test_simple_strategy():
    """Test a simple RSI strategy"""
    # Sample Python strategy code
    strategy_code = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05
POSITION_SIZE = 100

def calculate_rsi(data, period=14):
    '''Calculate RSI indicator'''
    delta = data['close'].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss
    rsi = 100 - (100 / (1 + rs))
    return rsi

def strategy(data):
    '''Main strategy function'''
    # Calculate RSI
    data['rsi'] = calculate_rsi(data)

    # Get latest RSI value
    current_rsi = data['rsi'].iloc[-1]

    # Generate signal
    if current_rsi < 30:
        return 'BUY'  # Oversold
    elif current_rsi > 70:
        return 'SELL'  # Overbought
    else:
        return 'HOLD'
"""

    # Create sample market data (10 bars)
    market_data = []
    base_time = datetime.now() - timedelta(days=10)
    base_price = 150.0

    for i in range(10):
        bar = strategy_execution_pb2.MarketDataBar(
            timestamp=(base_time + timedelta(days=i)).isoformat() + 'Z',
            open=base_price + i * 0.5,
            high=base_price + i * 0.5 + 2.0,
            low=base_price + i * 0.5 - 1.0,
            close=base_price + i * 0.5 + 0.5,
            volume=1000000 + i * 10000
        )
        market_data.append(bar)

    # Execute strategy
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = strategy_execution_pb2_grpc.StrategyExecutionServiceStub(channel)

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=strategy_code,
            language="python",
            market_data=market_data,
            user_id="test-user",
            strategy_id="test-rsi-strategy",
            timeout_seconds=30
        )

        print("\n📊 Executing RSI strategy...")
        response = stub.ExecuteStrategy(request)

        print(f"\n✅ Execution Result:")
        print(f"   Success: {response.success}")
        print(f"   Execution time: {response.execution_time_ms}ms")
        print(f"   Signals: {len(response.signals)}")
        print(f"   Indicators: {len(response.indicators)}")

        if response.error:
            print(f"   ❌ Error: {response.error}")

        if response.logs:
            print(f"\n📝 Logs:")
            for log in response.logs:
                print(f"   {log}")

        if response.signals:
            print(f"\n🎯 Signals:")
            for signal in response.signals:
                print(f"   {signal.timestamp}: {signal.type} @ ${signal.price:.2f}")
                if signal.reason:
                    print(f"      Reason: {signal.reason}")

        if response.performance:
            perf = response.performance
            print(f"\n📈 Performance:")
            print(f"   Total Return: {perf.total_return:.2f}%")
            print(f"   Total P&L: ${perf.total_pnl:.2f}")
            print(f"   Win Rate: {perf.win_rate:.2f}%")
            print(f"   Total Trades: {perf.total_trades}")
            print(f"   Sharpe Ratio: {perf.sharpe_ratio:.2f}")

        return response.success

if __name__ == '__main__':
    print("=" * 60)
    print("Strategy Execution Service - Test Client")
    print("=" * 60)

    # Test health check
    print("\n1️⃣  Testing Health Check...")
    health_ok = test_health()

    if health_ok:
        # Test strategy execution
        print("\n2️⃣  Testing Strategy Execution...")
        success = test_simple_strategy()

        if success:
            print("\n" + "=" * 60)
            print("✅ ALL TESTS PASSED!")
            print("=" * 60)
        else:
            print("\n" + "=" * 60)
            print("❌ Strategy execution failed")
            print("=" * 60)
    else:
        print("\n❌ Health check failed - server not ready")
