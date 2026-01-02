# Strategy Execution Service - Test Suite

Comprehensive test coverage for the Python gRPC microservice that executes trading strategies.

## Test Structure

```
tests/
├── test_executor.py           # Unit tests for PythonExecutor
├── test_grpc_integration.py   # Integration tests for gRPC service
└── README.md                  # This file
```

## Test Categories

### Unit Tests (`test_executor.py`)
Tests the Python strategy executor in isolation without gRPC:
- ✅ Basic strategy execution (BUY, SELL, HOLD)
- ✅ Technical indicator strategies (RSI, MA, pandas-ta)
- ✅ DataFrame operations and data access
- ✅ Error handling (syntax errors, runtime errors, invalid return values)
- ✅ Security sandbox (blocked imports, forbidden builtins)
- ✅ Timeout enforcement (infinite loops, slow computations)
- ✅ Performance optimization (large datasets, code caching)
- ✅ Edge cases (empty data, single bar, missing fields)

### Integration Tests (`test_grpc_integration.py`)
Tests the full gRPC service endpoints with real client-server communication:
- ✅ Health check endpoint
- ✅ Code validation endpoint
- ✅ Strategy execution endpoint
- ✅ Large dataset handling (1000+ bars)
- ✅ Concurrent request handling
- ✅ Edge cases and error scenarios

## Running Tests

### Prerequisites

1. **Install test dependencies:**
   ```bash
   pip install -r requirements-test.txt
   ```

2. **Start the gRPC service** (for integration tests):
   ```bash
   python -m strategiz_execution.server
   ```
   The service should be running on `localhost:50051`

### Run All Tests

```bash
# Run all tests with verbose output
pytest -v

# Run with coverage report
pytest --cov=strategiz_execution --cov-report=html

# Run in parallel (faster)
pytest -n auto
```

### Run Specific Test Categories

```bash
# Unit tests only (no gRPC service required)
pytest tests/test_executor.py -v

# Integration tests only (requires gRPC service running)
pytest tests/test_grpc_integration.py -v

# Tests by marker
pytest -m unit           # Unit tests
pytest -m integration    # Integration tests
pytest -m security       # Security tests
pytest -m performance    # Performance tests
```

### Run Specific Tests

```bash
# Run specific test class
pytest tests/test_executor.py::TestBasicExecution -v

# Run specific test method
pytest tests/test_executor.py::TestBasicExecution::test_simple_buy_strategy -v

# Run tests matching pattern
pytest -k "test_rsi" -v
```

## Test Markers

Use markers to organize and filter tests:

```python
@pytest.mark.unit
def test_basic_execution():
    pass

@pytest.mark.integration
def test_grpc_health_check():
    pass

@pytest.mark.slow
def test_large_dataset():
    pass

@pytest.mark.security
def test_sandbox_enforcement():
    pass
```

Run tests by marker:
```bash
pytest -m "unit"              # Only unit tests
pytest -m "not slow"          # Skip slow tests
pytest -m "security"          # Only security tests
pytest -m "integration"       # Only integration tests
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Python Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.12'

      - name: Install dependencies
        run: |
          pip install -r requirements-test.txt

      - name: Run unit tests
        run: |
          pytest tests/test_executor.py -v --cov=strategiz_execution

      - name: Start gRPC service
        run: |
          python -m strategiz_execution.server &
          sleep 5  # Wait for service to start

      - name: Run integration tests
        run: |
          pytest tests/test_grpc_integration.py -v

      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

## Test Coverage

### Current Coverage

```bash
# Generate coverage report
pytest --cov=strategiz_execution --cov-report=term-missing

# Generate HTML coverage report
pytest --cov=strategiz_execution --cov-report=html
open htmlcov/index.html
```

### Coverage Goals

- **Overall**: >80%
- **Executor**: >90% (core business logic)
- **Security**: 100% (security-critical code)
- **gRPC Service**: >85%

## Debugging Tests

### Verbose Output

```bash
# Extra verbose with stdout
pytest -vvs

# Show local variables on failures
pytest -l

# Drop into debugger on failure
pytest --pdb
```

### Logging

```bash
# Show all logs
pytest --log-cli-level=DEBUG

# Show logs only on failure
pytest --log-cli-level=INFO
```

### Specific Test with Print Statements

```bash
# Run single test with print output
pytest tests/test_executor.py::TestBasicExecution::test_simple_buy_strategy -s
```

## Common Issues

### Integration Tests Failing

**Problem**: Integration tests skip with "gRPC service not running"

**Solution**:
```bash
# Start the gRPC service first
python -m strategiz_execution.server

# In another terminal, run integration tests
pytest tests/test_grpc_integration.py -v
```

### Import Errors

**Problem**: `ModuleNotFoundError: No module named 'strategiz_execution'`

**Solution**:
```bash
# Ensure you're in the application-strategy-execution directory
cd application-strategy-execution

# Install in editable mode
pip install -e .

# Or add to PYTHONPATH
export PYTHONPATH="${PYTHONPATH}:$(pwd)"
```

### Timeout Errors

**Problem**: Tests timeout on slow machines

**Solution**:
```bash
# Increase timeout for slow tests
pytest --timeout=60  # 60 second timeout per test
```

## Writing New Tests

### Test Template

```python
import pytest
from strategiz_execution.executor.python_executor import PythonExecutor

@pytest.fixture
def executor():
    return PythonExecutor(timeout_seconds=5)

@pytest.fixture
def sample_data():
    return [
        {
            'timestamp': '2024-01-15T10:00:00Z',
            'open': 150.0,
            'high': 152.0,
            'low': 149.0,
            'close': 151.0,
            'volume': 1000000
        }
    ]

class TestMyFeature:
    """Test my new feature"""

    @pytest.mark.unit
    def test_my_feature(self, executor, sample_data):
        """Test description"""
        # Arrange
        code = "def strategy(data): return 'BUY'"

        # Act
        result = executor.execute(code, sample_data)

        # Assert
        assert result['success'] is True
```

### Best Practices

1. **Use descriptive test names**: `test_execute_rsi_strategy_oversold_signal`
2. **Follow AAA pattern**: Arrange, Act, Assert
3. **One assertion per test** (ideally)
4. **Use fixtures** for common setup
5. **Add markers** for organization
6. **Document edge cases** in docstrings
7. **Test both success and failure paths**

## Performance Testing

```bash
# Run performance benchmarks
pytest -m performance --durations=10

# Profile tests
pytest --profile

# Memory profiling
pytest --memprof
```

## Security Testing

All security tests should pass 100%:

```bash
# Run security-focused tests
pytest -m security -v

# Verify sandbox enforcement
pytest tests/test_executor.py::TestSecuritySandbox -v
```

## Local Development Workflow

1. **Make changes** to executor/validator/servicer code
2. **Run unit tests** to verify logic:
   ```bash
   pytest tests/test_executor.py -v
   ```
3. **Start gRPC service** locally:
   ```bash
   python -m strategiz_execution.server
   ```
4. **Run integration tests** to verify gRPC:
   ```bash
   pytest tests/test_grpc_integration.py -v
   ```
5. **Check coverage**:
   ```bash
   pytest --cov=strategiz_execution --cov-report=term-missing
   ```
6. **Commit** when all tests pass

## Questions?

For issues or questions about the test suite, see:
- [Main Project README](../README.md)
- [gRPC Service Documentation](../docs/grpc_service.md)
- [Security Sandbox Documentation](../docs/security.md)
