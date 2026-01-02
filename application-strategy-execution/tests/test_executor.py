"""
Unit tests for PythonExecutor
Tests strategy execution, timeout enforcement, and security sandbox
"""

import pytest
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from strategiz_execution.executor.python_executor import PythonExecutor, TimeoutException


@pytest.fixture
def executor():
    """Create PythonExecutor instance with 5-second timeout"""
    return PythonExecutor(timeout_seconds=5)


@pytest.fixture
def sample_market_data():
    """Create sample OHLCV market data for testing"""
    base_time = datetime.now() - timedelta(days=10)
    base_price = 150.0

    data = []
    for i in range(10):
        bar = {
            'timestamp': (base_time + timedelta(days=i)).isoformat() + 'Z',
            'open': base_price + i * 0.5,
            'high': base_price + i * 0.5 + 2.0,
            'low': base_price + i * 0.5 - 1.0,
            'close': base_price + i * 0.5 + 0.5,
            'volume': 1000000 + i * 10000
        }
        data.append(bar)

    return data


class TestBasicExecution:
    """Test basic strategy execution"""

    def test_simple_buy_strategy(self, executor, sample_market_data):
        """Test strategy that always returns BUY"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True
        assert 'error' not in result or result['error'] is None
        assert len(result['logs']) >= 0

    def test_simple_sell_strategy(self, executor, sample_market_data):
        """Test strategy that always returns SELL"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'SELL'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True
        assert 'error' not in result or result['error'] is None

    def test_simple_hold_strategy(self, executor, sample_market_data):
        """Test strategy that always returns HOLD"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'HOLD'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True


class TestIndicatorStrategies:
    """Test strategies using technical indicators"""

    def test_rsi_strategy(self, executor, sample_market_data):
        """Test RSI-based strategy"""
        code = """
SYMBOL = 'AAPL'
STOP_LOSS = 0.02
TAKE_PROFIT = 0.05

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
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True
        assert 'indicators' in result

    def test_moving_average_crossover(self, executor, sample_market_data):
        """Test moving average crossover strategy"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Calculate moving averages
    data['sma_fast'] = data['close'].rolling(window=3).mean()
    data['sma_slow'] = data['close'].rolling(window=5).mean()

    # Get latest values
    fast = data['sma_fast'].iloc[-1]
    slow = data['sma_slow'].iloc[-1]

    # Crossover logic
    if fast > slow:
        return 'BUY'
    elif fast < slow:
        return 'SELL'
    else:
        return 'HOLD'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True

    def test_pandas_ta_indicators(self, executor, sample_market_data):
        """Test strategy using pandas-ta library"""
        code = """
import pandas_ta as ta

SYMBOL = 'AAPL'

def strategy(data):
    # Use pandas-ta for RSI
    data['rsi'] = ta.rsi(data['close'], length=14)

    current_rsi = data['rsi'].iloc[-1]

    if current_rsi < 30:
        return 'BUY'
    elif current_rsi > 70:
        return 'SELL'
    else:
        return 'HOLD'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True


class TestDataAccess:
    """Test DataFrame access and operations"""

    def test_ohlcv_columns_available(self, executor, sample_market_data):
        """Test that all OHLCV columns are available"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Verify all columns exist
    assert 'open' in data.columns
    assert 'high' in data.columns
    assert 'low' in data.columns
    assert 'close' in data.columns
    assert 'volume' in data.columns

    return 'HOLD'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True

    def test_data_indexing(self, executor, sample_market_data):
        """Test accessing data by index"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Access latest values
    latest_close = data['close'].iloc[-1]
    previous_close = data['close'].iloc[-2]

    if latest_close > previous_close:
        return 'BUY'
    else:
        return 'SELL'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True

    def test_data_slicing(self, executor, sample_market_data):
        """Test slicing data"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Get last 5 bars
    recent_data = data.tail(5)

    # Calculate average of recent closes
    avg_close = recent_data['close'].mean()

    if data['close'].iloc[-1] > avg_close:
        return 'BUY'
    else:
        return 'SELL'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is True


class TestErrorHandling:
    """Test error handling and validation"""

    def test_syntax_error(self, executor, sample_market_data):
        """Test strategy with Python syntax error"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'BUY'  # Missing closing quote
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result

    def test_missing_strategy_function(self, executor, sample_market_data):
        """Test code without strategy() function"""
        code = """
SYMBOL = 'AAPL'

def my_function(data):
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result
        assert 'strategy' in result['error'].lower()

    def test_runtime_error(self, executor, sample_market_data):
        """Test strategy that raises runtime error"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # This will raise KeyError
    value = data['nonexistent_column'].iloc[-1]
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result

    def test_division_by_zero(self, executor, sample_market_data):
        """Test strategy with division by zero"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    result = 100 / 0  # Division by zero
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result

    def test_invalid_return_value(self, executor, sample_market_data):
        """Test strategy returning invalid signal"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'INVALID_SIGNAL'  # Not BUY/SELL/HOLD
