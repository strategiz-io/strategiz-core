package io.strategiz.service.labs.controller;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.model.StrategyResponse;
import io.strategiz.service.labs.service.ReadStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Reading", description = "Read and list trading strategies")
public class ReadStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReadStrategyController.class);

    private final ReadStrategyService readStrategyService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public ReadStrategyController(ReadStrategyService readStrategyService,
                                 SessionAuthBusiness sessionAuthBusiness) {
        this.readStrategyService = readStrategyService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    @GetMapping
    @Operation(summary = "Get user strategies", description = "Retrieves all strategies for the authenticated user")
    public ResponseEntity<List<StrategyResponse>> getUserStrategies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("GetUserStrategies: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Get strategies authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for get strategies");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for get strategies: {}", e.getMessage());
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
            StrategyResponse response = readStrategyService.getStrategyById(strategyId, userId)
                .map(this::convertToResponse)
                .orElse(null);

            if (response == null) {
                throwModuleException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, strategyId);
            }

            return ResponseEntity.ok(response);
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