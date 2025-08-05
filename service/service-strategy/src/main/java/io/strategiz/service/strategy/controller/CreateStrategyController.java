package io.strategiz.service.strategy.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.model.CreateStrategyRequest;
import io.strategiz.service.strategy.model.StrategyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategy Creation", description = "Create new trading strategies")
public class CreateStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateStrategyController.class);
    
    @PostMapping
    @Operation(summary = "Create a new strategy", description = "Creates a new trading strategy for the authenticated user")
    public ResponseEntity<StrategyResponse> createStrategy(
            @Valid @RequestBody CreateStrategyRequest request,
            Authentication authentication) {
        
        logger.info("Creating new strategy: {} for user: {}", request.getName(), authentication.getName());
        
        // TODO: Implement service layer
        // For now, return a mock response
        StrategyResponse response = new StrategyResponse();
        response.setId("mock-strategy-id");
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
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}