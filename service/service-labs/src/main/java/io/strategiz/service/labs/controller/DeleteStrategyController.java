package io.strategiz.service.labs.controller;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.service.DeleteStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/strategies")
@RequireAuth(minAcr = "1")
@Tag(name = "Strategy Deletion", description = "Delete trading strategies")
public class DeleteStrategyController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStrategyController.class);

    private final DeleteStrategyService deleteStrategyService;

    @Autowired
    public DeleteStrategyController(DeleteStrategyService deleteStrategyService) {
        this.deleteStrategyService = deleteStrategyService;
    }

    @DeleteMapping("/{strategyId}")
    @Operation(summary = "Delete a strategy", description = "Deletes a trading strategy")
    public ResponseEntity<Void> deleteStrategy(
            @PathVariable String strategyId,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Deleting strategy: {} for user: {}", strategyId, userId);

        try {
            boolean deleted = deleteStrategyService.deleteStrategy(strategyId, userId);

            if (!deleted) {
                throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, "service-labs", strategyId);
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