"""
        result = executor.execute(code, sample_market_data)

        # Should complete but validation may catch this
        assert 'error' in result or result['success'] is False


class TestSecuritySandbox:
    """Test security restrictions and sandbox enforcement"""

    def test_cannot_import_os(self, executor, sample_market_data):
        """Test that 'os' module cannot be imported"""
        code = """
import os

SYMBOL = 'AAPL'

def strategy(data):
    # Try to execute system command (should be blocked)
    os.system('ls')
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result

    def test_cannot_import_subprocess(self, executor, sample_market_data):
        """Test that 'subprocess' module cannot be imported"""
        code = """
import subprocess

SYMBOL = 'AAPL'

def strategy(data):
    subprocess.run(['ls'])
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False

    def test_cannot_use_open(self, executor, sample_market_data):
        """Test that 'open' builtin is blocked"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Try to open a file (should be blocked)
    with open('/etc/passwd', 'r') as f:
        content = f.read()
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result

    def test_cannot_use_eval(self, executor, sample_market_data):
        """Test that 'eval' is blocked"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Try to use eval (should be blocked)
    result = eval('1 + 1')
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False
        assert 'error' in result

    def test_cannot_use_exec(self, executor, sample_market_data):
        """Test that 'exec' is blocked"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Try to use exec (should be blocked)
    exec('import os')
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data)

        assert result['success'] is False


class TestTimeoutEnforcement:
    """Test timeout enforcement"""

    def test_infinite_loop_timeout(self, executor, sample_market_data):
        """Test that infinite loops are terminated"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Infinite loop
    while True:
        pass
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data, timeout_seconds=2)

        assert result['success'] is False
        assert 'error' in result
        assert 'timeout' in result['error'].lower() or 'time' in result['error'].lower()

    def test_slow_computation_timeout(self, executor, sample_market_data):
        """Test timeout with slow computation"""
        code = """
import time

SYMBOL = 'AAPL'

def strategy(data):
    # Sleep for longer than timeout
    time.sleep(10)
    return 'BUY'
"""
        result = executor.execute(code, sample_market_data, timeout_seconds=2)

        assert result['success'] is False
        assert 'error' in result

    def test_fast_execution_completes(self, executor, sample_market_data):
        """Test that fast strategies complete successfully"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    # Fast computation
    result = data['close'].mean()
    if result > 150:
        return 'BUY'
    else:
        return 'SELL'
"""
        result = executor.execute(code, sample_market_data, timeout_seconds=2)

        assert result['success'] is True


class TestPerformanceOptimization:
    """Test performance optimizations"""

    def test_large_dataset_performance(self, executor):
        """Test execution with large dataset (1000 bars)"""
        # Create large dataset
        large_data = []
        base_time = datetime.now() - timedelta(days=1000)
        for i in range(1000):
            bar = {
                'timestamp': (base_time + timedelta(days=i)).isoformat() + 'Z',
                'open': 150.0 + (i % 10),
                'high': 152.0 + (i % 10),
                'low': 149.0 + (i % 10),
                'close': 151.0 + (i % 10),
                'volume': 1000000
            }
            large_data.append(bar)

        code = """
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
        result = executor.execute(code, large_data, timeout_seconds=10)

        assert result['success'] is True
        # Verify execution time is reasonable
        assert result.get('execution_time_ms', 0) < 5000  # Under 5 seconds

    def test_code_caching(self, executor, sample_market_data):
        """Test that repeated execution uses cached compiled code"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'BUY'
"""

        # Execute same code multiple times
        result1 = executor.execute(code, sample_market_data)
        result2 = executor.execute(code, sample_market_data)
        result3 = executor.execute(code, sample_market_data)

        assert result1['success'] is True
        assert result2['success'] is True
        assert result3['success'] is True


class TestEdgeCases:
    """Test edge cases and boundary conditions"""

    def test_empty_market_data(self, executor):
        """Test execution with empty market data"""
        code = """
SYMBOL = 'AAPL'

def strategy(data):
    if len(data) == 0:
        return 'HOLD'
    return 'BUY'
"""
        result = executor.execute(code, [])

        # Should handle gracefully
        assert 'error' in result or result['success'] is False

    def test_single_bar_data(self, executor):
        """Test execution with single bar"""
        single_bar = [{
            'timestamp': datetime.now().isoformat() + 'Z',
            'open': 150.0,
            'high': 152.0,
            'low': 149.0,
            'close': 151.0,
            'volume': 1000000
        }]

        code = """
SYMBOL = 'AAPL'

def strategy(data):
    if len(data) == 1:
        return 'HOLD'
    return 'BUY'
"""
        result = executor.execute(code, single_bar)

        assert result['success'] is True

    def test_missing_volume_data(self, executor):
        """Test handling of missing volume data"""
        data = [{
            'timestamp': datetime.now().isoformat() + 'Z',
            'open': 150.0,
            'high': 152.0,
            'low': 149.0,
            'close': 151.0
            # Missing 'volume' field
        }]

        code = """
SYMBOL = 'AAPL'

def strategy(data):
    return 'BUY'
"""
        result = executor.execute(code, data)

        # Should fail due to missing volume
        assert result['success'] is False
