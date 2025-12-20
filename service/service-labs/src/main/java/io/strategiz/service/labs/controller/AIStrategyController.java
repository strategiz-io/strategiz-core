package io.strategiz.service.labs.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

	@Autowired
	public AIStrategyController(AIStrategyService aiStrategyService) {
		this.aiStrategyService = aiStrategyService;
	}

	/**
	 * Generate a new strategy from a natural language prompt.
	 */
	@PostMapping("/generate-strategy")
	@Operation(summary = "Generate strategy from prompt", description = "Uses AI to generate a trading strategy with both visual configuration and Python code from a natural language prompt")
	public Mono<ResponseEntity<AIStrategyResponse>> generateStrategy(@Valid @RequestBody AIStrategyRequest request) {
		logger.info("Received strategy generation request");

		return aiStrategyService.generateStrategy(request).map(ResponseEntity::ok).onErrorResume(e -> {
			logger.error("Error generating strategy", e);
			return Mono.just(ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage())));
		});
	}

	/**
	 * Refine an existing strategy based on user feedback.
	 */
	@PostMapping("/refine-strategy")
	@Operation(summary = "Refine existing strategy", description = "Uses AI to refine an existing strategy based on user feedback while maintaining consistency between visual config and code")
	public Mono<ResponseEntity<AIStrategyResponse>> refineStrategy(@Valid @RequestBody AIStrategyRequest request) {
		logger.info("Received strategy refinement request");

		return aiStrategyService.refineStrategy(request).map(ResponseEntity::ok).onErrorResume(e -> {
			logger.error("Error refining strategy", e);
			return Mono.just(ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage())));
		});
	}

	/**
	 * Parse Python code to extract visual configuration.
	 */
	@PostMapping("/parse-code")
	@Operation(summary = "Parse code to visual config", description = "Analyzes Python code and extracts a visual rule configuration for the UI")
	public Mono<ResponseEntity<AIStrategyResponse>> parseCode(@RequestBody Map<String, String> request) {
		String code = request.get("code");
		if (code == null || code.isEmpty()) {
			return Mono.just(ResponseEntity.badRequest().body(AIStrategyResponse.error("Code is required")));
		}

		logger.info("Received code parsing request");

		return aiStrategyService.parseCodeToVisual(code).map(ResponseEntity::ok).onErrorResume(e -> {
			logger.error("Error parsing code", e);
			return Mono.just(ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage())));
		});
	}

	/**
	 * Explain a specific element (rule, condition, or code section).
	 */
	@PostMapping("/explain")
	@Operation(summary = "Explain strategy element", description = "Uses AI to explain a specific rule, condition, or code section in plain English")
	public Mono<ResponseEntity<AIStrategyResponse>> explainElement(@Valid @RequestBody AIStrategyRequest request) {
		if (request.getElementToExplain() == null || request.getElementToExplain().isEmpty()) {
			return Mono.just(ResponseEntity.badRequest().body(AIStrategyResponse.error("Element to explain is required")));
		}

		logger.info("Received explain element request");

		return aiStrategyService.explainElement(request).map(ResponseEntity::ok).onErrorResume(e -> {
			logger.error("Error explaining element", e);
			return Mono.just(ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage())));
		});
	}

	/**
	 * Get optimization suggestions based on backtest results.
	 */
	@PostMapping("/optimize")
	@Operation(summary = "Get optimization suggestions", description = "Analyzes backtest results and suggests improvements to the strategy with expected impact")
	public Mono<ResponseEntity<AIStrategyResponse>> optimizeStrategy(@Valid @RequestBody AIStrategyRequest request) {
		if (request.getBacktestResults() == null) {
			return Mono.just(ResponseEntity.badRequest().body(AIStrategyResponse.error("Backtest results are required")));
		}

		logger.info("Received optimization request");

		return aiStrategyService.optimizeFromBacktest(request).map(ResponseEntity::ok).onErrorResume(e -> {
			logger.error("Error generating optimizations", e);
			return Mono.just(ResponseEntity.internalServerError().body(AIStrategyResponse.error(e.getMessage())));
		});
	}

	/**
	 * Detect indicators from a partial prompt (for live preview while typing).
	 */
	@PostMapping("/preview-indicators")
	@Operation(summary = "Preview detected indicators", description = "Detects which technical indicators are mentioned in a partial prompt for live preview")
	public Mono<ResponseEntity<AIStrategyResponse>> previewIndicators(@RequestBody Map<String, String> request) {
		String prompt = request.get("prompt");
		if (prompt == null || prompt.isEmpty()) {
			return Mono.just(ResponseEntity.badRequest().body(AIStrategyResponse.error("Prompt is required")));
		}

		logger.debug("Received indicator preview request");

		return aiStrategyService.previewIndicators(prompt).map(ResponseEntity::ok).onErrorResume(e -> {
			logger.error("Error previewing indicators", e);
			// Return empty list on error for graceful degradation
			AIStrategyResponse response = new AIStrategyResponse();
			response.setSuccess(true);
			response.setDetectedIndicators(java.util.Collections.emptyList());
			return Mono.just(ResponseEntity.ok(response));
		});
	}

	/**
	 * Parse a natural language backtest query.
	 */
	@PostMapping("/parse-backtest-query")
	@Operation(summary = "Parse backtest query", description = "Parses natural language backtest queries like 'How would this do in the 2022 crash?' into date parameters")
	public Mono<ResponseEntity<Map<String, Object>>> parseBacktestQuery(@RequestBody Map<String, String> request) {
		String query = request.get("query");
		if (query == null || query.isEmpty()) {
			return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Query is required")));
		}

		logger.info("Received backtest query parsing request");

		return aiStrategyService.parseBacktestQuery(query)
			.map(ResponseEntity::ok)
			.onErrorResume(e -> {
				logger.error("Error parsing backtest query", e);
				return Mono.just(ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())));
			});
	}

	/**
	 * Stream strategy generation (Server-Sent Events).
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream strategy generation", description = "Streams strategy generation in real-time using Server-Sent Events")
	public Flux<ServerSentEvent<String>> streamGeneration(@RequestParam String prompt,
			@RequestParam(required = false) String symbols, @RequestParam(required = false) String timeframe) {

		logger.info("Received streaming generation request");

		AIStrategyRequest request = new AIStrategyRequest();
		request.setPrompt(prompt);

		if (symbols != null || timeframe != null) {
			AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
			if (symbols != null) {
				context.setSymbols(java.util.Arrays.asList(symbols.split(",")));
			}
			context.setTimeframe(timeframe);
			request.setContext(context);
		}

		return aiStrategyService.streamGeneration(request)
			.map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
			.concatWith(Flux.just(ServerSentEvent.<String>builder().event("complete").data("").build()));
	}

	@Override
	protected String getModuleName() {
		return "ai-strategy";
	}

}
