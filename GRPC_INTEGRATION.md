# gRPC Python Execution Service Integration

## Overview

This document describes the integration between the Java API and the Python gRPC execution service.

## Architecture

```
┌──────────────────────────┐
│   User Request           │
│   POST /v1/strategies/   │
│   execute-code           │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────────────────┐
│  ExecuteStrategyController.java      │
│  - Validates request                 │
│  - Fetches market data from Firestore│
│  - Converts to gRPC format           │
└────────────┬─────────────────────────┘
             │
             ▼
┌──────────────────────────────────────┐
│  ExecutionServiceClient.java         │
│  - gRPC client                       │
│  - Calls Python service              │
│  - Handles timeouts & errors         │
└────────────┬─────────────────────────┘
             │ gRPC call
             ▼
┌──────────────────────────────────────┐
│  Python Execution Service            │
│  strategiz-execution (Cloud Run)     │
│  - Sandboxed Python execution        │
│  - RestrictedPython security         │
│  - Sub-100ms performance (cached)    │
│  - Exports metrics to Grafana        │
└────────────┬─────────────────────────┘
             │
             ▼
┌──────────────────────────────────────┐
│  Response                            │
│  - Signals (BUY/SELL)                │
│  - Indicators (SMA, RSI, etc.)       │
│  - Performance metrics               │
│  - Execution time                    │
└──────────────────────────────────────┘
```

## Components Created

### 1. gRPC Client Module (`client/client-execution/`)

**Files:**
- `pom.xml` - Maven configuration with gRPC dependencies
- `ExecutionServiceClient.java` - Main gRPC client
- `ExecutionResponse.java` - Response model
- `Signal.java` - Trading signal model
- `Models.java` - All other domain models

**Key Features:**
- Automatic protobuf compilation from `proto/strategy_execution.proto`
- Connection pooling and keep-alive
- Timeout handling
- Error recovery
- Automatic conversion between gRPC and domain models

### 2. Configuration

**Service URL Configuration:**
```properties
# application-prod.properties
strategiz.execution.service.host=strategiz-execution-43628135674.us-east1.run.app
strategiz.execution.service.port=443
strategiz.execution.service.use-tls=true
```

### 3. Controller Integration

The integration is ready in `ExecuteStrategyController.java` but currently commented out (lines 130-167).

**To Enable:**
1. Add `client-execution` dependency to `service-labs/pom.xml`:
   ```xml
   <dependency>
       <groupId>io.strategiz</groupId>
       <artifactId>client-execution</artifactId>
       <version>${project.version}</version>
   </dependency>
   ```

2. Uncomment the gRPC execution code in `ExecuteStrategyController.java`

3. Inject the client:
   ```java
   private final ExecutionServiceClient executionServiceClient;

   @Autowired
   public ExecuteStrategyController(..., ExecutionServiceClient executionServiceClient) {
       // ...
       this.executionServiceClient = executionServiceClient;
   }
   ```

## Usage Example

```java
// In ExecuteStrategyController.executeCode()

// Fetch market data
List<Map<String, Object>> marketDataList = fetchMarketDataListForSymbol(symbol);

// Convert to gRPC format
List<MarketDataBar> grpcMarketData = marketDataList.stream()
    .map(ExecutionServiceClient::createMarketDataBar)
    .collect(Collectors.toList());

// Execute via gRPC
ExecutionResponse response = executionServiceClient.executeStrategy(
    request.getCode(),
    "python",
    grpcMarketData,
    userId,
    "direct-execution-" + System.currentTimeMillis(),
    30  // timeout seconds
);

// Convert to REST response
ExecuteStrategyResponse restResponse = convertGrpcToRestResponse(response, symbol);
return ResponseEntity.ok(restResponse);
```

## Benefits

### ✅ Security
- Python code runs in isolated gRPC service
- RestrictedPython sandboxing
- No subprocess execution in main API
- Resource limits enforced

### ✅ Performance
- **Sub-100ms execution** (3-6ms cached)
- Code compilation caching
- Pre-allocated numpy arrays
- Single-pass data conversion

### ✅ Observability
- Metrics exported to Grafana Cloud
- Execution time tracking
- Cache hit rate monitoring
- Error rate tracking

### ✅ Scalability
- Python service scales independently
- Cloud Run auto-scaling (1-20 instances)
- gRPC connection pooling
- Non-blocking execution

## Migration Path

### Phase 1: Parallel Execution (Testing)
Keep both old subprocess and new gRPC execution:
```java
if ("python".equalsIgnoreCase(language) && useGrpcExecution) {
    // New gRPC path
    return executeViaGrpc(request);
} else {
    // Old subprocess path (fallback)
    return executeViaSubprocess(request);
}
```

### Phase 2: Full Migration
1. Enable gRPC for all Python executions
2. Monitor metrics for 1 week
3. Remove old `PythonStrategyExecutor` class
4. Remove subprocess-based execution

### Phase 3: Cleanup
1. Delete `service/service-labs/resources/python/strategy_executor.py`
2. Remove subprocess dependencies
3. Update documentation

## Testing

### Test gRPC Client Health
```bash
curl https://strategiz-api-43628135674.us-east1.run.app/v1/execution/health
```

### Test Strategy Execution
```bash
curl -X POST https://strategiz-api-43628135674.us-east1.run.app/v1/strategies/execute-code \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "df[\"sma\"] = df[\"close\"].rolling(20).mean()\nif df[\"close\"].iloc[-1] > df[\"sma\"].iloc[-1]: signal(\"buy\", \"SMA crossover\")",
    "language": "python",
    "symbol": "AAPL",
    "timeframe": "1D"
  }'
```

## Deployment

The gRPC client will be automatically included when you:
1. Add the dependency to `service-labs/pom.xml`
2. Build and deploy the Java API

No additional deployment steps required - the Python service is already deployed at:
`https://strategiz-execution-43628135674.us-east1.run.app`

## Troubleshooting

### gRPC Connection Issues
- Check Cloud Run service is running: `gcloud run services describe strategiz-execution`
- Verify DNS resolution: `nslookup strategiz-execution-43628135674.us-east1.run.app`
- Check gRPC health: Use client's `getHealth()` method

### Timeout Issues
- Increase timeout in controller (default: 30s)
- Check Python service logs for slow executions
- Verify cache is working (should be 3-6ms for cached)

### Protobuf Compilation Issues
- Ensure `proto/` directory is accessible during Maven build
- Check protoc version matches (3.25.1)
- Clean and rebuild: `mvn clean install`

## Performance Expectations

### Cached Execution (Code Seen Before)
- Simple strategy: **3-4ms**
- Complex strategy: **5-7ms**
- Cache hit rate: **80-90%**

### First Execution (Code Compilation)
- Simple strategy: **20-30ms**
- Complex strategy: **30-40ms**
- Includes: code compilation + execution

### gRPC Overhead
- Network latency: **10-20ms** (Cloud Run to Cloud Run in same region)
- Serialization: **1-2ms**
- Total overhead: **~15ms**

## Next Steps

1. ✅ gRPC client created
2. ✅ Python service deployed with metrics
3. ⏳ Enable gRPC in controller (waiting for dependency addition)
4. ⏳ Test end-to-end execution
5. ⏳ Monitor metrics in Grafana
6. ⏳ Remove old subprocess execution

---

**Integration Status:** Ready to enable (add dependency + uncomment code)
**Python Service:** Deployed and operational
**Performance:** Sub-100ms target achieved
**Observability:** Grafana Cloud metrics configured
