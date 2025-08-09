package io.strategiz.service.strategy.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.strategy.constants.StrategyConstants;
import io.strategiz.service.strategy.service.DeleteStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/strategies")
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
            Authentication authentication) {
        
        String userId = authentication.getName();
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