package io.strategiz.service.strategy.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.model.CreateStrategyRequest;
import io.strategiz.service.strategy.model.StrategyResponse;
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
@Tag(name = "Strategy Update", description = "Update existing trading strategies")
public class UpdateStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateStrategyController.class);
    
    @PutMapping("/{strategyId}")
    @Operation(summary = "Update a strategy", description = "Updates an existing trading strategy")
    public ResponseEntity<StrategyResponse> updateStrategy(
            @PathVariable String strategyId,
            @Valid @RequestBody CreateStrategyRequest request,
            Authentication authentication) {
        
        logger.info("Updating strategy: {} for user: {}", strategyId, authentication.getName());
        
        // TODO: Implement service layer
        // For now, return a mock response
        StrategyResponse response = new StrategyResponse();
        response.setId(strategyId);
        response.setName(request.getName());
        response.setDescription(request.getDescription());
        response.setCode(request.getCode());
        response.setLanguage(request.getLanguage());
        response.setType(request.getType());
        response.setStatus("active");
        response.setTags(request.getTags());
        response.setUserId(authentication.getName());
        response.setPublic(request.isPublic());
        response.setParameters(request.getParameters());
        
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{strategyId}/status")
    @Operation(summary = "Update strategy status", description = "Updates the status of a strategy (active, archived, etc.)")
    public ResponseEntity<StrategyResponse> updateStrategyStatus(
            @PathVariable String strategyId,
            @RequestParam String status,
            Authentication authentication) {
        
        logger.info("Updating strategy {} status to: {} for user: {}", 
            strategyId, status, authentication.getName());
        
        // TODO: Implement service layer
        StrategyResponse response = new StrategyResponse();
        response.setId(strategyId);
        response.setStatus(status);
        
        return ResponseEntity.ok(response);
    }
}