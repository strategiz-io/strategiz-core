package io.strategiz.service.strategy.controller;

import io.strategiz.data.strategy.Strategy;
import io.strategiz.service.strategy.StrategyService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for strategy management operations
 * Handles HTTP concerns only, delegates business logic to service layer
 */
@RestController
@RequestMapping("/v1/strategies")
@CrossOrigin(origins = "*")
public class StrategyController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.STRATEGY_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(StrategyController.class);

    private final StrategyService strategyService;

    @Autowired
    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    /**
     * Get all strategies for a user
     */
    @GetMapping
    public ResponseEntity<Object> getAllStrategies(@RequestParam String userId) {
        try {
            log.info("HTTP request to get all strategies for user: {}", userId);
            List<Strategy> strategies = strategyService.getAllStrategiesByUserId(userId);
            return ResponseEntity.ok(strategies);
        } catch (Exception e) {
            log.error("HTTP error getting strategies: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategies", "message", e.getMessage()));
        }
    }

    /**
     * Get a specific strategy
     */
    @GetMapping("/{strategyId}")
    public ResponseEntity<Object> getStrategy(@PathVariable String strategyId, @RequestParam String userId) {
        try {
            log.info("HTTP request to get strategy with id: {} for user: {}", strategyId, userId);
            Optional<Strategy> strategyOpt = strategyService.getStrategyByIdAndUserId(strategyId, userId);
            
            if (!strategyOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Strategy not found"));
            }
            
            return ResponseEntity.ok(strategyOpt.get());
        } catch (Exception e) {
            log.error("HTTP error getting strategy: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategy", "message", e.getMessage()));
        }
    }

    /**
     * Create a new strategy or update an existing one
     */
    @PostMapping
    public ResponseEntity<Object> saveStrategy(@RequestBody Strategy strategy, @RequestParam String userId) {
        try {
            log.info("HTTP request to save strategy for user: {}", userId);
            
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Strategy data is required"));
            }
            
            // Set userId from request parameter
            strategy.setUserId(userId);
            
            // Save strategy via service layer
            Strategy savedStrategy = strategyService.saveStrategy(strategy);
            
            // Return success response with strategy ID
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedStrategy.getId());
            response.put("message", "Strategy saved successfully");
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            log.error("HTTP validation error saving strategy: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("HTTP error saving strategy: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save strategy", "message", e.getMessage()));
        }
    }

    /**
     * Delete a strategy
     */
    @DeleteMapping("/{strategyId}")
    public ResponseEntity<Object> deleteStrategy(@PathVariable String strategyId, @RequestParam String userId) {
        try {
            log.info("HTTP request to delete strategy with id: {} for user: {}", strategyId, userId);
            boolean deleted = strategyService.deleteStrategy(strategyId, userId);
            
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Strategy not found or could not be deleted"));
            }
            
            return ResponseEntity.ok(Map.of("message", "Strategy deleted successfully"));
        } catch (Exception e) {
            log.error("HTTP error deleting strategy: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete strategy", "message", e.getMessage()));
        }
    }
    
    /**
     * Update strategy status (DRAFT, TESTING, LIVE)
     */
    @PutMapping("/{strategyId}/status")
    public ResponseEntity<Object> updateStrategyStatus(
            @PathVariable String strategyId, 
            @RequestParam String userId,
            @RequestBody Map<String, String> statusUpdate) {
        
        try {
            log.info("HTTP request to update status for strategy with id: {} for user: {}", strategyId, userId);
            
            String status = statusUpdate.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Status is required"));
            }
            
            // Get exchange info if present
            String exchangeInfo = statusUpdate.get("exchange");
            
            // Update status via service layer
            boolean updated = strategyService.updateStrategyStatus(strategyId, userId, status, exchangeInfo);
            
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Strategy not found or could not be updated"));
            }
            
            return ResponseEntity.ok(Map.of("message", "Strategy status updated successfully"));
        } catch (IllegalArgumentException e) {
            log.error("HTTP validation error updating strategy status: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("HTTP error updating strategy status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update strategy status", "message", e.getMessage()));
        }
    }
    
    /**
     * Run a backtest on a strategy
     */
    @PostMapping("/{strategyId}/backtest")
    public ResponseEntity<Object> runBacktest(
            @PathVariable String strategyId,
            @RequestParam String userId,
            @RequestBody Map<String, Object> backtestParams) {
        
        try {
            log.info("HTTP request to run backtest for strategy with id: {} for user: {}", strategyId, userId);
            
            // Run backtest via service layer
            Map<String, Object> backtestResult = strategyService.runBacktest(strategyId, userId, backtestParams);
            
            return ResponseEntity.ok(backtestResult);
        } catch (IllegalArgumentException e) {
            log.error("HTTP validation error running backtest: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("HTTP error running backtest: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to run backtest", "message", e.getMessage()));
        }
    }
}
