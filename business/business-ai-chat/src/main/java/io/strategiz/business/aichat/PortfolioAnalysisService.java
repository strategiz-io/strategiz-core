package io.strategiz.business.aichat;

import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.business.aichat.prompt.PortfolioAnalysisPrompts;
import io.strategiz.data.provider.entity.PortfolioInsightsCacheEntity;
import io.strategiz.data.provider.repository.PortfolioInsightsCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for AI-powered portfolio analysis. Generates insights for Risk Analysis,
 * Performance Analysis, Rebalancing, and Investment Opportunities.
 *
 * Implements caching strategy: - Return cached insights immediately if valid (< 24 hours,
 * not invalidated) - Generate new insights only when cache is invalid/missing - Cache is
 * invalidated when provider data changes (sync, connect, disconnect)
 */
@Service
public class PortfolioAnalysisService {

	private static final Logger logger = LoggerFactory.getLogger(PortfolioAnalysisService.class);

	/**
	 * Timeout for individual insight generation (20 seconds each)
	 */
	private static final Duration INSIGHT_TIMEOUT = Duration.ofSeconds(20);

	private final AIChatBusiness aiChatBusiness;

	private final PortfolioContextProvider portfolioContextProvider;

	private final PortfolioInsightsCacheRepository insightsCacheRepository;

	@Autowired
	public PortfolioAnalysisService(AIChatBusiness aiChatBusiness, PortfolioContextProvider portfolioContextProvider,
			PortfolioInsightsCacheRepository insightsCacheRepository) {
		this.aiChatBusiness = aiChatBusiness;
		this.portfolioContextProvider = portfolioContextProvider;
		this.insightsCacheRepository = insightsCacheRepository;
	}

	/**
	 * Generate a single portfolio insight
	 * @param userId User ID
	 * @param insightType Type of insight: "risk", "performance", "rebalancing",
	 * "opportunities"
	 * @param model LLM model to use
	 * @return ChatResponse containing the generated insight
	 */
	public Mono<ChatResponse> generateInsight(String userId, String insightType, String model) {
		logger.info("Generating {} insight for user: {}, model: {}", insightType, userId, model);

		try {
			// 1. Fetch portfolio context
			Map<String, Object> portfolioContext = portfolioContextProvider.getPortfolioContext(userId);

			// Check if user has portfolio data
			Boolean hasPortfolio = (Boolean) portfolioContext.get("hasPortfolio");
			if (Boolean.FALSE.equals(hasPortfolio)) {
				Integer connectedProviders = (Integer) portfolioContext.getOrDefault("connectedProviders", 0);
				String message = connectedProviders > 0
						? "Your account is syncing. Please check back in a few moments for AI insights."
						: "Connect your brokerage or exchange accounts to get AI-powered portfolio insights.";
				return Mono.just(new ChatResponse(message));
			}

			// 2. Build contextual prompt
			String systemPrompt = PortfolioAnalysisPrompts.buildContextualPrompt(insightType, portfolioContext);

			// 3. Create chat context with system prompt override
			ChatContext chatContext = new ChatContext();
			chatContext.setFeature("portfolio");
			chatContext.setCurrentPage("analysis");
			chatContext.setPortfolioData(portfolioContext);
			chatContext.setSystemPrompt(systemPrompt); // Pass the insight-specific prompt

			// 4. Generate insight using AIChatBusiness
			String userMessage = String.format("Provide a %s analysis for my portfolio.", insightType);
			final String insightTypeUpper = insightType.toUpperCase();

			return aiChatBusiness.chat(userMessage, chatContext, new ArrayList<>(), model).map(response -> {
				// Set insight type metadata so controller doesn't have to guess
				response.setMetadata(Map.of("type", insightTypeUpper, "title", formatInsightTitle(insightType),
						"generatedAt", System.currentTimeMillis() / 1000));
				return response;
			}).doOnSuccess(response -> {
				logger.info("Successfully generated {} insight for user: {}, tokens: {}", insightType, userId,
						response.getTokensUsed());
			}).doOnError(error -> {
				logger.error("Error generating {} insight for user {}: {}", insightType, userId, error.getMessage(),
						error);
			});
		}
		catch (Exception e) {
			logger.error("Error in generateInsight for user {}: {}", userId, e.getMessage(), e);
			return Mono.just(ChatResponse.error("Failed to generate insight: " + e.getMessage()));
		}
	}

