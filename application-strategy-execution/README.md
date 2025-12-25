# Strategy Execution Service

Python-based gRPC service for executing trading strategies in a secure, isolated environment.

## Features

- ✅ **Secure Execution**: RestrictedPython sandbox for user code
- ✅ **Full Python Ecosystem**: pandas, numpy, pandas-ta, TA-Lib support
- ✅ **High Performance**: Native Python execution (2-3x faster than subprocess)
- ✅ **Complete Isolation**: Separate Cloud Run service from main API
- ✅ **Resource Limits**: Timeout, memory, and CPU constraints
- ✅ **Observability**: OpenTelemetry integration for metrics and traces

## Architecture

```
Main API (Java) → gRPC → Execution Service (Python) → Direct execution
```

## Local Development

### Setup

```bash
# Create virtual environment
python3.11 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt

# Generate gRPC code from proto
python -m grpc_tools.protoc \
    -I../proto \
    --python_out=./strategiz_execution/generated \
    --grpc_python_out=./strategiz_execution/generated \
    ../proto/strategy_execution.proto
```

### Run Server

```bash
# Start gRPC server
python -m strategiz_execution.server

# Server runs on port 50051
```

### Run Tests

```bash
pytest
pytest --cov=strategiz_execution
```

## Docker

### Build

```bash
docker build -t strategiz-execution .
```

### Run

```bash
docker run -p 50051:50051 strategiz-execution
```

## Deployment

Deployed to Google Cloud Run:

```bash
gcloud builds submit --config=../deployment/cloudbuild-execution.yaml
```

## Configuration

Environment variables:

- `GRPC_PORT`: gRPC server port (default: 50051)
- `MAX_WORKERS`: Thread pool size (default: 20)
- `MAX_TIMEOUT_SECONDS`: Max execution timeout (default: 30)
- `MAX_MEMORY_MB`: Max memory per execution (default: 512)
- `LOG_LEVEL`: Logging level (default: INFO)
- `ENVIRONMENT`: Environment name (development/production)

## Security

### Code Execution Sandbox

- **RestrictedPython**: Compiles code with security restrictions
- **Blocked imports**: os, sys, subprocess, socket, etc.
- **Blocked functions**: eval, exec, open, etc.
- **Timeout**: 30 second maximum
- **Memory limit**: 512MB per execution

### Network Isolation

- No outbound network access from user code
- No file system access
- Process-level isolation via Docker/Cloud Run

## Testing with grpcurl

```bash
# Health check
grpcurl -plaintext localhost:50051 strategiz.execution.v1.StrategyExecutionService/GetHealth

# List services
grpcurl -plaintext localhost:50051 list

# Execute strategy
grpcurl -plaintext -d @ localhost:50051 strategiz.execution.v1.StrategyExecutionService/ExecuteStrategy <<EOF
{
  "code": "def strategy(data):\n    return 'BUY'",
  "language": "python",
  "market_data": [
    {"timestamp": "2024-01-01T00:00:00Z", "open": 100, "high": 101, "low": 99, "close": 100.5, "volume": 1000}
  ],
  "user_id": "test-user",
  "strategy_id": "test-strategy"
}
EOF
```
