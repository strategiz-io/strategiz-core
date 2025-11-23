package io.strategiz.service.labs.controller;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.service.DeleteStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Deletion", description = "Delete trading strategies")
public class DeleteStrategyController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStrategyController.class);

    private final DeleteStrategyService deleteStrategyService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public DeleteStrategyController(DeleteStrategyService deleteStrategyService,
                                  SessionAuthBusiness sessionAuthBusiness) {
        this.deleteStrategyService = deleteStrategyService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    @DeleteMapping("/{strategyId}")
    @Operation(summary = "Delete a strategy", description = "Deletes a trading strategy")
    public ResponseEntity<Void> deleteStrategy(
            @PathVariable String strategyId,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userIdParam) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        logger.debug("DeleteStrategy: Principal userId: {}, AuthHeader present: {}, Query param userId: {}",
                userId, authHeader != null, userIdParam);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    logger.info("Delete strategy authenticated via Bearer token for user: {}", userId);
                } else {
                    logger.warn("Invalid Bearer token provided for delete strategy");
                }
            } catch (Exception e) {
                logger.warn("Error validating Bearer token for delete strategy: {}", e.getMessage());
            }
        }

        // Use query param userId as fallback for development/testing
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            logger.warn("Using userId from query parameter for development: {}", userId);
        }

        // Require authentication
        if (userId == null) {
            throw new IllegalStateException("Authentication required to delete strategies");
        }

        logger.info("Deleting strategy: {} for user: {}", strategyId, userId);

        try {
            boolean deleted = deleteStrategyService.deleteStrategy(strategyId, userId);

            if (!deleted) {
                throw new RuntimeException("Strategy not found or access denied");
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to delete strategy", e);
            throw handleException(e, StrategyConstants.ERROR_STRATEGY_DELETION_FAILED);
        }
    }
    
    @Override
    protected String getModuleName() {
        return "strategy";
    }
}