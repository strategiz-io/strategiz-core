package io.strategiz.service.labs.controller;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.model.StrategyResponse;
import io.strategiz.service.labs.service.StrategyVersioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for strategy versioning operations.
 * Handles creating new versions of deployed strategies and viewing version history.
 */
@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Versioning", description = "Create and manage strategy versions")
public class StrategyVersionController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(StrategyVersionController.class);

    private final StrategyVersioningService versioningService;

    @Autowired
    public StrategyVersionController(StrategyVersioningService versioningService) {
        this.versioningService = versioningService;
    }

    /**
     * POST /v1/strategies/{id}/version - Create a new version of a deployed strategy
     * This creates a draft copy for editing while the original stays deployed.
     */
    @PostMapping("/{id}/version")
    @Operation(summary = "Create strategy version", description = "Create a new draft version of a deployed strategy for editing")
    public ResponseEntity<StrategyResponse> createVersion(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        logger.info("Creating new version of strategy {} for user {}", id, userId);

        try {
            Strategy newVersion = versioningService.createVersion(id, userId);
            StrategyResponse response = convertToResponse(newVersion);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for creating version: {}", e.getMessage());
            throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, "service-labs", e);
        } catch (IllegalStateException e) {
            logger.warn("Cannot create version: {}", e.getMessage());
            throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_VERSION_INVALID_STATE, "service-labs", e);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create strategy version", e);
            throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_VERSION_FAILED, "service-labs", e);
        }
    }

    /**
     * GET /v1/strategies/{id}/versions - Get version history
     * Returns all versions of a strategy, sorted newest first.
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "Get version history", description = "Get all versions of a strategy")
    public ResponseEntity<List<StrategyResponse>> getVersionHistory(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        logger.info("Fetching version history for strategy {} for user {}", id, userId);

        try {
            List<Strategy> versions = versioningService.getVersionHistory(id, userId);
            List<StrategyResponse> responses = versions.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for version history: {}", e.getMessage());
            throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, "service-labs", e);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch version history", e);
            throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_FETCH_FAILED, "service-labs", e);
        }
    }

    /**
     * Convert Strategy entity to StrategyResponse
     */
    private StrategyResponse convertToResponse(Strategy strategy) {
        StrategyResponse response = new StrategyResponse();
        response.setId(strategy.getId());
        response.setUserId(strategy.getOwnerId());
        response.setName(strategy.getName());
        response.setDescription(strategy.getDescription());
        response.setCode(strategy.getCode());
        response.setLanguage(strategy.getLanguage());
        response.setType(strategy.getType());
        response.setTags(strategy.getTags());
        response.setParameters(strategy.getParameters());
        response.setPerformance(strategy.getPerformance());
        response.setBacktestResults(strategy.getBacktestResults());
        response.setStatus(strategy.getPublishStatus());
        response.setPublicStatus(strategy.getPublicStatus());
        // Skip date fields - they'll be null
        // Versioning fields
        response.setVersion(strategy.getVersion());
        response.setParentStrategyId(strategy.getParentStrategyId());
        // Note: Deployment fields removed - tracked in BotDeployment/AlertDeployment entities
        return response;
    }

    @Override
    protected String getModuleName() {
        return "strategy-versioning";
    }
}
