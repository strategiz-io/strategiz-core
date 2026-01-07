package io.strategiz.client.execution;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.strategiz.client.execution.model.*;
import io.strategiz.execution.grpc.DeploymentExecution;
import io.strategiz.execution.grpc.ExecuteListRequest;
import io.strategiz.execution.grpc.ExecuteListResponse;
import io.strategiz.execution.grpc.ExecuteStrategyRequest;
import io.strategiz.execution.grpc.ExecuteStrategyResponse;
import io.strategiz.execution.grpc.HealthRequest;
import io.strategiz.execution.grpc.HealthResponse;
import io.strategiz.execution.grpc.MarketDataBar;
import io.strategiz.execution.grpc.StrategyExecutionServiceGrpc;
import io.strategiz.execution.grpc.SymbolMarketData;
import io.strategiz.execution.grpc.SymbolSetExecution;
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

    // ================================================================================
    // BATCH EXECUTION METHODS (for live alerts/bots)
    // ================================================================================

    /**
     * Execute multiple strategies in batch for live processing.
     * Used by SymbolSetProcessorJob to evaluate alerts and bots.
     *
     * @param symbolSets List of symbol sets with their deployments and market data
     * @param tier Subscription tier (TIER1, TIER2, TIER3)
     * @param timeoutSeconds Max execution time per strategy
     * @return Batch execution response with results per deployment
     */
    public io.strategiz.client.execution.model.ExecuteListResponse executeList(
            List<SymbolSetExecutionData> symbolSets,
            String tier,
            int timeoutSeconds) {

        logger.info("Executing batch via gRPC: tier={}, symbolSets={}, timeout={}s",
                tier, symbolSets.size(), timeoutSeconds);

        long startTime = System.currentTimeMillis();

        try {
            // Build request
            ExecuteListRequest.Builder requestBuilder = ExecuteListRequest.newBuilder()
                    .setTier(tier)
                    .setTimeoutSeconds(timeoutSeconds);

            // Add symbol sets
            for (SymbolSetExecutionData symbolSet : symbolSets) {
                requestBuilder.addSymbolSets(buildSymbolSetExecution(symbolSet));
            }

            ExecuteListRequest request = requestBuilder.build();

            // Calculate total timeout based on number of deployments
            int totalDeployments = symbolSets.stream()
                    .mapToInt(s -> s.getDeployments().size())
                    .sum();
            int batchTimeout = Math.max(timeoutSeconds * 2, timeoutSeconds + (totalDeployments / 10));

            // Call gRPC service
            ExecuteListResponse grpcResponse = blockingStub
                    .withDeadlineAfter(batchTimeout, TimeUnit.SECONDS)
                    .executeList(request);

            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Batch execution completed: success={}, total={}, successful={}, failed={}, time={}ms",
                    grpcResponse.getSuccess(),
                    grpcResponse.getTotalDeployments(),
                    grpcResponse.getSuccessfulDeployments(),
                    grpcResponse.getFailedDeployments(),
                    executionTime);

            // Convert gRPC response to domain model
            return convertListResponse(grpcResponse);

        } catch (StatusRuntimeException e) {
            logger.error("gRPC batch call failed: status={}", e.getStatus(), e);
            return io.strategiz.client.execution.model.ExecuteListResponse.builder()
                    .success(false)
                    .error("gRPC call failed: " + e.getStatus().getDescription())
                    .executionTimeMs((int) (System.currentTimeMillis() - startTime))
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error during batch execution", e);
            return io.strategiz.client.execution.model.ExecuteListResponse.builder()
                    .success(false)
                    .error("Unexpected error: " + e.getMessage())
                    .executionTimeMs((int) (System.currentTimeMillis() - startTime))
                    .build();
        }
    }

    /**
     * Build gRPC SymbolSetExecution from domain model.
     */
    private SymbolSetExecution buildSymbolSetExecution(SymbolSetExecutionData data) {
        SymbolSetExecution.Builder builder = SymbolSetExecution.newBuilder()
                .addAllSymbols(data.getSymbols());

        // Add market data per symbol
        for (Map.Entry<String, List<MarketDataBar>> entry : data.getMarketData().entrySet()) {
            SymbolMarketData symbolData = SymbolMarketData.newBuilder()
                    .setSymbol(entry.getKey())
                    .addAllBars(entry.getValue())
                    .build();
            builder.putMarketData(entry.getKey(), symbolData);
        }

        // Add deployments
        for (DeploymentExecutionData deployment : data.getDeployments()) {
            DeploymentExecution.Builder depBuilder = DeploymentExecution.newBuilder()
                    .setDeploymentId(deployment.getDeploymentId())
                    .setDeploymentType(deployment.getDeploymentType())
                    .setStrategyId(deployment.getStrategyId())
                    .setStrategyCode(deployment.getStrategyCode())
                    .setUserId(deployment.getUserId());

            if (deployment.getParameters() != null) {
                depBuilder.putAllParameters(deployment.getParameters());
            }

            builder.addDeployments(depBuilder.build());
        }

        return builder.build();
    }

    /**
     * Convert gRPC ExecuteListResponse to domain model.
     */
    private io.strategiz.client.execution.model.ExecuteListResponse convertListResponse(
            ExecuteListResponse grpcResponse) {

        List<io.strategiz.client.execution.model.DeploymentResult> results = grpcResponse.getResultsList().stream()
                .map(this::convertDeploymentResult)
                .collect(Collectors.toList());

        return io.strategiz.client.execution.model.ExecuteListResponse.builder()
                .success(grpcResponse.getSuccess())
                .results(results)
                .totalDeployments(grpcResponse.getTotalDeployments())
                .successfulDeployments(grpcResponse.getSuccessfulDeployments())
                .failedDeployments(grpcResponse.getFailedDeployments())
                .executionTimeMs(grpcResponse.getExecutionTimeMs())
                .error(grpcResponse.getError())
                .build();
    }

    /**
     * Convert gRPC DeploymentResult to domain model.
     */
    private io.strategiz.client.execution.model.DeploymentResult convertDeploymentResult(
            io.strategiz.execution.grpc.DeploymentResult grpcResult) {

        io.strategiz.client.execution.model.DeploymentResult.Builder builder =
                io.strategiz.client.execution.model.DeploymentResult.builder()
                        .deploymentId(grpcResult.getDeploymentId())
                        .deploymentType(grpcResult.getDeploymentType())
                        .success(grpcResult.getSuccess())
                        .executionTimeMs(grpcResult.getExecutionTimeMs())
                        .error(grpcResult.getError());

        // Convert signal if present
        if (grpcResult.hasSignal()) {
            io.strategiz.execution.grpc.LiveSignal grpcSignal = grpcResult.getSignal();
            builder.signal(io.strategiz.client.execution.model.LiveSignal.builder()
                    .signalType(grpcSignal.getSignalType())
                    .symbol(grpcSignal.getSymbol())
                    .price(grpcSignal.getPrice())
                    .quantity(grpcSignal.getQuantity())
                    .reason(grpcSignal.getReason())
                    .indicators(grpcSignal.getIndicatorsMap())
                    .timestamp(grpcSignal.getTimestamp())
                    .build());
        }

        return builder.build();
    }

    // ================================================================================
    // DATA CLASSES FOR BATCH EXECUTION
    // ================================================================================

    /**
     * Data for a symbol set to execute.
     */
    public static class SymbolSetExecutionData {
        private final List<String> symbols;
        private final Map<String, List<MarketDataBar>> marketData;
        private final List<DeploymentExecutionData> deployments;

        public SymbolSetExecutionData(List<String> symbols,
                                       Map<String, List<MarketDataBar>> marketData,
                                       List<DeploymentExecutionData> deployments) {
            this.symbols = symbols;
            this.marketData = marketData;
            this.deployments = deployments;
        }

        public List<String> getSymbols() {
            return symbols;
        }

        public Map<String, List<MarketDataBar>> getMarketData() {
            return marketData;
        }

        public List<DeploymentExecutionData> getDeployments() {
            return deployments;
        }
    }

    /**
     * Data for a deployment to execute.
     */
    public static class DeploymentExecutionData {
        private final String deploymentId;
        private final String deploymentType;
        private final String strategyId;
        private final String strategyCode;
        private final String userId;
        private final Map<String, String> parameters;

        public DeploymentExecutionData(String deploymentId, String deploymentType,
                                        String strategyId, String strategyCode,
                                        String userId, Map<String, String> parameters) {
            this.deploymentId = deploymentId;
            this.deploymentType = deploymentType;
            this.strategyId = strategyId;
            this.strategyCode = strategyCode;
            this.userId = userId;
            this.parameters = parameters;
        }

        public String getDeploymentId() {
            return deploymentId;
        }

        public String getDeploymentType() {
            return deploymentType;
        }

        public String getStrategyId() {
            return strategyId;
        }

        public String getStrategyCode() {
            return strategyCode;
        }

        public String getUserId() {
            return userId;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }
    }
}
