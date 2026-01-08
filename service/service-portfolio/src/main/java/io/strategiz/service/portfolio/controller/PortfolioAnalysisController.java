package io.strategiz.service.portfolio.controller;

import io.strategiz.business.aichat.PortfolioAnalysisService;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.portfolio.model.request.PortfolioAnalysisRequestDto;
import io.strategiz.service.portfolio.model.request.PortfolioChatRequestDto;
import io.strategiz.service.portfolio.model.response.PortfolioChatResponseDto;
import io.strategiz.service.portfolio.model.response.PortfolioInsightDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * REST controller for AI-powered portfolio analysis.
 * Provides endpoints for automated insights and conversational chat about portfolio.
 */
@RestController
@RequestMapping("/v1/portfolio/analysis")
@Tag(name = "Portfolio AI Analysis", description = "AI-powered portfolio insights and chat")
public class PortfolioAnalysisController extends BaseController {

	private static final String DEFAULT_MODEL = "gemini-2.5-flash";

	private final PortfolioAnalysisService portfolioAnalysisService;

	public PortfolioAnalysisController(PortfolioAnalysisService portfolioAnalysisService) {
		this.portfolioAnalysisService = portfolioAnalysisService;
	}

	@Override
	protected String getModuleName() {
		return "service-portfolio";
	}

	/**
	 * Generate AI-powered portfolio insights.
	 * Generates all 4 insight types (Risk, Performance, Rebalancing, Opportunities) in
	 * parallel.
	 */
	@PostMapping("/insights")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Generate AI portfolio insights", description = "Generate automated insights covering risk, performance, rebalancing, and opportunities")
	public Mono<ResponseEntity<List<PortfolioInsightDto>>> generateInsights(
			@Valid @RequestBody PortfolioAnalysisRequestDto request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;

		log.info("Generating portfolio insights for user: {}, model: {}", userId, model);

		// TODO: Add feature flag check: isPortfolioAIEnabled()
		// TODO: Add subscription limit check: canUseAIInsights(userId)

		return portfolioAnalysisService.generateAllInsights(userId, model)
			.map(chatResponses -> chatResponses.stream().map(this::convertToInsightDto).collect(Collectors.toList()))
			.map(ResponseEntity::ok)
			.onErrorResume(error -> {
				log.error("Error generating portfolio insights for user {}: {}", userId, error.getMessage(), error);
				return Mono.just(ResponseEntity.internalServerError().build());
			});

		// TODO: Add subscription usage tracking after successful generation:
		// subscriptionService.recordAIInsightUsage(userId)
	}

	/**
	 * Chat with AI about portfolio.
	 * Supports conversational interaction with portfolio context.
	 */
	@PostMapping("/chat")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Chat with portfolio AI assistant", description = "Ask questions about your portfolio and get AI-powered responses")
	public Mono<ResponseEntity<PortfolioChatResponseDto>> chat(@Valid @RequestBody PortfolioChatRequestDto request,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;

		log.info("Portfolio chat for user: {}, model: {}", userId, model);

		// TODO: Add subscription limit check: canSendMessage(userId)

		return portfolioAnalysisService
			.chat(request.getMessage(), userId,
					request.getConversationHistory() != null ? request.getConversationHistory() : new ArrayList<>(),
					model, request.getProviderId())
			.map(this::convertToChatResponseDto)
			.map(ResponseEntity::ok)
			.onErrorResume(error -> {
				log.error("Error in portfolio chat for user {}: {}", userId, error.getMessage(), error);
				PortfolioChatResponseDto errorResponse = new PortfolioChatResponseDto();
				errorResponse.setSuccess(false);
				errorResponse.setError("Failed to generate response: " + error.getMessage());
				return Mono.just(ResponseEntity.ok(errorResponse));
			});

		// TODO: Add subscription usage tracking after successful chat:
		// subscriptionService.recordMessageUsage(userId)
	}

