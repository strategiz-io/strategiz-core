package io.strategiz.service.labs.controller;

import io.strategiz.business.strategy.execution.model.*;
import io.strategiz.business.strategy.execution.service.ExecutionEngineService;
import io.strategiz.business.strategy.execution.service.StrategyExecutionService;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.business.strategy.execution.service.BacktestCalculatorBusiness;
import io.strategiz.service.labs.model.ExecuteStrategyRequest;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import io.strategiz.service.labs.service.ReadStrategyService;
import io.strategiz.service.labs.service.PythonStrategyExecutor;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
// Temporarily disabled - gRPC code generation issues
// import io.strategiz.client.execution.ExecutionServiceClient;
// import io.strategiz.client.execution.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/strategies")
@RequireAuth(minAcr = "1")
@Tag(name = "Strategy Execution", description = "Execute and backtest trading strategies")
public class ExecuteStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecuteStrategyController.class);

    private final ExecutionEngineService executionEngineService;
    private final StrategyExecutionService strategyExecutionService;
    private final ReadStrategyService readStrategyService;
    private final PythonStrategyExecutor pythonStrategyExecutor;
    private final BacktestCalculatorBusiness backtestCalculatorBusiness;
    private final MarketDataRepository marketDataRepository;
    // Temporarily disabled - gRPC code generation issues
    // private final ExecutionServiceClient executionServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ExecuteStrategyController(ExecutionEngineService executionEngineService,
                                   StrategyExecutionService strategyExecutionService,
                                   ReadStrategyService readStrategyService,
                                   PythonStrategyExecutor pythonStrategyExecutor,
                                   BacktestCalculatorBusiness backtestCalculatorBusiness,
                                   MarketDataRepository marketDataRepository) {
        this.executionEngineService = executionEngineService;
        this.strategyExecutionService = strategyExecutionService;
        this.readStrategyService = readStrategyService;
        this.pythonStrategyExecutor = pythonStrategyExecutor;
        this.backtestCalculatorBusiness = backtestCalculatorBusiness;
        this.marketDataRepository = marketDataRepository;
        // Temporarily disabled - gRPC code generation issues
        // this.executionServiceClient = executionServiceClient;
    }
    
    @PostMapping("/{strategyId}/execute")
    @Operation(summary = "Execute a strategy", description = "Executes a strategy with given parameters and returns signals/indicators")
    public ResponseEntity<ExecuteStrategyResponse> executeStrategy(
            @PathVariable String strategyId,
            @Valid @RequestBody ExecuteStrategyRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Executing strategy: {} for user: {} with symbol: {}",
            strategyId, userId, request.getSymbol());
        
        try {
            // Get strategy from database
            java.util.Optional<io.strategiz.data.strategy.entity.Strategy> strategyOpt = readStrategyService.getStrategyById(strategyId, userId);
            if (!strategyOpt.isPresent()) {
                throwModuleException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, strategyId);
            }
            io.strategiz.data.strategy.entity.Strategy strategy = strategyOpt.get();

            // Build execution request
            io.strategiz.business.strategy.execution.model.ExecutionRequest executionRequest = buildExecutionRequest(request, strategy, userId);

            // Execute strategy
            ExecutionResult result = executionEngineService.executeStrategy(executionRequest);

            // Convert to response
            ExecuteStrategyResponse response = convertToResponse(result, request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to execute strategy: {}", strategyId, e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_EXECUTION_FAILED);
        }
    }
    
    @PostMapping("/execute-code")
    @Operation(summary = "Execute code directly", description = "Executes strategy code without saving")
    public ResponseEntity<ExecuteStrategyResponse> executeCode(
            @Valid @RequestBody ExecuteStrategyRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Executing code for user: {} with language: {} and symbol: {}",
            userId, request.getLanguage(), request.getSymbol());

        try {
            // Validate required fields for direct code execution
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                throwModuleException(ServiceStrategyErrorDetails.STRATEGY_CODE_REQUIRED);
            }
            if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
                throwModuleException(ServiceStrategyErrorDetails.STRATEGY_INVALID_LANGUAGE);
            }

            // Temporarily disabled - gRPC execution service
            // For Python execution - use gRPC execution service
            // if ("python".equalsIgnoreCase(request.getLanguage())) {
            //     // Validate symbol is provided
            //     String symbol = request.getSymbol();
            //     if (symbol == null || symbol.trim().isEmpty()) {
            //         throwModuleException(
            //             ServiceStrategyErrorDetails.STRATEGY_EXECUTION_FAILED,
            //             "Symbol is required for strategy execution"
            //         );
            //     }
            //
            //     // Fetch real market data from repository
            //     List<Map<String, Object>> marketDataList = fetchMarketDataListForSymbol(symbol);
            //
            //     // Convert market data to gRPC format
            //     List<MarketDataBar> grpcMarketData = marketDataList.stream()
            //         .map(this::convertToGrpcMarketDataBar)
            //         .collect(java.util.stream.Collectors.toList());
            //
            //     // Build gRPC execution request
            //     io.strategiz.client.execution.model.ExecutionRequest grpcRequest = io.strategiz.client.execution.model.ExecutionRequest.builder()
            //         .code(request.getCode())
            //         .language("python")
            //         .userId(userId)
            //         .strategyId("direct-execution-" + System.currentTimeMillis())
            //         .marketData(grpcMarketData)
            //         .timeoutSeconds(30)
            //         .build();
            //
            //     // Execute via gRPC
            //     io.strategiz.client.execution.model.ExecutionResponse grpcResponse = executionServiceClient.executeStrategy(grpcRequest);
            //
            //     // Convert gRPC response to REST response
            //     ExecuteStrategyResponse response = convertGrpcToRestResponse(grpcResponse, symbol);
            //
            //     return ResponseEntity.ok(response);
            // }

            // Use existing execution engine for all languages
            io.strategiz.business.strategy.execution.model.ExecutionRequest executionRequest = buildDirectCodeExecutionRequest(request, userId);
            ExecutionResult result = executionEngineService.executeStrategy(executionRequest);
            ExecuteStrategyResponse response = convertToResponse(result, request);

            // TODO: Add strategy performance tracking when strategyId is added to request

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to execute code directly", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_EXECUTION_FAILED);
        }
    }
    
    @GetMapping("/providers")
    @Operation(summary = "Get available data providers", description = "Returns list of available market data providers")
    public ResponseEntity<?> getProviders() {
        try {
            var providers = executionEngineService.getAvailableProviders();
            return ResponseEntity.ok(providers);
        } catch (Exception e) {
            logger.error("Failed to get providers", e);
            throw handleException(e, "failed-to-get-providers");
        }
    }
    
    @GetMapping("/providers/{providerId}/symbols")
    @Operation(summary = "Get supported symbols", description = "Returns supported symbols for a provider")
    public ResponseEntity<?> getSupportedSymbols(@PathVariable String providerId) {
        try {
            var symbols = executionEngineService.getSupportedSymbols(providerId);
            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            logger.error("Failed to get supported symbols for provider: {}", providerId, e);
            throw handleException(e, "failed-to-get-symbols");
        }
    }
    
    @Override
    protected String getModuleName() {
        return "strategy";
    }
    
    // Helper methods
    
    private io.strategiz.business.strategy.execution.model.ExecutionRequest buildExecutionRequest(ExecuteStrategyRequest request,
                                                 io.strategiz.data.strategy.entity.Strategy strategy,
                                                 String userId) {
        io.strategiz.business.strategy.execution.model.ExecutionRequest executionRequest = new io.strategiz.business.strategy.execution.model.ExecutionRequest();
        executionRequest.setStrategyId(strategy.getId());
        executionRequest.setCode(strategy.getCode());
        executionRequest.setLanguage(strategy.getLanguage());
        executionRequest.setUserId(userId);
        
        // Set execution parameters from request
        executionRequest.setProviderId(request.getProviderId() != null ? request.getProviderId() : "alpaca");
        executionRequest.setSymbol(request.getSymbol());
        executionRequest.setTimeframe(request.getTimeframe() != null ? request.getTimeframe() : "1D");
        // executionRequest.setMode(request.getMode() != null ? request.getMode() : ExecutionMode.BACKTEST);
        
        // Set date range - default to last 30 days if not specified
        // LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();
        // LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : endDate.minusDays(30);
        // executionRequest.setStartDate(startDate);
        // executionRequest.setEndDate(endDate);
        
        executionRequest.setParameters(request.getParameters());
        
        return executionRequest;
    }
    
    private io.strategiz.business.strategy.execution.model.ExecutionRequest buildDirectCodeExecutionRequest(ExecuteStrategyRequest request, String userId) {
        io.strategiz.business.strategy.execution.model.ExecutionRequest executionRequest = new io.strategiz.business.strategy.execution.model.ExecutionRequest();
        executionRequest.setStrategyId("direct-execution-" + System.currentTimeMillis());
        executionRequest.setCode(request.getCode());
        executionRequest.setLanguage(request.getLanguage());
        executionRequest.setUserId(userId);
        
        // Set execution parameters
        executionRequest.setProviderId(request.getProviderId() != null ? request.getProviderId() : "alpaca");
        executionRequest.setSymbol(request.getSymbol());
        executionRequest.setTimeframe(request.getTimeframe() != null ? request.getTimeframe() : "1D");
        // executionRequest.setMode(request.getMode() != null ? request.getMode() : ExecutionMode.BACKTEST);
        
        // Set date range - default to last 30 days
        // LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();
        // LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : endDate.minusDays(30);
        // executionRequest.setStartDate(startDate);
        // executionRequest.setEndDate(endDate);
        
        executionRequest.setParameters(request.getParameters());
        
        return executionRequest;
    }
    
    private ExecuteStrategyResponse convertToResponse(ExecutionResult result, ExecuteStrategyRequest request) {
        ExecuteStrategyResponse response = new ExecuteStrategyResponse();
        response.setStrategyId(result.getStrategyId());
        // response.setExecutionId(result.getExecutionId());
        // response.setStatus(result.getStatus());
        // response.setSymbol(request.getSymbol());
        // response.setExecutionTime(result.getExecutionTimeMs());
        // response.setStartTime(result.getStartTime());
        // response.setEndTime(result.getEndTime());

        // Set signals
        // TODO: Map signals properly - incompatible types
        // if (result.getSignals() != null) {
        //     response.setSignals(result.getSignals());
        // }

        // Set performance metrics
        // if (result.getMetrics() != null) {
        //     response.setMetrics(result.getMetrics());
        // }

        // Set indicators
        // TODO: Implement indicator mapping
        // if (result.getIndicators() != null) {
        //     response.setIndicators(result.getIndicators());
        // }

        // Set logs and error info
        // response.setLogs(result.getLogs());
        // response.setErrorMessage(result.getErrorMessage());

        return response;
    }

    /**
     * Fetch real market data from the repository for strategy execution
     * Fetches last 180 days of daily OHLCV data for the specified symbol
     *
     * @param symbol Trading symbol (e.g., "AAPL", "TSLA", "BTC-USD")
     * @return JSON string of market data in format expected by Python strategies
     */
    private String fetchMarketDataForSymbol(String symbol) {
        try {
            logger.info("Fetching market data for symbol: {}", symbol);

            // Calculate date range (last 180 days for chart display)
            java.time.LocalDate endDate = java.time.LocalDate.now().minusDays(1);
            java.time.LocalDate startDate = endDate.minusDays(180);

            // Query Firestore for historical data
            java.util.List<MarketDataEntity> marketData = marketDataRepository
                .findBySymbolAndDateRange(symbol, startDate, endDate);

            // If no data found, throw proper exception
            if (marketData == null || marketData.isEmpty()) {
                logger.warn("No market data found for symbol: {}", symbol);
                throwModuleException(
                    ServiceStrategyErrorDetails.MARKET_DATA_NOT_FOUND,
                    String.format("No market data found for symbol: %s. Please ensure the batch job has run to populate data.", symbol)
                );
            }

            // Convert to JSON format expected by Python strategies
            java.util.List<java.util.Map<String, Object>> jsonData = marketData.stream()
                .map(this::convertToJsonCandle)
                .collect(java.util.stream.Collectors.toList());

            logger.info("Fetched {} data points for symbol: {}", jsonData.size(), symbol);
            return objectMapper.writeValueAsString(jsonData);

        } catch (Exception e) {
            logger.error("Failed to fetch market data for symbol: {}", symbol, e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_EXECUTION_FAILED);
        }
    }

    /**
     * Convert MarketDataEntity to JSON candle format for Python
     */
    private java.util.Map<String, Object> convertToJsonCandle(MarketDataEntity entity) {
        java.util.Map<String, Object> candle = new java.util.HashMap<>();
        candle.put("time", entity.getTimestampAsLocalDateTime().toString());
        candle.put("timestamp", entity.getTimestampAsLocalDateTime().toString());
        candle.put("open", entity.getOpen().doubleValue());
        candle.put("high", entity.getHigh().doubleValue());
        candle.put("low", entity.getLow().doubleValue());
        candle.put("close", entity.getClose().doubleValue());
        candle.put("volume", entity.getVolume() != null ? entity.getVolume().longValue() : 0);
        return candle;
    }

    /**
     * Fetch market data as a list of maps for both Python execution and backtest calculation.
     */
    private List<Map<String, Object>> fetchMarketDataListForSymbol(String symbol) {
        logger.info("Fetching market data for symbol: {}", symbol);

        // Calculate date range (last 365 days for more comprehensive backtesting)
        java.time.LocalDate endDate = java.time.LocalDate.now().minusDays(1);
        java.time.LocalDate startDate = endDate.minusDays(365);

        // Query Firestore for historical data
        java.util.List<MarketDataEntity> marketData = marketDataRepository
            .findBySymbolAndDateRange(symbol, startDate, endDate);

        // If no data found, throw proper exception
        if (marketData == null || marketData.isEmpty()) {
            logger.warn("No market data found for symbol: {}", symbol);
            throwModuleException(
                ServiceStrategyErrorDetails.MARKET_DATA_NOT_FOUND,
                String.format("No market data found for symbol: %s. Please ensure the batch job has run to populate data.", symbol)
            );
        }

        // Convert to list of maps
        List<Map<String, Object>> jsonData = marketData.stream()
            .map(this::convertToJsonCandle)
            .collect(java.util.stream.Collectors.toList());

        logger.info("Fetched {} data points for symbol: {}", jsonData.size(), symbol);
        return jsonData;
    }

    /**
     * Convert business layer BacktestPerformance to response layer Performance.
     */
    private ExecuteStrategyResponse.Performance convertBacktestPerformance(
            io.strategiz.business.strategy.execution.model.BacktestPerformance backtest) {
        ExecuteStrategyResponse.Performance performance = new ExecuteStrategyResponse.Performance();
        performance.setTotalReturn(backtest.getTotalReturn());
        performance.setTotalPnL(backtest.getTotalPnL());
        performance.setWinRate(backtest.getWinRate());
        performance.setTotalTrades(backtest.getTotalTrades());
        performance.setProfitableTrades(backtest.getProfitableTrades());
        performance.setBuyCount(backtest.getBuyCount());
        performance.setSellCount(backtest.getSellCount());
        performance.setAvgWin(backtest.getAvgWin());
        performance.setAvgLoss(backtest.getAvgLoss());
        performance.setProfitFactor(backtest.getProfitFactor());
        performance.setMaxDrawdown(backtest.getMaxDrawdown());
        performance.setSharpeRatio(backtest.getSharpeRatio());
        performance.setLastTestedAt(backtest.getLastTestedAt());

        // Convert trades
        if (backtest.getTrades() != null) {
            List<ExecuteStrategyResponse.Trade> trades = backtest.getTrades().stream()
                .map(bt -> {
                    ExecuteStrategyResponse.Trade trade = new ExecuteStrategyResponse.Trade();
                    trade.setBuyTimestamp(bt.getBuyTimestamp());
                    trade.setSellTimestamp(bt.getSellTimestamp());
                    trade.setBuyPrice(bt.getBuyPrice());
                    trade.setSellPrice(bt.getSellPrice());
                    trade.setPnl(bt.getPnl());
                    trade.setPnlPercent(bt.getPnlPercent());
                    trade.setWin(bt.isWin());
                    trade.setBuyReason(bt.getBuyReason());
                    trade.setSellReason(bt.getSellReason());
                    return trade;
                })
                .collect(java.util.stream.Collectors.toList());
            performance.setTrades(trades);
        }

        return performance;
    }

    // Temporarily disabled - gRPC execution service
    // /**
    //  * Convert Map-based market data to gRPC MarketDataBar format
    //  */
    // private MarketDataBar convertToGrpcMarketDataBar(Map<String, Object> data) {
    //     return MarketDataBar.builder()
    //         .timestamp((String) data.get("timestamp"))
    //         .open(((Number) data.get("open")).doubleValue())
    //         .high(((Number) data.get("high")).doubleValue())
    //         .low(((Number) data.get("low")).doubleValue())
    //         .close(((Number) data.get("close")).doubleValue())
    //         .volume(((Number) data.get("volume")).longValue())
    //         .build();
    // }
    //
    // /**
    //  * Convert gRPC ExecutionResponse to REST ExecuteStrategyResponse
    //  */
    // private ExecuteStrategyResponse convertGrpcToRestResponse(
    //         io.strategiz.client.execution.model.ExecutionResponse grpcResponse,
    //         String symbol) {
    //     ExecuteStrategyResponse response = new ExecuteStrategyResponse();
    //
    //     // Set basic fields
    //     response.setSymbol(symbol);
    //     response.setExecutionTime(grpcResponse.getExecutionTimeMs());
    //     response.setLogs(grpcResponse.getLogs());
    //
    //     // Handle errors
    //     if (!grpcResponse.isSuccess()) {
    //         List<String> errors = new java.util.ArrayList<>();
    //         if (grpcResponse.getError() != null) {
    //             errors.add(grpcResponse.getError());
    //         }
    //         response.setErrors(errors);
    //         return response;
    //     }
    //
    //     // Convert signals
    //     if (grpcResponse.getSignals() != null) {
    //         List<ExecuteStrategyResponse.Signal> signals = grpcResponse.getSignals().stream()
    //             .map(s -> {
    //                 ExecuteStrategyResponse.Signal signal = new ExecuteStrategyResponse.Signal();
    //                 signal.setTimestamp(s.getTimestamp());
    //                 signal.setType(s.getType());
    //                 signal.setPrice(s.getPrice());
    //                 signal.setQuantity(s.getQuantity());
    //                 signal.setReason(s.getReason());
    //                 return signal;
    //             })
    //             .collect(java.util.stream.Collectors.toList());
    //         response.setSignals(signals);
    //     }
    //
    //     // Convert indicators
    //     if (grpcResponse.getIndicators() != null) {
    //         List<ExecuteStrategyResponse.Indicator> indicators = grpcResponse.getIndicators().stream()
    //             .map(i -> {
    //                 ExecuteStrategyResponse.Indicator indicator = new ExecuteStrategyResponse.Indicator();
    //                 indicator.setName(i.getName());
    //
    //                 List<ExecuteStrategyResponse.Indicator.DataPoint> dataPoints = i.getData().stream()
    //                     .map(dp -> {
    //                         ExecuteStrategyResponse.Indicator.DataPoint point = new ExecuteStrategyResponse.Indicator.DataPoint();
    //                         point.setTime(dp.getTimestamp());
    //                         point.setValue(dp.getValue());
    //                         return point;
    //                     })
    //                     .collect(java.util.stream.Collectors.toList());
    //                 indicator.setData(dataPoints);
    //
    //                 return indicator;
    //             })
    //             .collect(java.util.stream.Collectors.toList());
    //         response.setIndicators(indicators);
    //     }
    //
    //     // Convert performance
    //     if (grpcResponse.getPerformance() != null) {
    //         io.strategiz.client.execution.model.Performance grpcPerf = grpcResponse.getPerformance();
    //         ExecuteStrategyResponse.Performance performance = new ExecuteStrategyResponse.Performance();
    //
    //         performance.setTotalReturn(grpcPerf.getTotalReturn());
    //         performance.setTotalPnL(grpcPerf.getTotalPnl());
    //         performance.setWinRate(grpcPerf.getWinRate());
    //         performance.setTotalTrades(grpcPerf.getTotalTrades());
    //         performance.setProfitableTrades(grpcPerf.getProfitableTrades());
    //         performance.setBuyCount(grpcPerf.getBuyCount());
    //         performance.setSellCount(grpcPerf.getSellCount());
    //         performance.setAvgWin(grpcPerf.getAvgWin());
    //         performance.setAvgLoss(grpcPerf.getAvgLoss());
    //         performance.setProfitFactor(grpcPerf.getProfitFactor());
    //         performance.setMaxDrawdown(grpcPerf.getMaxDrawdown());
    //         performance.setSharpeRatio(grpcPerf.getSharpeRatio());
    //         performance.setLastTestedAt(grpcPerf.getLastTestedAt());
    //
    //         // Convert trades
    //         if (grpcPerf.getTrades() != null) {
    //             List<ExecuteStrategyResponse.Trade> trades = grpcPerf.getTrades().stream()
    //                 .map(t -> {
    //                     ExecuteStrategyResponse.Trade trade = new ExecuteStrategyResponse.Trade();
    //                     trade.setBuyTimestamp(t.getBuyTimestamp());
    //                     trade.setSellTimestamp(t.getSellTimestamp());
    //                     trade.setBuyPrice(t.getBuyPrice());
    //                     trade.setSellPrice(t.getSellPrice());
    //                     trade.setPnl(t.getPnl());
    //                     trade.setPnlPercent(t.getPnlPercent());
    //                     trade.setWin(t.isWin());
    //                     trade.setBuyReason(t.getBuyReason());
    //                     trade.setSellReason(t.getSellReason());
    //                     return trade;
    //                 })
    //                 .collect(java.util.stream.Collectors.toList());
    //             performance.setTrades(trades);
    //         }
    //
    //         response.setPerformance(performance);
    //     }
    //
    //     return response;
    // }

    /**
     * Convert Performance object to Map for Firestore storage
     */
    private Map<String, Object> convertPerformanceToMap(ExecuteStrategyResponse.Performance performance) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("totalReturn", performance.getTotalReturn());
        map.put("totalPnL", performance.getTotalPnL());
        map.put("winRate", performance.getWinRate());
        map.put("totalTrades", performance.getTotalTrades());
        map.put("profitableTrades", performance.getProfitableTrades());
        map.put("buyCount", performance.getBuyCount());
        map.put("sellCount", performance.getSellCount());
        map.put("avgWin", performance.getAvgWin());
        map.put("avgLoss", performance.getAvgLoss());
        map.put("profitFactor", performance.getProfitFactor());
        map.put("maxDrawdown", performance.getMaxDrawdown());
        map.put("sharpeRatio", performance.getSharpeRatio());
        map.put("lastTestedAt", performance.getLastTestedAt());
        return map;
    }
}