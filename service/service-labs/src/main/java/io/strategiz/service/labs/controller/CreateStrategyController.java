package io.strategiz.service.labs.controller;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import io.strategiz.service.labs.model.StrategyResponse;
import io.strategiz.service.labs.service.CreateStrategyService;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/strategies")
@Tag(name = "Strategy Creation", description = "Create new trading strategies")
@RequireAuth(minAcr = "1")
public class CreateStrategyController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(CreateStrategyController.class);

    private final CreateStrategyService createStrategyService;

    @Autowired
    public CreateStrategyController(CreateStrategyService createStrategyService) {
        this.createStrategyService = createStrategyService;
    }
    
    @PostMapping
    @Operation(summary = "Create a new strategy", description = "Creates a new trading strategy for the authenticated user")
    public ResponseEntity<StrategyResponse> createStrategy(
            @Valid @RequestBody CreateStrategyRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
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