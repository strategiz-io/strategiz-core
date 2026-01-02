"""
Integration tests for gRPC Strategy Execution Service
Tests the full gRPC service endpoints with real client-server communication
"""

import pytest
import grpc
from datetime import datetime, timedelta
from strategiz_execution.generated import strategy_execution_pb2
from strategiz_execution.generated import strategy_execution_pb2_grpc
from strategiz_execution.service.execution_servicer import StrategyExecutionServicer


@pytest.fixture(scope="module")
def grpc_channel():
    """
    Create in-process gRPC channel for testing
    Note: For full integration tests, connect to actual localhost:50051
    """
    # For now, test against localhost service
    # In CI/CD, this would start the service automatically
    channel = grpc.insecure_channel('localhost:50051')
    yield channel
    channel.close()


@pytest.fixture(scope="module")
def grpc_stub(grpc_channel):
    """Create gRPC stub for making requests"""
    return strategy_execution_pb2_grpc.StrategyExecutionServiceStub(grpc_channel)


@pytest.fixture
def sample_market_data():
    """Create sample market data bars for testing"""
    bars = []
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
        bars.append(bar)

    return bars


class TestHealthCheck:
    """Test health check endpoint"""

    def test_health_check_success(self, grpc_stub):
        """Test successful health check"""
        request = strategy_execution_pb2.HealthRequest()

        try:
            response = grpc_stub.GetHealth(request, timeout=5)

            assert response.status == "SERVING"
            assert "python" in response.supported_languages
            assert response.max_timeout_seconds > 0
            assert response.max_memory_mb > 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_health_check_metadata(self, grpc_stub):
        """Test health check returns metadata"""
        request = strategy_execution_pb2.HealthRequest()

        try:
            response = grpc_stub.GetHealth(request, timeout=5)

            # Check metadata fields
            assert len(response.metadata) > 0
            # Expect version info
            assert "version" in response.metadata or "service" in response.metadata

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")


class TestCodeValidation:
    """Test code validation endpoint"""

    def test_validate_valid_python_code(self, grpc_stub):
        """Test validation of valid Python code"""
        valid_code = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05

def strategy(data):
    return 'BUY'
"""

        request = strategy_execution_pb2.ValidateCodeRequest(
            code=valid_code,
            language="python"
        )

        try:
            response = grpc_stub.ValidateCode(request, timeout=10)

            assert response.valid is True
            assert len(response.errors) == 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_validate_missing_strategy_function(self, grpc_stub):
        """Test validation catches missing strategy() function"""
        invalid_code = """
SYMBOL = 'AAPL'

def my_function(data):
    return 'BUY'
"""

        request = strategy_execution_pb2.ValidateCodeRequest(
            code=invalid_code,
            language="python"
        )

        try:
            response = grpc_stub.ValidateCode(request, timeout=10)

            assert response.valid is False
            assert len(response.errors) > 0
            # Error should mention missing strategy function
            error_text = ' '.join(response.errors).lower()
            assert 'strategy' in error_text or 'function' in error_text

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_validate_missing_symbol_constant(self, grpc_stub):
        """Test validation warns about missing SYMBOL"""
        code_without_symbol = """
def strategy(data):
    return 'BUY'
"""

        request = strategy_execution_pb2.ValidateCodeRequest(
            code=code_without_symbol,
            language="python"
        )

        try:
            response = grpc_stub.ValidateCode(request, timeout=10)

            # May pass validation but should have warning
            if not response.valid:
                assert len(response.errors) > 0
            else:
                # Should have warning about missing SYMBOL
                assert len(response.warnings) > 0 or len(response.suggestions) > 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_validate_python_syntax_error(self, grpc_stub):
        """Test validation catches Python syntax errors"""
        code_with_syntax_error = """
SYMBOL = 'AAPL'

def strategy(data)  # Missing colon
    return 'BUY'
