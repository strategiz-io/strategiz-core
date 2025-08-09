package io.strategiz.service.strategy.controller;

import io.strategiz.business.strategy.execution.model.*;
import io.strategiz.business.strategy.execution.service.ExecutionEngineService;
import io.strategiz.business.strategy.execution.service.StrategyExecutionService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.model.ExecuteStrategyRequest;
import io.strategiz.service.strategy.model.ExecuteStrategyResponse;
import io.strategiz.service.strategy.service.ReadStrategyService;
import io.strategiz.service.strategy.constants.StrategyConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Execution", description = "Execute and backtest trading strategies")
public class ExecuteStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecuteStrategyController.class);
    
    private final ExecutionEngineService executionEngineService;
    private final StrategyExecutionService strategyExecutionService;
    private final ReadStrategyService readStrategyService;
    
    @Autowired
    public ExecuteStrategyController(ExecutionEngineService executionEngineService,
                                   StrategyExecutionService strategyExecutionService,
                                   ReadStrategyService readStrategyService) {
        this.executionEngineService = executionEngineService;
        this.strategyExecutionService = strategyExecutionService;
        this.readStrategyService = readStrategyService;
    }
    
    @PostMapping("/{strategyId}/execute")
    @Operation(summary = "Execute a strategy", description = "Executes a strategy with given parameters and returns signals/indicators")
    public ResponseEntity<ExecuteStrategyResponse> executeStrategy(
            @PathVariable String strategyId,
            @Valid @RequestBody ExecuteStrategyRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Executing strategy: {} for user: {} with symbol: {}", 
            strategyId, userId, request.getSymbol());
        
        try {
            // Get strategy from database
            java.util.Optional<io.strategiz.data.strategy.entity.Strategy> strategyOpt = readStrategyService.getStrategyById(strategyId, userId);
            if (!strategyOpt.isPresent()) {
                throw new RuntimeException("Strategy not found or access denied");
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
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Executing code for user: {} with language: {} and symbol: {}", 
            userId, request.getLanguage(), request.getSymbol());
        
        try {
            // Validate required fields for direct code execution
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Code is required for direct execution");
            }
            if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
                throw new IllegalArgumentException("Language is required");
            }
            
            // Build execution request for direct code execution
            ExecutionRequest executionRequest = buildDirectCodeExecutionRequest(request, userId);
            
            // Execute code
            ExecutionResult result = executionEngineService.executeStrategy(executionRequest);
            
            // Convert to response
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
}