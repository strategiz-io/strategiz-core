package io.strategiz.service.labs.controller;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.constants.StrategyConstants;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import io.strategiz.service.labs.model.CreateStrategyRequest;
import io.strategiz.service.labs.model.CreateStrategyResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/strategies")
@RequireAuth(minAcr = "1")
@Tag(name = "Strategy Update", description = "Update existing trading strategies")
public class UpdateStrategyController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(UpdateStrategyController.class);

	private final UpdateStrategyService updateStrategyService;

	private final ReadStrategyService readStrategyService;

	@Autowired
	public UpdateStrategyController(UpdateStrategyService updateStrategyService,
			ReadStrategyService readStrategyService) {
		this.updateStrategyService = updateStrategyService;
		this.readStrategyService = readStrategyService;
	}

	@PutMapping("/{strategyId}")
	@Operation(summary = "Update a strategy", description = "Updates an existing trading strategy")
	public ResponseEntity<CreateStrategyResponse> updateStrategy(@PathVariable String strategyId,
			@Valid @RequestBody CreateStrategyRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Updating strategy: {} for user: {}", strategyId, userId);

		try {
			// Update strategy using service
			Strategy updated = updateStrategyService.updateStrategy(strategyId, userId, request);

			// Return minimal response (just essential fields, not code or performance
			// data)
			CreateStrategyResponse response = new CreateStrategyResponse();
			response.setId(updated.getId());
			response.setName(updated.getName());
			response.setIsPublished(updated.getIsPublished());
			response.setIsPublic(updated.getIsPublic());
			response.setIsListed(updated.getIsListed());
			response.setCreatedDate(updated.getCreatedDate() != null ? updated.getCreatedDate().toString() : null);
			response.setModifiedDate(updated.getModifiedDate() != null ? updated.getModifiedDate().toString() : null);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Failed to update strategy", e);
			throw handleException(e, StrategyConstants.ERROR_STRATEGY_UPDATE_FAILED);
		}
	}

	@PatchMapping("/{strategyId}/status")
	@Operation(summary = "Update strategy status",
			description = "Updates the status of a strategy (active, archived, etc.)")
	public ResponseEntity<StrategyResponse> updateStrategyStatus(@PathVariable String strategyId,
			@RequestParam String status, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Updating strategy {} status to: {} for user: {}", strategyId, status, userId);

		try {
			// Update status using service
			boolean updated = updateStrategyService.updateStrategyStatus(strategyId, userId, status);

			if (!updated) {
				throw new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, "service-labs",
						strategyId);
			}

			// Fetch updated strategy to return
			Strategy strategy = readStrategyService.getStrategyById(strategyId, userId)
				.orElseThrow(() -> new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND,
						"service-labs", strategyId));

			return ResponseEntity.ok(convertToResponse(strategy));
		}
		catch (Exception e) {
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
		response.setIsPublished(strategy.getIsPublished());
		response.setIsPublic(strategy.getIsPublic());
		response.setIsListed(strategy.getIsListed());
		response.setTags(strategy.getTags());
		response.setUserId(strategy.getOwnerId());
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