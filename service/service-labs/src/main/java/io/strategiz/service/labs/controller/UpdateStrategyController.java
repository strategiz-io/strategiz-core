package io.strategiz.service.labs.controller;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import io.strategiz.service.labs.model.StrategyResponse;
import io.strategiz.service.labs.service.UpdateStrategyService;
import io.strategiz.service.labs.service.ReadStrategyService;
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

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Update", description = "Update existing trading strategies")
public class UpdateStrategyController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStrategyController.class);

    private final UpdateStrategyService updateStrategyService;
    private final ReadStrategyService readStrategyService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public UpdateStrategyController(UpdateStrategyService updateStrategyService,
                                  ReadStrategyService readStrategyService,
                                  SessionAuthBusiness sessionAuthBusiness) {
        this.updateStrategyService = updateStrategyService;
        this.readStrategyService = readStrategyService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    @PutMapping("/{strategyId}")
    @Operation(summary = "Update a strategy", description = "Updates an existing trading strategy")
    public ResponseEntity<StrategyResponse> updateStrategy(
            @PathVariable String strategyId,
            @Valid @RequestBody CreateStrategyRequest request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("UpdateStrategy: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Update strategy authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for update strategy");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for update strategy: {}", e.getMessage());
            }
        }

        // Use query param userId as fallback for development/testing
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            logger.warn("Using userId from query parameter for development: {}", userId);
        }

        // Require authentication
        if (userId == null) {
            throw new IllegalStateException("Authentication required to update strategies");
        }

        logger.info("Updating strategy: {} for user: {}", strategyId, userId);

        try {
            // Update strategy using service
            Strategy updated = updateStrategyService.updateStrategy(strategyId, userId, request);

            // Convert to response
            StrategyResponse response = convertToResponse(updated);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update strategy", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_UPDATE_FAILED);
        }
    }

    @PatchMapping("/{strategyId}/status")
    @Operation(summary = "Update strategy status", description = "Updates the status of a strategy (active, archived, etc.)")
    public ResponseEntity<StrategyResponse> updateStrategyStatus(
            @PathVariable String strategyId,
            @RequestParam String status,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("UpdateStrategyStatus: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Update strategy status authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for update strategy status");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for update strategy status: {}", e.getMessage());
            }
        }

        // Use query param userId as fallback for development/testing
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            logger.warn("Using userId from query parameter for development: {}", userId);
        }

        // Require authentication
        if (userId == null) {
            throw new IllegalStateException("Authentication required to update strategy status");
        }

        logger.info("Updating strategy {} status to: {} for user: {}",
            strategyId, status, userId);

        try {
            // Update status using service
            boolean updated = updateStrategyService.updateStrategyStatus(strategyId, userId, status);

            if (!updated) {
                throw new RuntimeException("Strategy not found or access denied");
            }

            // Fetch updated strategy to return
            Strategy strategy = readStrategyService.getStrategyById(strategyId, userId)
                .orElseThrow(() -> new RuntimeException("Strategy not found"));

            return ResponseEntity.ok(convertToResponse(strategy));
        } catch (Exception e) {
            logger.error("Failed to update strategy status", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_UPDATE_FAILED);
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