	/**
	 * Generate all 4 insight types in parallel (Risk, Performance, Rebalancing,
	 * Opportunities) with caching.
	 *
	 * Cache Strategy: 1. Check cache first - return immediately if valid 2. If cache
	 * invalid/missing, generate new insights 3. Save new insights to cache for next time
	 * 4. Each insight has a 20-second timeout to prevent hanging
	 * @param userId User ID
	 * @param model LLM model to use
	 * @return List of ChatResponse objects, one for each insight type
	 */
	public Mono<List<ChatResponse>> generateAllInsights(String userId, String model) {
		logger.info("Generating all insights for user: {}, model: {}", userId, model);

		// 1. Check cache first
		try {
			Optional<PortfolioInsightsCacheEntity> cachedOpt = insightsCacheRepository.getCachedInsights(userId);

			if (cachedOpt.isPresent()) {
				PortfolioInsightsCacheEntity cached = cachedOpt.get();
				logger.info("Returning cached insights for user: {} (generated at: {})", userId,
						cached.getGeneratedAt());

				// Convert cached insights to ChatResponse list
				List<ChatResponse> cachedResponses = convertCacheToResponses(cached);
				return Mono.just(cachedResponses);
			}

			logger.info("No valid cache found for user: {}, generating new insights", userId);
		}
		catch (Exception e) {
			logger.warn("Error checking cache for user {}: {} - proceeding with generation", userId, e.getMessage());
		}

		// 2. Generate all 4 insight types in parallel with individual timeouts
		Mono<ChatResponse> riskMono = generateInsight(userId, "risk", model).timeout(INSIGHT_TIMEOUT)
			.onErrorResume(e -> {
				logger.warn("Risk insight timed out or failed for user {}: {}", userId, e.getMessage());
				return Mono.just(createErrorResponse("risk", e));
			});

		Mono<ChatResponse> performanceMono = generateInsight(userId, "performance", model).timeout(INSIGHT_TIMEOUT)
			.onErrorResume(e -> {
				logger.warn("Performance insight timed out or failed for user {}: {}", userId, e.getMessage());
				return Mono.just(createErrorResponse("performance", e));
			});

		Mono<ChatResponse> rebalancingMono = generateInsight(userId, "rebalancing", model).timeout(INSIGHT_TIMEOUT)
			.onErrorResume(e -> {
				logger.warn("Rebalancing insight timed out or failed for user {}: {}", userId, e.getMessage());
				return Mono.just(createErrorResponse("rebalancing", e));
			});

		Mono<ChatResponse> opportunitiesMono = generateInsight(userId, "opportunities", model).timeout(INSIGHT_TIMEOUT)
			.onErrorResume(e -> {
				logger.warn("Opportunities insight timed out or failed for user {}: {}", userId, e.getMessage());
				return Mono.just(createErrorResponse("opportunities", e));
			});

		return Mono.zip(riskMono, performanceMono, rebalancingMono, opportunitiesMono)
			.map(tuple -> List.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()))
			.doOnSuccess(insights -> {
				logger.info("Successfully generated all 4 insights for user: {}", userId);

				// 3. Save to cache asynchronously (don't block response)
				Mono.fromRunnable(() -> saveInsightsToCache(userId, insights, model))
					.subscribeOn(Schedulers.boundedElastic())
					.subscribe(null, error -> logger.error("Failed to save insights to cache for user {}: {}", userId,
							error.getMessage()));
			})
			.doOnError(error -> {
				logger.error("Error generating all insights for user {}: {}", userId, error.getMessage(), error);
			});
	}

	/**
	 * Convert cached insights entity to list of ChatResponse
	 */
	private List<ChatResponse> convertCacheToResponses(PortfolioInsightsCacheEntity cached) {
		if (cached.getInsights() == null) {
			return new ArrayList<>();
		}

		return cached.getInsights().stream().map(insight -> {
			ChatResponse response = new ChatResponse();
			response.setContent(insight.getContent());
			response.setSuccess(Boolean.TRUE.equals(insight.getSuccess()));
			response.setModel(insight.getModel());

			// Set metadata fields
			Map<String, Object> metadata = Map.of("type", insight.getType() != null ? insight.getType() : "", "title",
					insight.getTitle() != null ? insight.getTitle() : "", "summary",
					insight.getSummary() != null ? insight.getSummary() : "", "riskLevel",
					insight.getRiskLevel() != null ? insight.getRiskLevel() : "", "generatedAt",
					insight.getGeneratedAtEpoch() != null ? insight.getGeneratedAtEpoch() : 0L, "fromCache", true);
			response.setMetadata(metadata);

			return response;
		}).collect(Collectors.toList());
	}

	/**
	 * Save generated insights to cache
	 */
	private void saveInsightsToCache(String userId, List<ChatResponse> insights, String model) {
		try {
			PortfolioInsightsCacheEntity cache = new PortfolioInsightsCacheEntity();
			cache.setModel(model);

			List<PortfolioInsightsCacheEntity.CachedInsight> cachedInsights = insights.stream().map(response -> {
				PortfolioInsightsCacheEntity.CachedInsight cached = new PortfolioInsightsCacheEntity.CachedInsight();
				cached.setContent(response.getContent());
				cached.setSuccess(response.isSuccess());
				cached.setModel(response.getModel());
				cached.setGeneratedAtEpoch(System.currentTimeMillis() / 1000);

				// Extract metadata if present
				if (response.getMetadata() != null) {
					Map<String, Object> meta = response.getMetadata();
					cached.setType((String) meta.getOrDefault("type", ""));
					cached.setTitle((String) meta.getOrDefault("title", ""));
					cached.setSummary((String) meta.getOrDefault("summary", ""));
					cached.setRiskLevel((String) meta.getOrDefault("riskLevel", ""));
				}

				return cached;
			}).collect(Collectors.toList());

			cache.setInsights(cachedInsights);
			insightsCacheRepository.saveCache(userId, cache);
			logger.info("Successfully saved insights to cache for user: {}", userId);
		}
		catch (Exception e) {
			logger.error("Failed to save insights to cache for user {}: {}", userId, e.getMessage(), e);
		}
	}

	/**
	 * Create an error response for a failed insight
	 */
	private ChatResponse createErrorResponse(String insightType, Throwable error) {
		ChatResponse response = new ChatResponse();
		response.setContent("Unable to generate " + insightType + " analysis at this time. Please try again later.");
		response.setSuccess(false);
		response.setModel("error");

		Map<String, Object> metadata = Map.of("type", insightType.toUpperCase(), "title",
				formatInsightTitle(insightType), "summary", "Analysis temporarily unavailable", "error",
				error.getMessage() != null ? error.getMessage() : "Unknown error", "generatedAt",
				System.currentTimeMillis() / 1000);
		response.setMetadata(metadata);

		return response;
	}

	/**
	 * Format insight type to title
	 */
	private String formatInsightTitle(String insightType) {
		switch (insightType.toLowerCase()) {
			case "risk":
				return "Portfolio Risk Analysis";
			case "performance":
				return "Performance Analysis";
			case "rebalancing":
				return "Rebalancing Recommendations";
			case "opportunities":
				return "Investment Opportunities";
			default:
				return insightType.substring(0, 1).toUpperCase() + insightType.substring(1) + " Analysis";
		}
	}

	/**
	 * Invalidate insights cache for a user. Should be called when provider data changes
	 * (sync, connect, disconnect).
	 * @param userId User ID
	 */
	public void invalidateInsightsCache(String userId) {
		try {
			insightsCacheRepository.invalidateCache(userId);
			logger.info("Invalidated insights cache for user: {}", userId);
		}
		catch (Exception e) {
			logger.error("Failed to invalidate insights cache for user {}: {}", userId, e.getMessage(), e);
		}
	}

	/**
	 * Conversational chat about portfolio
	 * @param userMessage User's message/question
	 * @param userId User ID
	 * @param conversationHistory Previous messages in conversation
	 * @param model LLM model to use
	 * @param providerId Optional: analyze specific provider only
	 * @return ChatResponse with AI's reply
	 */
	public Mono<ChatResponse> chat(String userMessage, String userId, List<ChatMessage> conversationHistory,
			String model, String providerId) {
		logger.info("Portfolio chat for user: {}, providerId: {}, model: {}", userId, providerId, model);

		try {
			// Fetch portfolio context (overall or provider-specific)
			Map<String, Object> portfolioContext;
			if (providerId != null && !providerId.isBlank()) {
				// TODO: Implement provider-specific context fetching
				// For now, use overall portfolio context
				portfolioContext = portfolioContextProvider.getPortfolioContext(userId);
				logger.debug("Provider-specific context not yet implemented, using overall portfolio");
			}
			else {
				portfolioContext = portfolioContextProvider.getPortfolioContext(userId);
			}

			// Build system prompt with portfolio context
			String systemPrompt = PortfolioAnalysisPrompts.buildContextualPrompt("overview", portfolioContext);

			// Create chat context with system prompt override
			ChatContext chatContext = new ChatContext();
			chatContext.setFeature("portfolio");
			chatContext.setCurrentPage("chat");
			chatContext.setPortfolioData(portfolioContext);
			chatContext.setSystemPrompt(systemPrompt); // Pass the portfolio-specific
														// prompt

			// Call AIChatBusiness
			return aiChatBusiness.chat(userMessage, chatContext, conversationHistory, model).doOnSuccess(response -> {
				logger.info("Portfolio chat response generated for user: {}, tokens: {}", userId,
						response.getTokensUsed());
			}).doOnError(error -> {
				logger.error("Error in portfolio chat for user {}: {}", userId, error.getMessage(), error);
			});
		}
		catch (Exception e) {
			logger.error("Error in portfolio chat for user {}: {}", userId, e.getMessage(), e);
			return Mono.just(ChatResponse.error("Failed to generate chat response: " + e.getMessage()));
		}
	}

	/**
	 * Streaming chat about portfolio (SSE)
	 * @param userMessage User's message/question
	 * @param userId User ID
	 * @param conversationHistory Previous messages in conversation
	 * @param model LLM model to use
	 * @param providerId Optional: analyze specific provider only
	 * @return Flux of ChatResponse chunks
	 */
	public Flux<ChatResponse> chatStream(String userMessage, String userId, List<ChatMessage> conversationHistory,
			String model, String providerId) {
		logger.info("Portfolio chat stream for user: {}, providerId: {}, model: {}", userId, providerId, model);

		try {
			// Fetch portfolio context
			Map<String, Object> portfolioContext;
			if (providerId != null && !providerId.isBlank()) {
				// TODO: Implement provider-specific context fetching
				portfolioContext = portfolioContextProvider.getPortfolioContext(userId);
			}
			else {
				portfolioContext = portfolioContextProvider.getPortfolioContext(userId);
			}

			// Build system prompt with portfolio context
			String systemPrompt = PortfolioAnalysisPrompts.buildContextualPrompt("overview", portfolioContext);

			// Create chat context with system prompt override
			ChatContext chatContext = new ChatContext();
			chatContext.setFeature("portfolio");
			chatContext.setCurrentPage("chat-stream");
			chatContext.setPortfolioData(portfolioContext);
			chatContext.setSystemPrompt(systemPrompt); // Pass the portfolio-specific
														// prompt

			// Call AIChatBusiness streaming method
			return aiChatBusiness.chatStream(userMessage, chatContext, conversationHistory, model).doOnComplete(() -> {
				logger.info("Portfolio chat stream completed for user: {}", userId);
			}).doOnError(error -> {
				logger.error("Error in portfolio chat stream for user {}: {}", userId, error.getMessage(), error);
			});
		}
		catch (Exception e) {
			logger.error("Error in portfolio chat stream for user {}: {}", userId, e.getMessage(), e);
			return Flux.just(ChatResponse.error("Failed to generate streaming response: " + e.getMessage()));
		}
	}

}