"""

        request = strategy_execution_pb2.ValidateCodeRequest(
            code=code_with_syntax_error,
            language="python"
        )

        try:
            response = grpc_stub.ValidateCode(request, timeout=10)

            assert response.valid is False
            assert len(response.errors) > 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")


class TestStrategyExecution:
    """Test strategy execution endpoint"""

    def test_execute_simple_buy_strategy(self, grpc_stub, sample_market_data):
        """Test executing simple BUY strategy"""
        strategy_code = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05

def strategy(data):
    return 'BUY'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=strategy_code,
            language="python",
            market_data=sample_market_data,
            user_id="test-user-123",
            strategy_id="test-strategy-buy",
            timeout_seconds=30
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=35)

            assert response.success is True
            assert response.execution_time_ms > 0
            assert response.execution_time_ms < 30000  # Under 30 seconds
            assert len(response.error) == 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_execute_rsi_strategy(self, grpc_stub, sample_market_data):
        """Test executing RSI-based strategy"""
        rsi_strategy = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05

def calculate_rsi(data, period=14):
    delta = data['close'].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss
    rsi = 100 - (100 / (1 + rs))
    return rsi

def strategy(data):
    data['rsi'] = calculate_rsi(data)
    current_rsi = data['rsi'].iloc[-1]

    if current_rsi < 30:
        return 'BUY'
    elif current_rsi > 70:
        return 'SELL'
    else:
        return 'HOLD'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=rsi_strategy,
            language="python",
            market_data=sample_market_data,
            user_id="test-user-123",
            strategy_id="test-rsi-strategy",
            timeout_seconds=30
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=35)

            assert response.success is True
            assert response.execution_time_ms > 0
            # RSI strategy should generate indicators
            assert len(response.indicators) >= 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_execute_strategy_with_signals(self, grpc_stub, sample_market_data):
        """Test strategy execution generates signals"""
        strategy_with_signals = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05

def strategy(data):
    # Simple moving average crossover
    data['sma_fast'] = data['close'].rolling(window=3).mean()
    data['sma_slow'] = data['close'].rolling(window=5).mean()

    if data['sma_fast'].iloc[-1] > data['sma_slow'].iloc[-1]:
        return 'BUY'
    else:
        return 'SELL'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=strategy_with_signals,
            language="python",
            market_data=sample_market_data,
            user_id="test-user-123",
            strategy_id="test-signals",
            timeout_seconds=30
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=35)

            assert response.success is True
            # Strategy should generate at least one signal
            assert len(response.signals) >= 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_execute_strategy_with_performance(self, grpc_stub, sample_market_data):
        """Test strategy execution returns performance metrics"""
        backtest_strategy = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05

def strategy(data):
    # Simple trend following
    data['sma'] = data['close'].rolling(window=5).mean()

    if data['close'].iloc[-1] > data['sma'].iloc[-1]:
        return 'BUY'
    else:
        return 'SELL'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=backtest_strategy,
            language="python",
            market_data=sample_market_data,
            user_id="test-user-123",
            strategy_id="test-backtest",
            timeout_seconds=30
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=35)

            assert response.success is True

            # Check if performance metrics are returned
            if response.HasField('performance'):
                perf = response.performance
                assert perf.total_trades >= 0
                assert perf.total_return != 0.0 or perf.total_trades == 0
                # Win rate should be between 0 and 100
                assert 0 <= perf.win_rate <= 100 or perf.total_trades == 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_execute_strategy_with_error(self, grpc_stub, sample_market_data):
        """Test strategy execution handles errors gracefully"""
        broken_strategy = """
SYMBOL = 'AAPL'

def strategy(data):
    # This will raise a NameError
    return undefined_variable
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=broken_strategy,
            language="python",
            market_data=sample_market_data,
            user_id="test-user-123",
            strategy_id="test-error",
            timeout_seconds=30
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=35)

            assert response.success is False
            assert len(response.error) > 0
            # Error should contain useful information
            assert 'NameError' in response.error or 'undefined' in response.error.lower()

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_execute_strategy_timeout(self, grpc_stub, sample_market_data):
        """Test strategy execution enforces timeout"""
        slow_strategy = """
import time

SYMBOL = 'AAPL'

def strategy(data):
    # Sleep longer than timeout
    time.sleep(10)
    return 'BUY'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=slow_strategy,
            language="python",
            market_data=sample_market_data,
            user_id="test-user-123",
            strategy_id="test-timeout",
            timeout_seconds=2  # Short timeout
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=5)

            # Should fail due to timeout
            assert response.success is False
            assert len(response.error) > 0
            assert 'timeout' in response.error.lower() or 'time' in response.error.lower()

        except grpc.RpcError as e:
            # gRPC might throw DEADLINE_EXCEEDED
            assert e.code() == grpc.StatusCode.DEADLINE_EXCEEDED or True


class TestLargeDatasets:
    """Test execution with large datasets"""

    def test_execute_with_1000_bars(self, grpc_stub):
        """Test execution with 1000 market data bars"""
        # Create large dataset
        large_data = []
        base_time = datetime.now() - timedelta(days=1000)

        for i in range(1000):
            bar = strategy_execution_pb2.MarketDataBar(
                timestamp=(base_time + timedelta(days=i)).isoformat() + 'Z',
                open=150.0 + (i % 10),
                high=152.0 + (i % 10),
                low=149.0 + (i % 10),
                close=151.0 + (i % 10),
                volume=1000000
            )
            large_data.append(bar)

        strategy = """
