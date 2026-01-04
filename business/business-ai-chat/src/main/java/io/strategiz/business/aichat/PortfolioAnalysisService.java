package io.strategiz.business.aichat;

import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.business.aichat.prompt.PortfolioAnalysisPrompts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for AI-powered portfolio analysis.
 * Generates insights for Risk Analysis, Performance Analysis, Rebalancing, and Investment Opportunities.
 */
@Service
public class PortfolioAnalysisService {

	private static final Logger logger = LoggerFactory.getLogger(PortfolioAnalysisService.class);

	private final AIChatBusiness aiChatBusiness;

	private final PortfolioContextProvider portfolioContextProvider;

	public PortfolioAnalysisService(AIChatBusiness aiChatBusiness, PortfolioContextProvider portfolioContextProvider) {
		this.aiChatBusiness = aiChatBusiness;
		this.portfolioContextProvider = portfolioContextProvider;
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

			// 3. Create chat context
			ChatContext chatContext = new ChatContext();
			chatContext.setFeature("portfolio");
			chatContext.setCurrentPage("analysis");
			chatContext.setPortfolioData(portfolioContext);

			// 4. Generate insight using AIChatBusiness
			String userMessage = String.format("Provide a %s analysis for my portfolio.", insightType);

			return aiChatBusiness.chat(userMessage, chatContext, new ArrayList<>(), model).doOnSuccess(response -> {
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
	 * Opportunities)
	 * @param userId User ID
	 * @param model LLM model to use
	 * @return List of ChatResponse objects, one for each insight type
	 */
	public Mono<List<ChatResponse>> generateAllInsights(String userId, String model) {
		logger.info("Generating all insights for user: {}, model: {}", userId, model);

		// Generate all 4 insight types in parallel
		Mono<ChatResponse> riskMono = generateInsight(userId, "risk", model);
		Mono<ChatResponse> performanceMono = generateInsight(userId, "performance", model);
		Mono<ChatResponse> rebalancingMono = generateInsight(userId, "rebalancing", model);
		Mono<ChatResponse> opportunitiesMono = generateInsight(userId, "opportunities", model);

		return Mono.zip(riskMono, performanceMono, rebalancingMono, opportunitiesMono)
			.map(tuple -> List.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()))
			.doOnSuccess(insights -> {
				logger.info("Successfully generated all 4 insights for user: {}", userId);
			})
			.doOnError(error -> {
				logger.error("Error generating all insights for user {}: {}", userId, error.getMessage(), error);
			});
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

			// Create chat context
			ChatContext chatContext = new ChatContext();
			chatContext.setFeature("portfolio");
			chatContext.setCurrentPage("chat");
			chatContext.setPortfolioData(portfolioContext);

			// Call AIChatBusiness
			return aiChatBusiness.chat(userMessage, chatContext, conversationHistory, model)
				.doOnSuccess(response -> {
					logger.info("Portfolio chat response generated for user: {}, tokens: {}", userId,
							response.getTokensUsed());
				})
				.doOnError(error -> {
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

			// Create chat context
			ChatContext chatContext = new ChatContext();
			chatContext.setFeature("portfolio");
			chatContext.setCurrentPage("chat-stream");
			chatContext.setPortfolioData(portfolioContext);

			// Call AIChatBusiness streaming method
			return aiChatBusiness.chatStream(userMessage, chatContext, conversationHistory, model)
				.doOnComplete(() -> {
					logger.info("Portfolio chat stream completed for user: {}", userId);
				})
				.doOnError(error -> {
					logger.error("Error in portfolio chat stream for user {}: {}", userId, error.getMessage(), error);
				});
		}
		catch (Exception e) {
			logger.error("Error in portfolio chat stream for user {}: {}", userId, e.getMessage(), e);
			return Flux.just(ChatResponse.error("Failed to generate streaming response: " + e.getMessage()));
		}
	}

}
