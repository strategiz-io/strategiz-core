package io.strategiz.service.labs.controller;

import io.strategiz.business.strategy.execution.model.*;
import io.strategiz.business.strategy.execution.service.ExecutionEngineService;
import io.strategiz.business.fundamentals.service.FundamentalsQueryService;
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
    private final io.strategiz.service.labs.service.StrategyExecutionService strategyExecutionService;  // Service layer for gRPC execution
    private final ReadStrategyService readStrategyService;
    private final PythonStrategyExecutor pythonStrategyExecutor;
    private final BacktestCalculatorBusiness backtestCalculatorBusiness;
    private final MarketDataRepository marketDataRepository;
    private final FundamentalsQueryService fundamentalsQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ExecuteStrategyController(ExecutionEngineService executionEngineService,
                                   io.strategiz.service.labs.service.StrategyExecutionService strategyExecutionService,
                                   ReadStrategyService readStrategyService,
                                   PythonStrategyExecutor pythonStrategyExecutor,
                                   BacktestCalculatorBusiness backtestCalculatorBusiness,
                                   MarketDataRepository marketDataRepository,
                                   FundamentalsQueryService fundamentalsQueryService) {
        this.executionEngineService = executionEngineService;
        this.strategyExecutionService = strategyExecutionService;
        this.readStrategyService = readStrategyService;
        this.pythonStrategyExecutor = pythonStrategyExecutor;
        this.backtestCalculatorBusiness = backtestCalculatorBusiness;
        this.marketDataRepository = marketDataRepository;
        this.fundamentalsQueryService = fundamentalsQueryService;
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
            // Get strategy from database with access control
            // Access rules enforced by readStrategyService.getStrategyById():
            // - DRAFT + PRIVATE: Owner only
            // - PUBLISHED + PRIVATE: Owner and subscribers
            // - PUBLISHED + PUBLIC: Everyone (including non-subscribers)
            // TODO Phase 8: Consider if execution should require subscription even for PUBLIC strategies
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

            // For Python execution - delegate to service layer
            if ("python".equalsIgnoreCase(request.getLanguage())) {
                logger.info("🚀 PYTHON EXECUTION - Delegating to service layer for userId={}", userId);

                // Validate symbol is provided
                String symbol = request.getSymbol();
                if (symbol == null || symbol.trim().isEmpty()) {
                    throwModuleException(
                        ServiceStrategyErrorDetails.STRATEGY_EXECUTION_FAILED,
                        "Symbol is required for strategy execution"
                    );
                }

                // Delegate to service layer (handles market data fetching, gRPC call, and DTO mapping)
                ExecuteStrategyResponse response = strategyExecutionService.executeStrategy(
                    request.getCode(),
                    "python",
                    symbol,
                    request.getTimeframe() != null ? request.getTimeframe() : "1D",
                    userId,
                    null  // No saved strategy for direct code execution
                );

                logger.info("✅ Strategy execution completed: {} trades, {} signals",
                    response.getPerformance() != null ? response.getPerformance().getTotalTrades() : 0,
                    response.getSignals() != null ? response.getSignals().size() : 0);

                return ResponseEntity.ok(response);
            }

            // Use existing execution engine for non-Python languages
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
     * Convert MarketDataEntity to JSON candle format
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
        performance.setTestPeriod(backtest.getTestPeriod());

        // NEW: Buy-and-hold comparison metrics
        performance.setBuyAndHoldReturn(backtest.getBuyAndHoldReturn());
        performance.setBuyAndHoldReturnPercent(backtest.getBuyAndHoldReturnPercent());
        performance.setOutperformance(backtest.getOutperformance());

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

    /**
     * Fetch fundamentals data for strategy execution.
     * Returns empty JSON object if no fundamentals are available (e.g., for crypto symbols).
     *
     * @param symbol Trading symbol (e.g., "AAPL", "TSLA")
     * @return JSON string of fundamentals in format expected by Python strategies
     */
    private String fetchFundamentalsForSymbol(String symbol) {
        try {
            logger.info("Fetching fundamentals for symbol: {}", symbol);

            // Query fundamentals service (returns Map with snake_case keys)
            Map<String, Object> fundamentals = fundamentalsQueryService.getFundamentalsForStrategy(symbol);

            // Convert to JSON
            String fundamentalsJson = objectMapper.writeValueAsString(fundamentals);

            logger.info("Fetched {} fundamental metrics for symbol: {}", fundamentals.size(), symbol);
            return fundamentalsJson;

        } catch (Exception e) {
            // Fundamentals not available (e.g., crypto symbols, new stocks)
            // Return empty JSON object - Python code will handle gracefully
            logger.debug("No fundamentals available for symbol: {} ({})", symbol, e.getMessage());
            return "{}";
        }
    }
}