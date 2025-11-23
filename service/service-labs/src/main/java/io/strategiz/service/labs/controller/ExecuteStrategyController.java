package io.strategiz.service.labs.controller;

import io.strategiz.business.strategy.execution.model.*;
import io.strategiz.business.strategy.execution.service.ExecutionEngineService;
import io.strategiz.business.strategy.execution.service.StrategyExecutionService;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.service.base.controller.BaseController;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Execution", description = "Execute and backtest trading strategies")
public class ExecuteStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecuteStrategyController.class);

    private final ExecutionEngineService executionEngineService;
    private final StrategyExecutionService strategyExecutionService;
    private final ReadStrategyService readStrategyService;
    private final PythonStrategyExecutor pythonStrategyExecutor;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final MarketDataRepository marketDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ExecuteStrategyController(ExecutionEngineService executionEngineService,
                                   StrategyExecutionService strategyExecutionService,
                                   ReadStrategyService readStrategyService,
                                   PythonStrategyExecutor pythonStrategyExecutor,
                                   SessionAuthBusiness sessionAuthBusiness,
                                   MarketDataRepository marketDataRepository) {
        this.executionEngineService = executionEngineService;
        this.strategyExecutionService = strategyExecutionService;
        this.readStrategyService = readStrategyService;
        this.pythonStrategyExecutor = pythonStrategyExecutor;
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.marketDataRepository = marketDataRepository;
    }
    
    @PostMapping("/{strategyId}/execute")
    @Operation(summary = "Execute a strategy", description = "Executes a strategy with given parameters and returns signals/indicators")
    public ResponseEntity<ExecuteStrategyResponse> executeStrategy(
            @PathVariable String strategyId,
            @Valid @RequestBody ExecuteStrategyRequest request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("ExecuteStrategy: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Strategy execution authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for strategy execution");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for strategy execution: {}", e.getMessage());
            }
        }

        // Use query param userId as fallback for development/testing
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            logger.warn("Using userId from query parameter for development: {}", userId);
        }

        // Require authentication for saved strategy execution (more sensitive operation)
        if (userId == null) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_ACCESS_DENIED);
        }

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
            ExecutionRequest executionRequest = buildExecutionRequest(request, strategy, userId);

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
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("ExecuteCode: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Strategy execution authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for strategy execution");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for strategy execution: {}", e.getMessage());
            }
        }

        // Use query param userId as fallback for development/testing
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            logger.warn("Using userId from query parameter for development: {}", userId);
        }

        // Use anonymous user as last resort
        if (userId == null) {
            userId = "anonymous";
            logger.warn("No authentication found - using anonymous user for development");
        }

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

            // For Python execution
            if ("python".equalsIgnoreCase(request.getLanguage())) {
                //  TODO: Replace with real market data from database or API
                // Generate simulated market data for testing
                String ticker = request.getSymbol() != null ? request.getSymbol() : "SAMPLE";
                String marketDataJson = generateSimulatedMarketData(ticker);

                // Execute Python code with market data
                ExecuteStrategyResponse response = pythonStrategyExecutor.executePythonCode(
                    request.getCode(),
                    marketDataJson
                );

                // Set additional response fields
                response.setStrategyId("direct-execution-" + System.currentTimeMillis());
                response.setTicker(ticker);

                return ResponseEntity.ok(response);
            }

            // Fallback for other languages (use existing execution engine)
            ExecutionRequest executionRequest = buildDirectCodeExecutionRequest(request, userId);
            ExecutionResult result = executionEngineService.executeStrategy(executionRequest);
            ExecuteStrategyResponse response = convertToResponse(result, request);

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
    
    private ExecutionRequest buildExecutionRequest(ExecuteStrategyRequest request, 
                                                 io.strategiz.data.strategy.entity.Strategy strategy,
                                                 String userId) {
        ExecutionRequest executionRequest = new ExecutionRequest();
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
    
    private ExecutionRequest buildDirectCodeExecutionRequest(ExecuteStrategyRequest request, String userId) {
        ExecutionRequest executionRequest = new ExecutionRequest();
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
     * Generate simulated market data for testing strategies
     * TODO: Replace this with real market data from database or API
     */
    private String generateSimulatedMarketData(String ticker) {
        try {
            logger.info("Generating simulated market data for ticker: {}", ticker);

            java.util.List<java.util.Map<String, Object>> marketData = new java.util.ArrayList<>();
            java.time.LocalDate startDate = java.time.LocalDate.now().minusDays(90);
            double basePrice = 150.0;
            java.util.Random random = new java.util.Random();

            for (int i = 0; i < 90; i++) {
                java.time.LocalDate date = startDate.plusDays(i);
                String dateString = date.toString();

                // Generate realistic OHLC data with some volatility
                double volatility = 3.0;
                double open = basePrice + (random.nextDouble() - 0.5) * volatility;
                double close = open + (random.nextDouble() - 0.5) * volatility;
                double high = Math.max(open, close) + random.nextDouble() * volatility;
                double low = Math.min(open, close) - random.nextDouble() * volatility;

                java.util.Map<String, Object> candle = new java.util.HashMap<>();
                candle.put("time", dateString);
                candle.put("open", Math.round(open * 100.0) / 100.0);
                candle.put("high", Math.round(high * 100.0) / 100.0);
                candle.put("low", Math.round(low * 100.0) / 100.0);
                candle.put("close", Math.round(close * 100.0) / 100.0);

                marketData.add(candle);

                // Trending behavior
                basePrice = close + (random.nextDouble() - 0.48) * 2;
            }

            return objectMapper.writeValueAsString(marketData);

        } catch (Exception e) {
            logger.error("Failed to generate simulated market data for ticker: {}", ticker, e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_EXECUTION_FAILED);
        }
    }
}