package io.strategiz.client.execution;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.strategiz.client.execution.model.*;
import io.strategiz.execution.grpc.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ExecutionServiceClient using in-process gRPC server.
 * Tests client behavior with real gRPC communication but mocked service implementation.
 */
@ExtendWith(MockitoExtension.class)
class ExecutionServiceClientTest {

    private final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private ExecutionServiceClient client;
    private TestExecutionServiceImpl testService;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        // Generate unique in-process server name
        String serverName = InProcessServerBuilder.generateName();

        // Create test service implementation
        testService = new TestExecutionServiceImpl();

        // Register server
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(testService)
                .build()
                .start());

        // Create channel
        channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        // Create client with test channel
        client = new ExecutionServiceClient();
        // Use reflection to set the channel (since it's normally created in @PostConstruct)
        java.lang.reflect.Field channelField = ExecutionServiceClient.class.getDeclaredField("channel");
        channelField.setAccessible(true);
        channelField.set(client, channel);

        java.lang.reflect.Field stubField = ExecutionServiceClient.class.getDeclaredField("blockingStub");
        stubField.setAccessible(true);
        stubField.set(client, StrategyExecutionServiceGrpc.newBlockingStub(channel));
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    // ========================
    // Health Check Tests
    // ========================

    @Test
    void testGetHealth_Success() {
        // Arrange
        testService.setHealthResponse(HealthResponse.newBuilder()
                .setStatus("SERVING")
                .addSupportedLanguages("python")
                .setMaxTimeoutSeconds(60)
                .setMaxMemoryMb(512)
                .putMetadata("version", "1.0.0")
                .build());

        // Act
        HealthStatus health = client.getHealth();

        // Assert
        assertNotNull(health);
        assertEquals("SERVING", health.getStatus());
        assertTrue(health.getSupportedLanguages().contains("python"));
        assertEquals(60, health.getMaxTimeoutSeconds());
        assertEquals(512, health.getMaxMemoryMb());
        assertEquals("1.0.0", health.getMetadata().get("version"));
    }

    @Test
    void testGetHealth_ServiceUnavailable() {
        // Arrange
        testService.setHealthError(Status.UNAVAILABLE.withDescription("Service not ready"));

        // Act
        HealthStatus health = client.getHealth();

        // Assert
        assertNotNull(health);
        assertEquals("NOT_SERVING", health.getStatus());
    }

    // ========================
    // Code Validation Tests
    // ========================

    @Test
    void testValidateCode_ValidPythonCode() {
        // Arrange
        String validCode = "def strategy(data):\n    return 'BUY'";
        testService.setValidationResponse(ValidateCodeResponse.newBuilder()
                .setValid(true)
                .build());

        // Act
        ValidationResponse response = client.validateCode(validCode, "python");

        // Assert
        assertNotNull(response);
        assertTrue(response.getValid());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void testValidateCode_InvalidPythonCode() {
        // Arrange
        String invalidCode = "def strategy(data):\nreturn 'BUY'";  // Indentation error
        testService.setValidationResponse(ValidateCodeResponse.newBuilder()
                .setValid(false)
                .addErrors("IndentationError: expected an indented block")
                .addWarnings("Missing SYMBOL constant")
                .addSuggestions("Add SYMBOL = 'AAPL' at the top of your code")
                .build());

        // Act
        ValidationResponse response = client.validateCode(invalidCode, "python");

        // Assert
        assertNotNull(response);
        assertFalse(response.getValid());
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().get(0).contains("IndentationError"));
        assertEquals(1, response.getWarnings().size());
        assertEquals(1, response.getSuggestions().size());
    }

    @Test
    void testValidateCode_ServiceError() {
        // Arrange
        testService.setValidationError(Status.INTERNAL.withDescription("Validation engine crashed"));

        // Act
        ValidationResponse response = client.validateCode("def strategy(data): return 'BUY'", "python");

        // Assert
        assertNotNull(response);
        assertFalse(response.getValid());
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().get(0).contains("Validation service unavailable"));
    }

    // ========================
    // Strategy Execution Tests
    // ========================

    @Test
    void testExecuteStrategy_SuccessWithSignals() {
        // Arrange
        String strategyCode = """
                SYMBOL = 'AAPL'
                def strategy(data):
                    if data['rsi'].iloc[-1] < 30:
                        return 'BUY'
                    return 'HOLD'
                """;

        List<MarketDataBar> marketData = createTestMarketData();

        testService.setExecutionResponse(ExecuteStrategyResponse.newBuilder()
                .setSuccess(true)
                .setExecutionTimeMs(150)
                .addLogs("Strategy executed successfully")
                .addSignals(io.strategiz.execution.grpc.Signal.newBuilder()
                        .setTimestamp("2024-01-15T10:30:00Z")
                        .setType("BUY")
                        .setPrice(150.50)
                        .setQuantity(100)
                        .setReason("RSI oversold (28.5)")
                        .build())
                .build());

        // Act
        ExecutionResponse response = client.executeStrategy(
                strategyCode,
                "python",
                marketData,
                "test-user-123",
                "test-strategy-456",
                30
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(150, response.getExecutionTimeMs());
        assertEquals(1, response.getLogs().size());
        assertEquals(1, response.getSignals().size());

        Signal signal = response.getSignals().get(0);
        assertEquals("BUY", signal.getType());
        assertEquals(150.50, signal.getPrice());
        assertEquals(100, signal.getQuantity());
        assertEquals("RSI oversold (28.5)", signal.getReason());
    }

    @Test
    void testExecuteStrategy_SuccessWithPerformance() {
        // Arrange
        List<MarketDataBar> marketData = createTestMarketData();

        testService.setExecutionResponse(ExecuteStrategyResponse.newBuilder()
                .setSuccess(true)
                .setExecutionTimeMs(200)
                .setPerformance(io.strategiz.execution.grpc.Performance.newBuilder()
                        .setTotalReturn(15.5)
                        .setTotalPnl(1550.0)
                        .setWinRate(66.7)
                        .setTotalTrades(3)
                        .setProfitableTrades(2)
                        .setBuyCount(3)
                        .setSellCount(3)
                        .setAvgWin(1200.0)
                        .setAvgLoss(-600.0)
                        .setProfitFactor(2.0)
                        .setMaxDrawdown(-5.2)
                        .setSharpeRatio(1.8)
                        .setLastTestedAt("2024-01-15T10:30:00Z")
                        .addTrades(io.strategiz.execution.grpc.Trade.newBuilder()
                                .setBuyTimestamp("2024-01-10T10:00:00Z")
                                .setSellTimestamp("2024-01-11T15:00:00Z")
                                .setBuyPrice(150.0)
                                .setSellPrice(155.0)
                                .setPnl(500.0)
                                .setPnlPercent(3.33)
                                .setWin(true)
                                .setBuyReason("RSI oversold")
                                .setSellReason("Take profit hit")
                                .build())
                        .build())
                .build());

        // Act
        ExecutionResponse response = client.executeStrategy(
                "SYMBOL='AAPL'\ndef strategy(data): return 'BUY'",
                "python",
                marketData,
                "test-user",
                "test-strategy",
                30
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertNotNull(response.getPerformance());

        Performance perf = response.getPerformance();
        assertEquals(15.5, perf.getTotalReturn(), 0.01);
        assertEquals(1550.0, perf.getTotalPnl(), 0.01);
        assertEquals(66.7, perf.getWinRate(), 0.01);
        assertEquals(3, perf.getTotalTrades());
        assertEquals(2, perf.getProfitableTrades());
        assertEquals(1.8, perf.getSharpeRatio(), 0.01);

        assertEquals(1, perf.getTrades().size());
        Trade trade = perf.getTrades().get(0);
        assertEquals(150.0, trade.getBuyPrice());
        assertEquals(155.0, trade.getSellPrice());
        assertEquals(500.0, trade.getPnl());
        assertTrue(trade.isWin());
    }

    @Test
    void testExecuteStrategy_SuccessWithIndicators() {
        // Arrange
        List<MarketDataBar> marketData = createTestMarketData();

        testService.setExecutionResponse(ExecuteStrategyResponse.newBuilder()
                .setSuccess(true)
                .setExecutionTimeMs(180)
                .addIndicators(io.strategiz.execution.grpc.Indicator.newBuilder()
                        .setName("RSI")
                        .addData(io.strategiz.execution.grpc.DataPoint.newBuilder()
                                .setTimestamp("2024-01-15T10:00:00Z")
                                .setValue(28.5)
                                .build())
                        .addData(io.strategiz.execution.grpc.DataPoint.newBuilder()
                                .setTimestamp("2024-01-15T11:00:00Z")
                                .setValue(32.1)
                                .build())
                        .build())
                .build());

        // Act
        ExecutionResponse response = client.executeStrategy(
                "def strategy(data): return 'HOLD'",
                "python",
                marketData,
                "test-user",
                "test-strategy",
                30
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(1, response.getIndicators().size());

        Indicator indicator = response.getIndicators().get(0);
        assertEquals("RSI", indicator.getName());
        assertEquals(2, indicator.getData().size());
        assertEquals(28.5, indicator.getData().get(0).getValue());
        assertEquals(32.1, indicator.getData().get(1).getValue());
    }

    @Test
    void testExecuteStrategy_PythonExecutionError() {
        // Arrange
        List<MarketDataBar> marketData = createTestMarketData();

        testService.setExecutionResponse(ExecuteStrategyResponse.newBuilder()
                .setSuccess(false)
                .setExecutionTimeMs(50)
                .setError("NameError: name 'rsi' is not defined")
                .addLogs("Executing strategy...")
                .addLogs("ERROR: NameError: name 'rsi' is not defined")
                .build());

        // Act
        ExecutionResponse response = client.executeStrategy(
                "def strategy(data): return rsi",
                "python",
                marketData,
                "test-user",
                "test-strategy",
                30
        );

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertTrue(response.getError().contains("NameError"));
        assertEquals(2, response.getLogs().size());
    }

    @Test
    void testExecuteStrategy_GrpcFailure() {
        // Arrange
        testService.setExecutionError(Status.DEADLINE_EXCEEDED.withDescription("Execution timeout"));

        // Act
        ExecutionResponse response = client.executeStrategy(
                "def strategy(data): return 'BUY'",
                "python",
                createTestMarketData(),
                "test-user",
                "test-strategy",
                30
        );

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertTrue(response.getError().contains("gRPC call failed"));
        assertTrue(response.getError().contains("DEADLINE_EXCEEDED"));
    }

    // ========================
    // Helper: Create Market Data Bar
    // ========================

    @Test
    void testCreateMarketDataBar() {
        // Arrange
        Map<String, Object> data = Map.of(
                "timestamp", "2024-01-15T10:00:00Z",
                "open", 150.0,
                "high", 152.0,
                "low", 149.0,
                "close", 151.5,
                "volume", 1000000L
        );

        // Act
        MarketDataBar bar = ExecutionServiceClient.createMarketDataBar(data);

        // Assert
        assertNotNull(bar);
        assertEquals("2024-01-15T10:00:00Z", bar.getTimestamp());
        assertEquals(150.0, bar.getOpen());
        assertEquals(152.0, bar.getHigh());
        assertEquals(149.0, bar.getLow());
        assertEquals(151.5, bar.getClose());
        assertEquals(1000000L, bar.getVolume());
    }

    // ========================
    // Helper Methods
    // ========================

    private List<MarketDataBar> createTestMarketData() {
        return Arrays.asList(
                MarketDataBar.newBuilder()
                        .setTimestamp("2024-01-10T10:00:00Z")
                        .setOpen(150.0)
                        .setHigh(152.0)
                        .setLow(149.0)
                        .setClose(151.0)
                        .setVolume(1000000)
                        .build(),
                MarketDataBar.newBuilder()
                        .setTimestamp("2024-01-11T10:00:00Z")
                        .setOpen(151.0)
                        .setHigh(153.0)
                        .setLow(150.0)
                        .setClose(152.5)
                        .setVolume(1100000)
                        .build(),
                MarketDataBar.newBuilder()
                        .setTimestamp("2024-01-12T10:00:00Z")
                        .setOpen(152.5)
                        .setHigh(154.0)
                        .setLow(151.5)
                        .setClose(153.0)
                        .setVolume(1200000)
                        .build()
        );
    }

    /**
     * Test implementation of the gRPC service for testing purposes
     */
    private static class TestExecutionServiceImpl extends StrategyExecutionServiceGrpc.StrategyExecutionServiceImplBase {
        private HealthResponse healthResponse;
        private Status healthError;
        private ValidateCodeResponse validationResponse;
        private Status validationError;
        private ExecuteStrategyResponse executionResponse;
        private Status executionError;

        public void setHealthResponse(HealthResponse response) {
            this.healthResponse = response;
            this.healthError = null;
        }

        public void setHealthError(Status error) {
            this.healthError = error;
            this.healthResponse = null;
        }

        public void setValidationResponse(ValidateCodeResponse response) {
            this.validationResponse = response;
            this.validationError = null;
        }

        public void setValidationError(Status error) {
            this.validationError = error;
            this.validationResponse = null;
        }

        public void setExecutionResponse(ExecuteStrategyResponse response) {
            this.executionResponse = response;
            this.executionError = null;
        }

        public void setExecutionError(Status error) {
            this.executionError = error;
            this.executionResponse = null;
        }

        @Override
        public void getHealth(HealthRequest request, io.grpc.stub.StreamObserver<HealthResponse> responseObserver) {
            if (healthError != null) {
                responseObserver.onError(new StatusRuntimeException(healthError));
            } else {
                responseObserver.onNext(healthResponse);
                responseObserver.onCompleted();
            }
        }

        @Override
        public void validateCode(ValidateCodeRequest request, io.grpc.stub.StreamObserver<ValidateCodeResponse> responseObserver) {
            if (validationError != null) {
                responseObserver.onError(new StatusRuntimeException(validationError));
            } else {
                responseObserver.onNext(validationResponse);
                responseObserver.onCompleted();
            }
        }

        @Override
        public void executeStrategy(ExecuteStrategyRequest request, io.grpc.stub.StreamObserver<ExecuteStrategyResponse> responseObserver) {
            if (executionError != null) {
                responseObserver.onError(new StatusRuntimeException(executionError));
            } else {
                responseObserver.onNext(executionResponse);
                responseObserver.onCompleted();
            }
        }
    }
}
