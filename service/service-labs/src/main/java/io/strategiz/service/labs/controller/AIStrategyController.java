package io.strategiz.service.labs.controller;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * REST controller for AI-powered strategy generation, explanation, and
 * optimization.
 */
@RestController
@RequestMapping("/v1/labs/ai")
@Tag(name = "AI Strategy", description = "AI-powered strategy generation, explanation, and optimization")
public class AIStrategyController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(AIStrategyController.class);

	private final AIStrategyService aiStrategyService;

	private final SubscriptionService subscriptionService;

	private final FeatureFlagService featureFlagService;

	@Autowired
	public AIStrategyController(AIStrategyService aiStrategyService, SubscriptionService subscriptionService,
			FeatureFlagService featureFlagService) {
		this.aiStrategyService = aiStrategyService;
		this.subscriptionService = subscriptionService;
		this.featureFlagService = featureFlagService;
	}

	/**
	 * Generate a new strategy from a natural language prompt.
	 */
	@PostMapping("/generate-strategy")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Generate strategy from prompt", description = "Uses AI to generate a trading strategy with both visual configuration and Python code from a natural language prompt")
	public ResponseEntity<AIStrategyResponse> generateStrategy(@Valid @RequestBody AIStrategyRequest request,
			@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Received strategy generation request from user {}", userId);

		// Check if Labs AI is enabled
		if (!featureFlagService.isLabsAIEnabled()) {
			logger.warn("Labs AI Strategy Generator is currently disabled");
			return ResponseEntity.status(503)
				.body(AIStrategyResponse.error("AI Strategy Generator is temporarily unavailable. Please try again later."));
		}

		// Check if user can send AI message (within limits)
		// Gracefully handle subscription check errors - allow request if check fails
		try {
			if (!subscriptionService.canSendMessage(userId)) {
				logger.warn("User {} exceeded daily AI chat limit", userId);
				return ResponseEntity.status(429)
					.body(AIStrategyResponse.error("Daily AI chat limit exceeded. Upgrade your plan for more messages."));
			}
		}
		catch (Exception e) {
			logger.warn("Error checking subscription limits for user {}, allowing request", userId, e);
			// Continue with request - fail open for subscription checks
		}

		try {
			AIStrategyResponse response = aiStrategyService.generateStrategy(request);

			// Record usage after successful generation
			// Gracefully handle usage recording errors - don't fail the request
			if (response != null && response.isSuccess()) {
				try {
					subscriptionService.recordMessageUsage(userId);
					logger.debug("Recorded AI chat usage for user {}", userId);
				}
				catch (Exception e) {
					logger.error("Error recording AI chat usage for user {}", userId, e);
					// Continue despite recording error
				}
			}

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error generating strategy", e);
			return ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage()));
		}
	}

	/**
	 * Refine an existing strategy based on user feedback.
	 */
	@PostMapping("/refine-strategy")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Refine existing strategy", description = "Uses AI to refine an existing strategy based on user feedback while maintaining consistency between visual config and code")
	public ResponseEntity<AIStrategyResponse> refineStrategy(@Valid @RequestBody AIStrategyRequest request,
			@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Received strategy refinement request from user {}", userId);

		// Check if Labs AI is enabled
		if (!featureFlagService.isLabsAIEnabled()) {
			logger.warn("Labs AI Strategy Generator is currently disabled");
			return ResponseEntity.status(503)
				.body(AIStrategyResponse.error("AI Strategy Generator is temporarily unavailable. Please try again later."));
		}

		// Check if user can send AI message (refinement counts as AI chat)
		// Gracefully handle subscription check errors - allow request if check fails
		try {
			if (!subscriptionService.canSendMessage(userId)) {
				logger.warn("User {} exceeded daily AI chat limit", userId);
				return ResponseEntity.status(429)
					.body(AIStrategyResponse.error("Daily AI chat limit exceeded. Upgrade your plan for more messages."));
			}
		}
		catch (Exception e) {
			logger.warn("Error checking subscription limits for user {}, allowing request", userId, e);
			// Continue with request - fail open for subscription checks
		}

		try {
			AIStrategyResponse response = aiStrategyService.refineStrategy(request);

			// Record usage after successful refinement
			// Gracefully handle usage recording errors - don't fail the request
			if (response != null && response.isSuccess()) {
				try {
					subscriptionService.recordMessageUsage(userId);
					logger.debug("Recorded AI chat usage for user {}", userId);
				}
				catch (Exception e) {
					logger.error("Error recording AI chat usage for user {}", userId, e);
					// Continue despite recording error
				}
			}

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error refining strategy", e);
			return ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage()));
		}
	}

	/**
	 * Parse Python code to extract visual configuration.
	 */
	@PostMapping("/parse-code")
	@Operation(summary = "Parse code to visual config", description = "Analyzes Python code and extracts a visual rule configuration for the UI")
	public ResponseEntity<AIStrategyResponse> parseCode(@RequestBody Map<String, String> request) {
		String code = request.get("code");
		if (code == null || code.isEmpty()) {
			return ResponseEntity.badRequest().body(AIStrategyResponse.error("Code is required"));
		}

		String visualEditorSchema = request.get("visualEditorSchema");

		logger.info("Received code parsing request with schema: {}", visualEditorSchema != null ? "provided" : "not provided");

		try {
			AIStrategyResponse response = aiStrategyService.parseCodeToVisual(code, visualEditorSchema);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error parsing code", e);
			return ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage()));
		}
	}

	/**
	 * Explain a specific element (rule, condition, or code section).
	 */
	@PostMapping("/explain")
	@Operation(summary = "Explain strategy element", description = "Uses AI to explain a specific rule, condition, or code section in plain English")
	public ResponseEntity<AIStrategyResponse> explainElement(@Valid @RequestBody AIStrategyRequest request) {
		if (request.getElementToExplain() == null || request.getElementToExplain().isEmpty()) {
			return ResponseEntity.badRequest().body(AIStrategyResponse.error("Element to explain is required"));
		}

		logger.info("Received explain element request");

		try {
			AIStrategyResponse response = aiStrategyService.explainElement(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error explaining element", e);
			return ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage()));
		}
	}

	/**
	 * Get optimization suggestions based on backtest results.
	 */
	@PostMapping("/optimize")
	@Operation(summary = "Get optimization suggestions", description = "Analyzes backtest results and suggests improvements to the strategy with expected impact")
	public ResponseEntity<AIStrategyResponse> optimizeStrategy(@Valid @RequestBody AIStrategyRequest request) {
		if (request.getBacktestResults() == null) {
			return ResponseEntity.badRequest().body(AIStrategyResponse.error("Backtest results are required"));
		}

		logger.info("Received optimization request");

		try {
			AIStrategyResponse response = aiStrategyService.optimizeFromBacktest(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error generating optimizations", e);
			return ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage()));
		}
	}

	/**
	 * Detect indicators from a partial prompt (for live preview while typing).
	 */
	@PostMapping("/preview-indicators")
	@Operation(summary = "Preview detected indicators", description = "Detects which technical indicators are mentioned in a partial prompt for live preview")
	public ResponseEntity<AIStrategyResponse> previewIndicators(@RequestBody Map<String, String> request) {
		String prompt = request.get("prompt");
		if (prompt == null || prompt.isEmpty()) {
			return ResponseEntity.badRequest().body(AIStrategyResponse.error("Prompt is required"));
		}

		logger.debug("Received indicator preview request");

		try {
			AIStrategyResponse response = aiStrategyService.previewIndicators(prompt);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error previewing indicators", e);
			// Return empty list on error for graceful degradation
			AIStrategyResponse response = new AIStrategyResponse();
			response.setSuccess(true);
			response.setDetectedIndicators(Collections.emptyList());
			return ResponseEntity.ok(response);
		}
	}

	/**
	 * Parse a natural language backtest query.
	 */
	@PostMapping("/parse-backtest-query")
	@Operation(summary = "Parse backtest query", description = "Parses natural language backtest queries like 'How would this do in the 2022 crash?' into date parameters")
	public ResponseEntity<Map<String, Object>> parseBacktestQuery(@RequestBody Map<String, String> request) {
		String query = request.get("query");
		if (query == null || query.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
		}

		logger.info("Received backtest query parsing request");

		try {
			Map<String, Object> response = aiStrategyService.parseBacktestQuery(query);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error parsing backtest query", e);
			return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
		}
	}

	@Override
	protected String getModuleName() {
		return "ai-strategy";
	}

}
