package io.strategiz.service.labs.controller;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import io.strategiz.service.labs.model.StrategyResponse;
import io.strategiz.service.labs.service.CreateStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Creation", description = "Create new trading strategies")
public class CreateStrategyController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(CreateStrategyController.class);

    private final CreateStrategyService createStrategyService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public CreateStrategyController(CreateStrategyService createStrategyService,
                                  SessionAuthBusiness sessionAuthBusiness) {
        this.createStrategyService = createStrategyService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    @PostMapping
    @Operation(summary = "Create a new strategy", description = "Creates a new trading strategy for the authenticated user")
    public ResponseEntity<StrategyResponse> createStrategy(
            @Valid @RequestBody CreateStrategyRequest request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("CreateStrategy: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Create strategy authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for create strategy");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for create strategy: {}", e.getMessage());
            }
        }

        // Use query param userId as fallback for development/testing
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            logger.warn("Using userId from query parameter for development: {}", userId);
        }

        // Require authentication for creating strategies
        if (userId == null) {
            throw new IllegalStateException("Authentication required to create strategies");
        }

        logger.info("Creating new strategy: {} for user: {}", request.getName(), userId);

        try {
            // Create strategy using service
            Strategy created = createStrategyService.createStrategy(request, userId);

            // Convert to response
            StrategyResponse response = convertToResponse(created);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Failed to create strategy", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_CREATION_FAILED);
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