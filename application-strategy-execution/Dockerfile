# Multi-stage build for Python gRPC service
FROM python:3.12-slim as builder

WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY application-strategy-execution/requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# Generate gRPC code from proto
COPY proto/strategy_execution.proto proto/
RUN python -m grpc_tools.protoc \
    -I proto \
    --python_out=strategiz_execution/generated \
    --grpc_python_out=strategiz_execution/generated \
    proto/strategy_execution.proto

# Production stage
FROM python:3.12-slim

WORKDIR /app

# Copy Python dependencies from builder
COPY --from=builder /root/.local /root/.local

# Copy application code
COPY application-strategy-execution/strategiz_execution/ strategiz_execution/

# Copy generated gRPC code from builder
COPY --from=builder /app/strategiz_execution/generated/ strategiz_execution/generated/

# Create __init__.py files for Python packages
RUN touch strategiz_execution/__init__.py && \
    touch strategiz_execution/service/__init__.py && \
    touch strategiz_execution/executor/__init__.py && \
    touch strategiz_execution/security/__init__.py && \
    touch strategiz_execution/generated/__init__.py && \
    touch strategiz_execution/utils/__init__.py

# Make sure scripts in .local are usable
ENV PATH=/root/.local/bin:$PATH
ENV PYTHONPATH=/app:$PYTHONPATH

# Security: Run as non-root user
RUN useradd -m -u 1000 strategiz && \
    chown -R strategiz:strategiz /app
USER strategiz

# Environment variables with defaults
ENV GRPC_PORT=50051
ENV MAX_WORKERS=20
ENV MAX_TIMEOUT_SECONDS=30
ENV MAX_MEMORY_MB=512
ENV ENVIRONMENT=production
ENV LOG_LEVEL=INFO

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import grpc; from strategiz_execution.generated import strategy_execution_pb2_grpc, strategy_execution_pb2; channel = grpc.insecure_channel('localhost:50051'); stub = strategy_execution_pb2_grpc.StrategyExecutionServiceStub(channel); stub.GetHealth(strategy_execution_pb2.HealthRequest(), timeout=2)"

# Expose gRPC port
EXPOSE 50051

# Run the gRPC server
CMD ["python", "-m", "strategiz_execution.server"]
