package io.strategiz.service.labs.controller;

import io.strategiz.client.execution.ExecutionServiceClient;
import io.strategiz.client.execution.model.*;
import io.strategiz.execution.grpc.MarketDataBar;
import io.strategiz.service.labs.model.ExecuteStrategyRequest;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ExecuteStrategyController
 * Tests controller orchestration with mocked gRPC client
 */
@ExtendWith(MockitoExtension.class)
class ExecuteStrategyControllerIntegrationTest {

    @Mock
    private ExecutionServiceClient executionServiceClient;

    @InjectMocks
    private ExecuteStrategyController controller;

    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        // Mock dependencies that ExecuteStrategyController needs
        // (These would normally be injected by Spring)
        ReflectionTestUtils.setField(controller, "executionServiceClient", executionServiceClient);
    }

    // ========================
    // Python Execution Tests
    // ========================

    @Test
    void testExecutePythonStrategy_Success() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("SYMBOL='AAPL'\ndef strategy(data): return 'BUY'");
        request.setLanguage("python");
        request.setSymbol("AAPL");
        request.setTimeframe("1Day");

        // Mock gRPC response
        ExecutionResponse grpcResponse = ExecutionResponse.builder()
                .success(true)
                .executionTimeMs(150)
                .logs(List.of("Strategy executed successfully"))
                .signals(List.of(
                        Signal.builder()
                                .timestamp("2024-01-15T10:00:00Z")
                                .type("BUY")
                                .price(150.50)
                                .quantity(100)
                                .reason("Strategy signal")
                                .build()
                ))
                .build();

        when(executionServiceClient.executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        )).thenReturn(grpcResponse);

        // Act
        ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                request,
                null,  // AuthenticatedUser - would be injected by @AuthUser in real scenario
                TEST_USER_ID
        );

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        ExecuteStrategyResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("AAPL", body.getSymbol());
        assertEquals(150, body.getExecutionTime());
        assertTrue(body.getLogs().contains("Strategy executed successfully"));

        // Verify gRPC client was called
        verify(executionServiceClient, times(1)).executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        );
    }

    @Test
    void testExecutePythonStrategy_WithPerformance() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("SYMBOL='AAPL'\ndef strategy(data): return 'BUY'");
        request.setLanguage("python");
        request.setSymbol("AAPL");
        request.setTimeframe("1Day");

        // Mock gRPC response with performance metrics
        ExecutionResponse grpcResponse = ExecutionResponse.builder()
                .success(true)
                .executionTimeMs(200)
                .performance(Performance.builder()
                        .totalReturn(15.5)
                        .totalPnl(1550.0)
                        .winRate(66.7)
                        .totalTrades(3)
                        .profitableTrades(2)
                        .sharpeRatio(1.8)
                        .maxDrawdown(-5.2)
                        .trades(List.of(
                                Trade.builder()
                                        .buyTimestamp("2024-01-10T10:00:00Z")
                                        .sellTimestamp("2024-01-11T15:00:00Z")
                                        .buyPrice(150.0)
                                        .sellPrice(155.0)
                                        .pnl(500.0)
                                        .pnlPercent(3.33)
                                        .win(true)
                                        .build()
                        ))
                        .build())
                .build();

        when(executionServiceClient.executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        )).thenReturn(grpcResponse);

        // Act
        ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                request,
                null,
                TEST_USER_ID
        );

        // Assert
        assertNotNull(response);
        ExecuteStrategyResponse body = response.getBody();
        assertNotNull(body);

        // Verify performance metrics were converted correctly
        assertNotNull(body.getPerformance());
        assertEquals(15.5, body.getPerformance().getTotalReturn(), 0.01);
        assertEquals(3, body.getPerformance().getTotalTrades());
        assertEquals(66.7, body.getPerformance().getWinRate(), 0.01);

        // Verify trades
        assertNotNull(body.getTrades());
        assertEquals(1, body.getTrades().size());
        assertEquals(500.0, body.getTrades().get(0).getPnl(), 0.01);
    }

    @Test
    void testExecutePythonStrategy_WithIndicators() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("SYMBOL='AAPL'\ndef strategy(data): data['rsi'] = 50.0; return 'HOLD'");
        request.setLanguage("python");
        request.setSymbol("AAPL");
        request.setTimeframe("1Day");

        // Mock gRPC response with indicators
        ExecutionResponse grpcResponse = ExecutionResponse.builder()
                .success(true)
                .executionTimeMs(180)
                .indicators(List.of(
                        Indicator.builder()
                                .name("RSI")
                                .data(List.of(
                                        DataPoint.builder()
                                                .timestamp("2024-01-15T10:00:00Z")
                                                .value(28.5)
                                                .build(),
                                        DataPoint.builder()
                                                .timestamp("2024-01-15T11:00:00Z")
                                                .value(32.1)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(executionServiceClient.executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        )).thenReturn(grpcResponse);

        // Act
        ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                request,
                null,
                TEST_USER_ID
        );

        // Assert
        assertNotNull(response);
        ExecuteStrategyResponse body = response.getBody();
        assertNotNull(body);

        // Verify indicators were converted
        assertNotNull(body.getIndicators());
        assertTrue(body.getIndicators().containsKey("RSI"));
        List<?> rsiData = body.getIndicators().get("RSI");
        assertEquals(2, rsiData.size());
    }

    @Test
    void testExecutePythonStrategy_ExecutionError() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("SYMBOL='AAPL'\ndef strategy(data): return undefined_variable");
        request.setLanguage("python");
        request.setSymbol("AAPL");
        request.setTimeframe("1Day");

        // Mock gRPC error response
        ExecutionResponse grpcResponse = ExecutionResponse.builder()
                .success(false)
                .executionTimeMs(50)
                .error("NameError: name 'undefined_variable' is not defined")
                .logs(List.of("ERROR: NameError"))
                .build();

        when(executionServiceClient.executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        )).thenReturn(grpcResponse);

        // Act & Assert
        // Controller should handle error and either return error response or throw exception
        try {
            ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                    request,
                    null,
                    TEST_USER_ID
            );

            // If controller returns error in response body
            assertNotNull(response);
            ExecuteStrategyResponse body = response.getBody();
            assertNotNull(body);
            // Error should be communicated to client somehow

        } catch (Exception e) {
            // If controller throws exception, that's also acceptable
            assertTrue(e.getMessage().contains("NameError") ||
                    e.getMessage().contains("undefined"));
        }

        // Verify gRPC client was called
        verify(executionServiceClient, times(1)).executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        );
    }

    @Test
    void testExecutePythonStrategy_MissingSymbol() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("def strategy(data): return 'BUY'");
        request.setLanguage("python");
        request.setSymbol(null);  // Missing symbol
        request.setTimeframe("1Day");

        // Act & Assert
        // Controller should reject request without symbol
        try {
            ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                    request,
                    null,
                    TEST_USER_ID
            );

            // Should fail validation
            fail("Expected exception for missing symbol");

        } catch (Exception e) {
            // Expected - symbol is required
            assertTrue(e.getMessage().toLowerCase().contains("symbol") ||
                    e.getMessage().toLowerCase().contains("required"));
        }

        // gRPC client should NOT be called
        verify(executionServiceClient, never()).executeStrategy(
                anyString(),
                anyString(),
                anyList(),
                anyString(),
                anyString(),
                anyInt()
        );
    }

    @Test
    void testExecutePythonStrategy_MarketDataFetch() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("SYMBOL='AAPL'\ndef strategy(data): return 'BUY'");
        request.setLanguage("python");
        request.setSymbol("AAPL");
        request.setTimeframe("1Day");

        // Mock successful gRPC execution
        ExecutionResponse grpcResponse = ExecutionResponse.builder()
                .success(true)
                .executionTimeMs(100)
                .build();

        when(executionServiceClient.executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        )).thenReturn(grpcResponse);

        // Act
        ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                request,
                null,
                TEST_USER_ID
        );

        // Assert
        assertNotNull(response);

        // Verify gRPC client received market data
        ArgumentCaptor<List<MarketDataBar>> marketDataCaptor = ArgumentCaptor.forClass(List.class);
        verify(executionServiceClient).executeStrategy(
                anyString(),
                eq("python"),
                marketDataCaptor.capture(),
                eq(TEST_USER_ID),
                anyString(),
                anyInt()
        );

        List<MarketDataBar> capturedMarketData = marketDataCaptor.getValue();
        assertNotNull(capturedMarketData);
        // Market data should have been fetched from TimescaleDB or other source
        assertTrue(capturedMarketData.size() > 0, "Market data should be fetched for symbol");
    }

    @Test
    void testExecutePythonStrategy_TimeoutParameter() {
        // Arrange
        ExecuteStrategyRequest request = new ExecuteStrategyRequest();
        request.setCode("SYMBOL='AAPL'\ndef strategy(data): return 'BUY'");
        request.setLanguage("python");
        request.setSymbol("AAPL");
        request.setTimeframe("1Day");
        request.setTimeout(60);  // Custom timeout

        ExecutionResponse grpcResponse = ExecutionResponse.builder()
                .success(true)
                .executionTimeMs(100)
                .build();

        when(executionServiceClient.executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                eq(60)  // Verify timeout is passed
        )).thenReturn(grpcResponse);

        // Act
        ResponseEntity<ExecuteStrategyResponse> response = controller.executeStrategy(
                request,
                null,
                TEST_USER_ID
        );

        // Assert
        assertNotNull(response);

        // Verify timeout was passed to gRPC client
        verify(executionServiceClient).executeStrategy(
                anyString(),
                eq("python"),
                anyList(),
                eq(TEST_USER_ID),
                anyString(),
                eq(60)
        );
    }

    // ========================
    // Helper Methods
    // ========================

    @Test
    void testCreateMarketDataBar_ValidData() {
        // Arrange
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", "2024-01-15T10:00:00Z");
        data.put("open", 150.0);
        data.put("high", 152.0);
        data.put("low", 149.0);
        data.put("close", 151.5);
        data.put("volume", 1000000L);

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

    @Test
    void testCreateMarketDataBar_NumericConversion() {
        // Arrange - Test with different numeric types (Integer, Double, etc.)
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", "2024-01-15T10:00:00Z");
        data.put("open", Integer.valueOf(150));  // Integer instead of Double
        data.put("high", Float.valueOf(152.0f));  // Float instead of Double
        data.put("low", Double.valueOf(149.0));
        data.put("close", Long.valueOf(151));
        data.put("volume", Integer.valueOf(1000000));  // Integer instead of Long

        // Act
        MarketDataBar bar = ExecutionServiceClient.createMarketDataBar(data);

        // Assert - Should handle numeric conversions
        assertNotNull(bar);
        assertEquals(150.0, bar.getOpen());
        assertEquals(152.0, bar.getHigh(), 0.01);
        assertEquals(149.0, bar.getLow());
        assertEquals(151.0, bar.getClose());
        assertEquals(1000000L, bar.getVolume());
    }
}
