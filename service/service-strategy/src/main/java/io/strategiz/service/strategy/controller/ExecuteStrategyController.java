package io.strategiz.service.strategy.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.model.ExecuteStrategyRequest;
import io.strategiz.service.strategy.model.ExecuteStrategyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategy Execution", description = "Execute and backtest trading strategies")
public class ExecuteStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecuteStrategyController.class);
    
    @PostMapping("/{strategyId}/execute")
    @Operation(summary = "Execute a strategy", description = "Executes a strategy with given parameters and returns signals/indicators")
    public ResponseEntity<ExecuteStrategyResponse> executeStrategy(
            @PathVariable String strategyId,
            @Valid @RequestBody ExecuteStrategyRequest request,
            Authentication authentication) {
        
        logger.info("Executing strategy: {} for user: {} with ticker: {}", 
            strategyId, authentication.getName(), request.getTicker());
        
        // TODO: Implement execution service
        // This would call Python/Java execution engine
        
        ExecuteStrategyResponse response = new ExecuteStrategyResponse();
        response.setStrategyId(strategyId);
        response.setTicker(request.getTicker());
        response.setExecutionTime(System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/execute-code")
    @Operation(summary = "Execute code directly", description = "Executes strategy code without saving")
    public ResponseEntity<ExecuteStrategyResponse> executeCode(
            @Valid @RequestBody ExecuteStrategyRequest request,
            Authentication authentication) {
        
        logger.info("Executing code for user: {} with language: {}", 
            authentication.getName(), request.getLanguage());
        
        // TODO: Implement execution service
        
        ExecuteStrategyResponse response = new ExecuteStrategyResponse();
        response.setTicker(request.getTicker());
        response.setExecutionTime(System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}