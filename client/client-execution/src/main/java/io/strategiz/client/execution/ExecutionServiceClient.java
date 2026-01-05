package io.strategiz.client.execution;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.strategiz.client.execution.model.*;
import io.strategiz.execution.grpc.ExecuteStrategyRequest;
import io.strategiz.execution.grpc.ExecuteStrategyResponse;
import io.strategiz.execution.grpc.HealthRequest;
import io.strategiz.execution.grpc.HealthResponse;
import io.strategiz.execution.grpc.MarketDataBar;
import io.strategiz.execution.grpc.StrategyExecutionServiceGrpc;
import io.strategiz.execution.grpc.ValidateCodeRequest;
import io.strategiz.execution.grpc.ValidateCodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * gRPC client for Python strategy execution service.
 * Handles communication with the isolated Python execution environment.
 */
@Component
public class ExecutionServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionServiceClient.class);

    @Value("${strategiz.execution.service.host:strategiz-execution-43628135674.us-east1.run.app}")
    private String executionServiceHost;

    @Value("${strategiz.execution.service.port:443}")
    private int executionServicePort;

    @Value("${strategiz.execution.service.use-tls:true}")
    private boolean useTls;

    private ManagedChannel channel;
    private StrategyExecutionServiceGrpc.StrategyExecutionServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        logger.info("Initializing gRPC client for execution service: {}:{}", executionServiceHost, executionServicePort);

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forAddress(executionServiceHost, executionServicePort)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .idleTimeout(5, TimeUnit.MINUTES);

        if (useTls) {
            channelBuilder.useTransportSecurity();
        } else {
            channelBuilder.usePlaintext();
        }

        channel = channelBuilder.build();
        blockingStub = StrategyExecutionServiceGrpc.newBlockingStub(channel);

        logger.info("✅ gRPC client initialized for {}:{}", executionServiceHost, executionServicePort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                logger.info("gRPC channel shutdown complete");
            } catch (InterruptedException e) {
                logger.warn("gRPC channel shutdown interrupted", e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Execute a strategy via gRPC
     *
     * @param code Strategy code
     * @param language Programming language ("python")
     * @param marketData Market data bars (OHLCV)
     * @param userId User ID for tracking
     * @param strategyId Strategy ID for tracking
     * @param timeoutSeconds Max execution time
     * @return Execution response with signals, indicators, and performance
     */
    public ExecutionResponse executeStrategy(
            String code,
            String language,
            List<MarketDataBar> marketData,
            String userId,
            String strategyId,
            int timeoutSeconds) {

        logger.info("Executing strategy via gRPC: strategyId={}, language={}, dataPoints={}",
            strategyId, language, marketData.size());

        long startTime = System.currentTimeMillis();

        try {
            ExecuteStrategyRequest.Builder requestBuilder = ExecuteStrategyRequest.newBuilder()
                    .setCode(code)
                    .setLanguage(language)
                    .addAllMarketData(marketData)
                    .setUserId(userId)
                    .setStrategyId(strategyId)
                    .setTimeoutSeconds(timeoutSeconds);

            ExecuteStrategyRequest request = requestBuilder.build();

            // Call gRPC service
            ExecuteStrategyResponse grpcResponse = blockingStub
                    .withDeadlineAfter(timeoutSeconds + 5, TimeUnit.SECONDS)
                    .executeStrategy(request);

            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Strategy execution completed: success={}, executionTime={}ms",
                    grpcResponse.getSuccess(), executionTime);

            // Convert gRPC response to domain model
            return convertResponse(grpcResponse);

        } catch (StatusRuntimeException e) {
            logger.error("gRPC call failed: status={}", e.getStatus(), e);
            return ExecutionResponse.builder()
                    .success(false)
                    .error("gRPC call failed: " + e.getStatus().getDescription())
                    .executionTimeMs((int) (System.currentTimeMillis() - startTime))
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error during strategy execution", e);
            return ExecutionResponse.builder()
                    .success(false)
                    .error("Unexpected error: " + e.getMessage())
                    .executionTimeMs((int) (System.currentTimeMillis() - startTime))
                    .build();
        }
    }

    /**
     * Validate strategy code without executing
     */
    public ValidationResponse validateCode(String code, String language) {
        logger.info("Validating code via gRPC: language={}", language);

        try {
            ValidateCodeRequest request = ValidateCodeRequest.newBuilder()
                    .setCode(code)
                    .setLanguage(language)
                    .build();

            ValidateCodeResponse grpcResponse = blockingStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .validateCode(request);

            return ValidationResponse.builder()
                    .valid(grpcResponse.getValid())
                    .errors(grpcResponse.getErrorsList())
                    .warnings(grpcResponse.getWarningsList())
                    .suggestions(grpcResponse.getSuggestionsList())
                    .build();

        } catch (StatusRuntimeException e) {
            logger.error("Code validation failed: status={}", e.getStatus(), e);
            return ValidationResponse.builder()
                    .valid(false)
                    .errors(List.of("Validation service unavailable: " + e.getStatus().getDescription()))
                    .build();
        }
    }

    /**
     * Check service health
     */
    public HealthStatus getHealth() {
        try {
            HealthRequest request = HealthRequest.newBuilder().build();

            HealthResponse grpcResponse = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getHealth(request);

            return HealthStatus.builder()
                    .status(grpcResponse.getStatus())
                    .supportedLanguages(grpcResponse.getSupportedLanguagesList())
                    .maxTimeoutSeconds(grpcResponse.getMaxTimeoutSeconds())
                    .maxMemoryMb(grpcResponse.getMaxMemoryMb())
                    .metadata(grpcResponse.getMetadataMap())
                    .build();

        } catch (StatusRuntimeException e) {
            logger.warn("Health check failed: status={}", e.getStatus());
            return HealthStatus.builder()
                    .status("NOT_SERVING")
                    .build();
        }
    }

    /**
     * Convert gRPC response to domain model
     */
    private ExecutionResponse convertResponse(ExecuteStrategyResponse grpcResponse) {
        ExecutionResponse.ExecutionResponseBuilder builder = ExecutionResponse.builder()
                .success(grpcResponse.getSuccess())
                .executionTimeMs(grpcResponse.getExecutionTimeMs())
                .logs(grpcResponse.getLogsList())
                .error(grpcResponse.getError());

        // Convert signals
        if (grpcResponse.getSignalsCount() > 0) {
            List<Signal> signals = grpcResponse.getSignalsList().stream()
                    .map(s -> Signal.builder()
                            .timestamp(s.getTimestamp())
                            .type(s.getType())
                            .price(s.getPrice())
                            .quantity(s.getQuantity())
                            .reason(s.getReason())
                            .build())
                    .collect(Collectors.toList());
            builder.signals(signals);
        }

        // Convert indicators
        if (grpcResponse.getIndicatorsCount() > 0) {
            List<Indicator> indicators = grpcResponse.getIndicatorsList().stream()
                    .map(i -> Indicator.builder()
                            .name(i.getName())
                            .data(i.getDataList().stream()
                                    .map(dp -> DataPoint.builder()
                                            .timestamp(dp.getTimestamp())
                                            .value(dp.getValue())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
            builder.indicators(indicators);
        }

        // Convert performance
        if (grpcResponse.hasPerformance()) {
            io.strategiz.execution.grpc.Performance grpcPerf = grpcResponse.getPerformance();
            Performance.PerformanceBuilder perfBuilder = Performance.builder()
                    .totalReturn(grpcPerf.getTotalReturn())
                    .totalPnl(grpcPerf.getTotalPnl())
                    .winRate(grpcPerf.getWinRate())
                    .totalTrades(grpcPerf.getTotalTrades())
                    .profitableTrades(grpcPerf.getProfitableTrades())
                    .buyCount(grpcPerf.getBuyCount())
                    .sellCount(grpcPerf.getSellCount())
                    .avgWin(grpcPerf.getAvgWin())
                    .avgLoss(grpcPerf.getAvgLoss())
                    .profitFactor(grpcPerf.getProfitFactor())
                    .maxDrawdown(grpcPerf.getMaxDrawdown())
                    .sharpeRatio(grpcPerf.getSharpeRatio())
                    .lastTestedAt(grpcPerf.getLastTestedAt())
                    // New fields
                    .startDate(grpcPerf.getStartDate())
                    .endDate(grpcPerf.getEndDate())
                    .testPeriod(grpcPerf.getTestPeriod())
                    .buyAndHoldReturn(grpcPerf.getBuyAndHoldReturn())
                    .buyAndHoldReturnPercent(grpcPerf.getBuyAndHoldReturnPercent())
                    .outperformance(grpcPerf.getOutperformance());

            // Convert trades
            if (grpcPerf.getTradesCount() > 0) {
                List<Trade> trades = grpcPerf.getTradesList().stream()
                        .map(t -> Trade.builder()
                                .buyTimestamp(t.getBuyTimestamp())
                                .sellTimestamp(t.getSellTimestamp())
                                .buyPrice(t.getBuyPrice())
                                .sellPrice(t.getSellPrice())
                                .pnl(t.getPnl())
                                .pnlPercent(t.getPnlPercent())
                                .win(t.getWin())
                                .buyReason(t.getBuyReason())
                                .sellReason(t.getSellReason())
                                .build())
                        .collect(Collectors.toList());
                perfBuilder.trades(trades);
            }

            // Convert equity curve
            if (grpcPerf.getEquityCurveCount() > 0) {
                List<EquityPoint> equityCurve = grpcPerf.getEquityCurveList().stream()
                        .map(ep -> EquityPoint.builder()
                                .timestamp(ep.getTimestamp())
                                .portfolioValue(ep.getPortfolioValue())
                                .type(ep.getType())
                                .build())
                        .collect(Collectors.toList());
                perfBuilder.equityCurve(equityCurve);
            }

            builder.performance(perfBuilder.build());
        }

        return builder.build();
    }

    /**
     * Create MarketDataBar from map (helper for controller)
     */
    public static MarketDataBar createMarketDataBar(Map<String, Object> data) {
        return MarketDataBar.newBuilder()
                .setTimestamp((String) data.get("timestamp"))
                .setOpen(((Number) data.get("open")).doubleValue())
                .setHigh(((Number) data.get("high")).doubleValue())
                .setLow(((Number) data.get("low")).doubleValue())
                .setClose(((Number) data.get("close")).doubleValue())
                .setVolume(((Number) data.get("volume")).longValue())
                .build();
    }
}
