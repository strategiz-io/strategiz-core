package io.strategiz.service.strategy.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.model.StrategyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategy Reading", description = "Read and list trading strategies")
public class ReadStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReadStrategyController.class);
    
    @GetMapping
    @Operation(summary = "Get user strategies", description = "Retrieves all strategies for the authenticated user")
    public ResponseEntity<List<StrategyResponse>> getUserStrategies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        
        logger.info("Fetching strategies for user: {} with status: {} and language: {}", 
            authentication.getName(), status, language);
        
        // TODO: Implement service layer
        // For now, return empty list
        List<StrategyResponse> strategies = new ArrayList<>();
        
        return ResponseEntity.ok(strategies);
    }
    
    @GetMapping("/{strategyId}")
    @Operation(summary = "Get strategy by ID", description = "Retrieves a specific strategy by its ID")
    public ResponseEntity<StrategyResponse> getStrategyById(
            @PathVariable String strategyId,
            Authentication authentication) {
        
        logger.info("Fetching strategy: {} for user: {}", strategyId, authentication.getName());
        
        // TODO: Implement service layer
        // For now, return a mock response
        StrategyResponse response = new StrategyResponse();
        response.setId(strategyId);
        response.setName("Sample Strategy");
        response.setDescription("A sample trading strategy");
        response.setCode("# Sample Python code");
        response.setLanguage("python");
        response.setType("technical");
        response.setStatus("active");
        response.setUserId(authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/public")
    @Operation(summary = "Get public strategies", description = "Retrieves all public strategies")
    public ResponseEntity<List<StrategyResponse>> getPublicStrategies(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) List<String> tags) {
        
        logger.info("Fetching public strategies with language: {} and tags: {}", language, tags);
        
        // TODO: Implement service layer
        List<StrategyResponse> strategies = new ArrayList<>();
        
        return ResponseEntity.ok(strategies);
    }
}