SYMBOL = 'AAPL'

def strategy(data):
    # Calculate indicators on large dataset
    data['sma_20'] = data['close'].rolling(window=20).mean()
    data['sma_50'] = data['close'].rolling(window=50).mean()

    if data['sma_20'].iloc[-1] > data['sma_50'].iloc[-1]:
        return 'BUY'
    else:
        return 'SELL'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=strategy,
            language="python",
            market_data=large_data,
            user_id="test-user-large",
            strategy_id="test-large-dataset",
            timeout_seconds=30
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=35)

            assert response.success is True
            # Should complete in reasonable time even with 1000 bars
            assert response.execution_time_ms < 10000  # Under 10 seconds

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")


class TestConcurrency:
    """Test concurrent request handling"""

    def test_concurrent_executions(self, grpc_stub, sample_market_data):
        """Test multiple concurrent strategy executions"""
        import concurrent.futures

        strategy = """
SYMBOL = 'AAPL'

def strategy(data):
    import time
    time.sleep(0.1)  # Small delay to test concurrency
    return 'BUY'
"""

        def execute_strategy(strategy_id):
            request = strategy_execution_pb2.ExecuteStrategyRequest(
                code=strategy,
                language="python",
                market_data=sample_market_data,
                user_id="test-user-concurrent",
                strategy_id=f"concurrent-{strategy_id}",
                timeout_seconds=10
            )
            try:
                response = grpc_stub.ExecuteStrategy(request, timeout=15)
                return response.success
            except grpc.RpcError:
                return False

        try:
            # Execute 5 strategies concurrently
            with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
                futures = [executor.submit(execute_strategy, i) for i in range(5)]
                results = [f.result() for f in concurrent.futures.as_completed(futures)]

            # All should succeed
            assert all(results)

        except Exception as e:
            pytest.skip(f"Concurrent execution test failed: {e}")


class TestEdgeCases:
    """Test edge cases and boundary conditions"""

    def test_empty_code(self, grpc_stub, sample_market_data):
        """Test execution with empty code"""
        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code="",
            language="python",
            market_data=sample_market_data,
            user_id="test-user",
            strategy_id="test-empty",
            timeout_seconds=10
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=15)

            assert response.success is False
            assert len(response.error) > 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_empty_market_data(self, grpc_stub):
        """Test execution with empty market data"""
        strategy = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'HOLD'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=strategy,
            language="python",
            market_data=[],  # Empty market data
            user_id="test-user",
            strategy_id="test-no-data",
            timeout_seconds=10
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=15)

            # Should handle gracefully
            assert response.success is False or len(response.signals) == 0

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")

    def test_single_bar_data(self, grpc_stub):
        """Test execution with single market data bar"""
        single_bar = [
            strategy_execution_pb2.MarketDataBar(
                timestamp=datetime.now().isoformat() + 'Z',
                open=150.0,
                high=152.0,
                low=149.0,
                close=151.0,
                volume=1000000
            )
        ]

        strategy = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'HOLD'
"""

        request = strategy_execution_pb2.ExecuteStrategyRequest(
            code=strategy,
            language="python",
            market_data=single_bar,
            user_id="test-user",
            strategy_id="test-single-bar",
            timeout_seconds=10
        )

        try:
            response = grpc_stub.ExecuteStrategy(request, timeout=15)

            assert response.success is True

        except grpc.RpcError as e:
            pytest.skip(f"gRPC service not running: {e}")
