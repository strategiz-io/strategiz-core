package io.strategiz.service.strategy.controller;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.constants.StrategyConstants;
import io.strategiz.service.strategy.model.StrategyResponse;
import io.strategiz.service.strategy.service.ReadStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Reading", description = "Read and list trading strategies")
public class ReadStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReadStrategyController.class);
    
    private final ReadStrategyService readStrategyService;
    
    @Autowired
    public ReadStrategyController(ReadStrategyService readStrategyService) {
        this.readStrategyService = readStrategyService;
    }
    
    @GetMapping
    @Operation(summary = "Get user strategies", description = "Retrieves all strategies for the authenticated user")
    public ResponseEntity<List<StrategyResponse>> getUserStrategies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Fetching strategies for user: {} with status: {} and language: {}", 
            userId, status, language);
        
        try {
            List<Strategy> strategies;
            
            if (status != null && language != null) {
                // Filter by both status and language
                strategies = readStrategyService.getUserStrategiesByStatus(userId, status)
                    .stream()
                    .filter(s -> language.equals(s.getLanguage()))
                    .collect(Collectors.toList());
            } else if (status != null) {
                strategies = readStrategyService.getUserStrategiesByStatus(userId, status);
            } else if (language != null) {
                strategies = readStrategyService.getUserStrategiesByLanguage(userId, language);
            } else {
                strategies = readStrategyService.getUserStrategies(userId);
            }
            
            List<StrategyResponse> responses = strategies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Failed to fetch strategies", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_NOT_FOUND);
        }
    }
    
    @GetMapping("/{strategyId}")
    @Operation(summary = "Get strategy by ID", description = "Retrieves a specific strategy by its ID")
    public ResponseEntity<StrategyResponse> getStrategyById(
            @PathVariable String strategyId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Fetching strategy: {} for user: {}", strategyId, userId);
        
        try {
            return readStrategyService.getStrategyById(strategyId, userId)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Strategy not found"));
        } catch (Exception e) {
            logger.error("Failed to fetch strategy", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_NOT_FOUND);
        }
    }
    
    @GetMapping("/public")
    @Operation(summary = "Get public strategies", description = "Retrieves all public strategies")
    public ResponseEntity<List<StrategyResponse>> getPublicStrategies(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) List<String> tags) {
        
        logger.info("Fetching public strategies with language: {} and tags: {}", language, tags);
        
        try {
            List<Strategy> strategies;
            
            if (language != null) {
                strategies = readStrategyService.getPublicStrategiesByLanguage(language);
            } else if (tags != null && !tags.isEmpty()) {
                strategies = readStrategyService.getPublicStrategiesByTags(tags);
            } else {
                strategies = readStrategyService.getPublicStrategies();
            }
            
            List<StrategyResponse> responses = strategies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Failed to fetch public strategies", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_NOT_FOUND);
        }
    }
    
    private StrategyResponse convertToResponse(Strategy strategy) {
        StrategyResponse response = new StrategyResponse();
        response.setId(strategy.getId());
        response.setName(strategy.getName());
        response.setDescription(strategy.getDescription());
        response.setCode(strategy.getCode());
        response.setLanguage(strategy.getLanguage());
        response.setType(strategy.getType());
        response.setStatus(strategy.getStatus());
        response.setTags(strategy.getTags());
        response.setUserId(strategy.getUserId());
        response.setPublic(strategy.isPublic());
        response.setParameters(strategy.getParameters());
        response.setBacktestResults(strategy.getBacktestResults());
        response.setPerformance(strategy.getPerformance());
        // Convert string dates to Date objects if needed
        // For now, leave them null as they'll be set by the repository
        return response;
    }
    
    @Override
    protected String getModuleName() {
        return "strategy";
    }
}