	/**
	 * Stream chat with AI about portfolio (SSE).
	 * Provides real-time streaming responses for better UX.
	 */
	@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Stream chat with portfolio AI", description = "Real-time streaming chat about your portfolio")
	public Flux<PortfolioChatResponseDto> chatStream(@RequestParam String message,
			@RequestParam(required = false) String providerId, @RequestParam(required = false) String model,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		String selectedModel = model != null ? model : DEFAULT_MODEL;

		log.info("Portfolio chat stream for user: {}, model: {}", userId, selectedModel);

		// TODO: Add subscription usage tracking:
		// subscriptionService.recordMessageUsage(userId)

		return portfolioAnalysisService
			.chatStream(message, userId, new ArrayList<>(), selectedModel, providerId)
			.map(this::convertToChatResponseDto)
			.onErrorResume(error -> {
				log.error("Error in portfolio chat stream for user {}: {}", userId, error.getMessage(), error);
				PortfolioChatResponseDto errorResponse = new PortfolioChatResponseDto();
				errorResponse.setSuccess(false);
				errorResponse.setError("Stream error: " + error.getMessage());
				return Flux.just(errorResponse);
			});
	}

	/**
	 * Convert ChatResponse to PortfolioInsightDto. Uses metadata from service
	 * for type/title, extracts summary and action items from AI response content.
	 */
	private PortfolioInsightDto convertToInsightDto(ChatResponse chatResponse) {
		PortfolioInsightDto dto = new PortfolioInsightDto();
		dto.setId(chatResponse.getId());
		dto.setContent(chatResponse.getContent());
		dto.setModel(chatResponse.getModel());
		dto.setSuccess(chatResponse.isSuccess());
		dto.setError(chatResponse.getError());

		if (chatResponse.getTimestamp() != null) {
			dto.setGeneratedAt(chatResponse.getTimestamp().toEpochSecond(ZoneOffset.UTC));
		}

		// Use metadata if available (preferred), otherwise fall back to content extraction
		String type;
		String title;
		if (chatResponse.getMetadata() != null) {
			type = (String) chatResponse.getMetadata().getOrDefault("type", "OVERVIEW");
			title = (String) chatResponse.getMetadata().getOrDefault("title", generateTitle(type));

			// Use generatedAt from metadata if present
			Object generatedAt = chatResponse.getMetadata().get("generatedAt");
			if (generatedAt instanceof Number) {
				dto.setGeneratedAt(((Number) generatedAt).longValue());
			}
		}
		else {
			// Fallback to content extraction (legacy behavior)
			type = extractInsightType(chatResponse.getContent());
			title = generateTitle(type);
		}
		dto.setType(type);
		dto.setTitle(title);

		// Extract summary (first 1-2 sentences)
		String summary = extractSummary(chatResponse.getContent());
		dto.setSummary(summary);

		// Extract risk level for risk insights
		if ("RISK".equals(type)) {
			String riskLevel = extractRiskLevel(chatResponse.getContent());
			dto.setRiskLevel(riskLevel);
		}

		// Extract action items (if any)
		List<PortfolioInsightDto.ActionItem> actionItems = extractActionItems(chatResponse.getContent());
		dto.setActionItems(actionItems);

		return dto;
	}

	/**
	 * Convert ChatResponse to PortfolioChatResponseDto
	 */
	private PortfolioChatResponseDto convertToChatResponseDto(ChatResponse chatResponse) {
		PortfolioChatResponseDto dto = new PortfolioChatResponseDto();
		dto.setId(chatResponse.getId());
		dto.setContent(chatResponse.getContent());
		dto.setModel(chatResponse.getModel());
		dto.setSuccess(chatResponse.isSuccess());
		dto.setError(chatResponse.getError());
		dto.setTokensUsed(chatResponse.getTokensUsed());

		if (chatResponse.getTimestamp() != null) {
			dto.setTimestamp(chatResponse.getTimestamp().toEpochSecond(ZoneOffset.UTC));
		}

		return dto;
	}

