package io.strategiz.service.strategy.controller;

import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategy Deletion", description = "Delete trading strategies")
public class DeleteStrategyController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeleteStrategyController.class);
    
    @DeleteMapping("/{strategyId}")
    @Operation(summary = "Delete a strategy", description = "Deletes a trading strategy")
    public ResponseEntity<Void> deleteStrategy(
            @PathVariable String strategyId,
            Authentication authentication) {
        
        logger.info("Deleting strategy: {} for user: {}", strategyId, authentication.getName());
        
        // TODO: Implement service layer
        
        return ResponseEntity.noContent().build();
    }
}