	/**
	 * Extract insight type from AI response content
	 */
	private String extractInsightType(String content) {
		if (content == null)
			return "OVERVIEW";

		String lowerContent = content.toLowerCase();
		if (lowerContent.contains("risk") && (lowerContent.contains("analysis") || lowerContent.contains("assessment"))) {
			return "RISK";
		}
		if (lowerContent.contains("performance")) {
			return "PERFORMANCE";
		}
		if (lowerContent.contains("rebalancing") || lowerContent.contains("rebalance")) {
			return "REBALANCING";
		}
		if (lowerContent.contains("opportunities") || lowerContent.contains("opportunity")) {
			return "OPPORTUNITIES";
		}

		return "OVERVIEW";
	}

	/**
	 * Generate title based on insight type
	 */
	private String generateTitle(String type) {
		return switch (type) {
			case "RISK" -> "Portfolio Risk Analysis";
			case "PERFORMANCE" -> "Performance Analysis";
			case "REBALANCING" -> "Rebalancing Recommendations";
			case "OPPORTUNITIES" -> "Investment Opportunities";
			default -> "Portfolio Overview";
		};
	}

	/**
	 * Extract summary (first 1-2 sentences) from content
	 */
	private String extractSummary(String content) {
		if (content == null || content.isBlank()) {
			return "Analysis in progress...";
		}

		// Find first 2 sentences
		String[] sentences = content.split("\\. ");
		if (sentences.length >= 2) {
			return sentences[0] + ". " + sentences[1] + ".";
		}
		if (sentences.length == 1) {
			return sentences[0] + (sentences[0].endsWith(".") ? "" : ".");
		}

		// Fallback: first 150 characters
		return content.length() > 150 ? content.substring(0, 150) + "..." : content;
	}

	/**
	 * Extract risk level from risk analysis content
	 */
	private String extractRiskLevel(String content) {
		if (content == null)
			return "MEDIUM";

		String lowerContent = content.toLowerCase();
		if (lowerContent.contains("high risk") || lowerContent.contains("risk: high")
				|| lowerContent.contains("risk score: high")) {
			return "HIGH";
		}
		if (lowerContent.contains("low risk") || lowerContent.contains("risk: low")
				|| lowerContent.contains("risk score: low")) {
			return "LOW";
		}

		return "MEDIUM";
	}

	/**
	 * Extract action items from AI response content. Looks for bullet points or
	 * numbered lists.
	 */
	private List<PortfolioInsightDto.ActionItem> extractActionItems(String content) {
		List<PortfolioInsightDto.ActionItem> actionItems = new ArrayList<>();

		if (content == null)
			return actionItems;

		// Pattern to match markdown bullet points or numbered lists
		Pattern pattern = Pattern.compile("(?m)^[\\-\\*]\\s+(.+)$|^\\d+\\.\\s+(.+)$");
		Matcher matcher = pattern.matcher(content);

		while (matcher.find()) {
			String action = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			if (action != null && !action.isBlank()) {
				PortfolioInsightDto.ActionItem item = new PortfolioInsightDto.ActionItem();
				item.setAction(action.trim());
				item.setRationale("See analysis above");
				item.setPriority(determinePriority(action));
				actionItems.add(item);

				// Limit to 5 action items
				if (actionItems.size() >= 5)
					break;
			}
		}

		return actionItems;
	}

	/**
	 * Determine action priority based on keywords
	 */
	private String determinePriority(String action) {
		String lowerAction = action.toLowerCase();
		if (lowerAction.contains("immediately") || lowerAction.contains("urgent") || lowerAction.contains("critical")) {
			return "HIGH";
		}
		if (lowerAction.contains("consider") || lowerAction.contains("explore") || lowerAction.contains("review")) {
			return "LOW";
		}
		return "MEDIUM";
	}

}
