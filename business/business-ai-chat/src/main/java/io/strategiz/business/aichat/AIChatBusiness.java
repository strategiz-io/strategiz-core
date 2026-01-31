package io.strategiz.business.aichat;

import io.strategiz.business.aichat.context.MarketContextProvider;
import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.business.aichat.prompt.LabsPrompts;
import io.strategiz.business.aichat.prompt.LearnPrompts;
import io.strategiz.business.preferences.service.TokenUsageService;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.base.llm.model.ModelInfo;
import io.strategiz.data.preferences.entity.TokenUsageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for AI-powered chat across Learn, Labs, and other features. Uses
 * LLMRouter to support multiple LLM providers (Gemini, Claude, etc.)
 */
@Service
public class AIChatBusiness {

	private static final Logger logger = LoggerFactory.getLogger(AIChatBusiness.class);

	private final LLMRouter llmRouter;

	private final MarketContextProvider marketContextProvider;

	private final PortfolioContextProvider portfolioContextProvider;

	private final TokenUsageService tokenUsageService;

	@Autowired
	public AIChatBusiness(LLMRouter llmRouter, MarketContextProvider marketContextProvider,
			PortfolioContextProvider portfolioContextProvider,
			@Autowired(required = false) TokenUsageService tokenUsageService) {
		this.llmRouter = llmRouter;
		this.marketContextProvider = marketContextProvider;
		this.portfolioContextProvider = portfolioContextProvider;
		this.tokenUsageService = tokenUsageService;
	}

	/**
	 * Send a chat message and get AI response using the specified model
	 * @param userMessage the user's message
	 * @param context the chat context (feature, page, user data)
	 * @param conversationHistory previous messages in the conversation
	 * @param model the LLM model to use (e.g., "gemini-1.5-flash", "claude-3-5-sonnet")
	 * @return ChatResponse containing AI's reply
	 */
	public Mono<ChatResponse> chat(String userMessage, ChatContext context, List<ChatMessage> conversationHistory,
			String model) {
		logger.info("Processing chat message for feature: {}, page: {}, model: {}", context.getFeature(),
				context.getCurrentPage(), model);

		String userId = context.getUserId();

		// Check credits before making LLM call (if token tracking is enabled)
		if (tokenUsageService != null && userId != null) {
			try {
				TokenUsageService.UsageStatus status = tokenUsageService.checkUsageStatus(userId);
				if (status.isBlocked()) {
					ChatResponse blocked = new ChatResponse();
					blocked.setSuccess(false);
					blocked
						.setError("You've used all your credits for this billing period. Please upgrade to continue.");
					blocked.setUsageWarningLevel("blocked");
					blocked.setRemainingCredits(0);
					return Mono.just(blocked);
				}
			}
			catch (Exception e) {
				logger.warn("Failed to check usage status for user {}: {}", userId, e.getMessage());
				// Continue with the request - don't block on tracking failures
			}
		}

		try {
			// Build the enhanced prompt with system context
			String systemPrompt = buildSystemPrompt(context);
			String enhancedMessage = buildEnhancedMessage(userMessage, context);

			// Convert conversation history to LLM format
			List<LLMMessage> llmHistory = convertToLLMHistory(conversationHistory, systemPrompt);

			// Call LLM via router
			return llmRouter.generateContent(enhancedMessage, llmHistory, model).map(llmResponse -> {
				ChatResponse response = new ChatResponse();
				response.setId(UUID.randomUUID().toString());
				response.setContent(llmResponse.getContent());
				response.setModel(llmResponse.getModel());
				response.setSuccess(llmResponse.isSuccess());

				// Extract token usage if available
				if (llmResponse.getTotalTokens() != null) {
					response.setTokensUsed(llmResponse.getTotalTokens());
				}

				if (!llmResponse.isSuccess()) {
					// Convert technical error to user-friendly message
					response.setError(sanitizeErrorMessage(llmResponse.getError()));
					logger.warn("LLM error occurred: {}", llmResponse.getError());
				}
				else {
					// Record token usage for successful responses
					recordUsageAndUpdateResponse(userId, llmResponse, context.getFeature(), response);
				}

				logger.info("Chat response generated successfully, model: {}, tokens used: {}, credits: {}",
						response.getModel(), response.getTokensUsed(), response.getCreditsUsed());
				return response;
			}).onErrorResume(error -> {
				logger.error("Error generating chat response", error);
				return Mono
					.just(ChatResponse.error("Our AI is temporarily unavailable. Please try again in a moment."));
			});
		}
		catch (Exception e) {
			logger.error("Error in chat business logic", e);
			return Mono.just(ChatResponse.error("Our AI is temporarily unavailable. Please try again in a moment."));
		}
	}

	/**
	 * Send a chat message using the default model (backward compatibility)
	 */
	public Mono<ChatResponse> chat(String userMessage, ChatContext context, List<ChatMessage> conversationHistory) {
		return chat(userMessage, context, conversationHistory, null);
	}

	/**
	 * Send a chat message with streaming response
	 * @param userMessage the user's message
	 * @param context the chat context
	 * @param conversationHistory previous messages
	 * @param model the LLM model to use
	 * @return Flux of ChatResponse chunks
	 */
	public Flux<ChatResponse> chatStream(String userMessage, ChatContext context, List<ChatMessage> conversationHistory,
			String model) {
		logger.info("Processing streaming chat message for feature: {}, model: {}", context.getFeature(), model);

		try {
			String systemPrompt = buildSystemPrompt(context);
			String enhancedMessage = buildEnhancedMessage(userMessage, context);
			List<LLMMessage> llmHistory = convertToLLMHistory(conversationHistory, systemPrompt);

			return llmRouter.generateContentStream(enhancedMessage, llmHistory, model).map(llmResponse -> {
				ChatResponse response = new ChatResponse();
				response.setId(UUID.randomUUID().toString());
				response.setContent(llmResponse.getContent());
				response.setModel(llmResponse.getModel());
				response.setSuccess(llmResponse.isSuccess());

				// Convert technical error to user-friendly message for streaming too
				if (!llmResponse.isSuccess()) {
					response.setError(sanitizeErrorMessage(llmResponse.getError()));
					logger.warn("LLM streaming error occurred: {}", llmResponse.getError());
				}

				return response;
			}).onErrorResume(error -> {
				logger.error("Error in streaming chat", error);
				return Flux
					.just(ChatResponse.error("Our AI is temporarily unavailable. Please try again in a moment."));
			});
		}
		catch (Exception e) {
			logger.error("Error in streaming chat business logic", e);
			return Flux.just(ChatResponse.error("Our AI is temporarily unavailable. Please try again in a moment."));
		}
	}

	/**
	 * Send a streaming chat message using the default model (backward compatibility)
	 */
	public Flux<ChatResponse> chatStream(String userMessage, ChatContext context,
			List<ChatMessage> conversationHistory) {
		return chatStream(userMessage, context, conversationHistory, null);
	}

	/**
	 * Get available LLM models
	 * @return list of ModelInfo objects
	 */
	public List<ModelInfo> getAvailableModels() {
		return llmRouter.getAvailableModels();
	}

	/**
	 * Get the default model
	 */
	public String getDefaultModel() {
		return llmRouter.getDefaultModel();
	}

	/**
	 * Build system prompt based on feature and context
	 */
	private String buildSystemPrompt(ChatContext context) {
		// Check for explicit system prompt override first
		if (context.getSystemPrompt() != null && !context.getSystemPrompt().isBlank()) {
			return context.getSystemPrompt();
		}

		String feature = context.getFeature();
		String currentPage = context.getCurrentPage();

		if ("learn".equalsIgnoreCase(feature)) {
			return LearnPrompts.buildContextualPrompt("", currentPage);
		}
		else if ("labs".equalsIgnoreCase(feature)) {
			return LabsPrompts.buildContextualPrompt("", currentPage);
		}

		// Default educational prompt
		return LearnPrompts.SYSTEM_PROMPT;
	}

	/**
	 * Enhance user message with contextual data
	 */
	private String buildEnhancedMessage(String userMessage, ChatContext context) {
		StringBuilder enhanced = new StringBuilder();

		// Add market context if relevant
		if (context.getMarketData() != null && !context.getMarketData().isEmpty()) {
			enhanced.append("[Market Context: ").append(context.getMarketData().toString()).append("]\n\n");
		}

		// Add portfolio context if relevant
		if (context.getPortfolioData() != null && !context.getPortfolioData().isEmpty()) {
			enhanced.append("[Portfolio Context: ").append(context.getPortfolioData().toString()).append("]\n\n");
		}

		enhanced.append(userMessage);

		return enhanced.toString();
	}

	/**
	 * Convert chat message history to LLM message format
	 */
	private List<LLMMessage> convertToLLMHistory(List<ChatMessage> conversationHistory, String systemPrompt) {
		List<LLMMessage> llmHistory = new ArrayList<>();

		// Add system prompt as first message pair
		if (systemPrompt != null && !systemPrompt.isEmpty()) {
			llmHistory.add(LLMMessage.user(systemPrompt));
			llmHistory
				.add(LLMMessage.assistant("Understood. I'm ready to assist with trading education and development."));
		}

		// Add conversation history
		if (conversationHistory != null) {
			for (ChatMessage message : conversationHistory) {
				String role = "user".equals(message.getRole()) ? "user" : "assistant";
				llmHistory.add(new LLMMessage(role, message.getContent()));
			}
		}

		return llmHistory;
	}

	/**
	 * Record token usage and update the response with credit information.
	 * @param userId the user ID
	 * @param llmResponse the LLM response with token counts
	 * @param feature the feature that made the request (for analytics)
	 * @param response the ChatResponse to update
	 */
	private void recordUsageAndUpdateResponse(String userId, LLMResponse llmResponse, String feature,
			ChatResponse response) {
		if (tokenUsageService == null || userId == null) {
			return;
		}

		try {
			int promptTokens = llmResponse.getPromptTokens() != null ? llmResponse.getPromptTokens() : 0;
			int completionTokens = llmResponse.getCompletionTokens() != null ? llmResponse.getCompletionTokens() : 0;
			String modelId = llmResponse.getModel();

			// Determine request type from feature
			String requestType = feature != null ? feature.toLowerCase() : "chat";

			// Record the usage
			TokenUsageRecord record = tokenUsageService.recordUsage(userId, modelId, promptTokens, completionTokens,
					requestType);

			// Update response with credit information
			response.setCreditsUsed(record.getCreditsConsumed());

			// Get current usage status
			TokenUsageService.UsageStatus status = tokenUsageService.checkUsageStatus(userId);
			response.setRemainingCredits(status.remainingCredits());
			response.setUsageWarningLevel(status.level());

			logger.debug("Recorded usage for user {}: {} credits consumed, {} remaining, warning level: {}", userId,
					record.getCreditsConsumed(), status.remainingCredits(), status.level());
		}
		catch (Exception e) {
			logger.warn("Failed to record usage for user {}: {}", userId, e.getMessage());
			// Don't fail the response - just skip tracking
		}
	}

	/**
	 * Sanitize technical error messages to be user-friendly. Logs the full technical
	 * error for debugging while showing a friendly message to users.
	 * @param technicalError the raw error from LLM provider
	 * @return user-friendly error message
	 */
	private String sanitizeErrorMessage(String technicalError) {
		if (technicalError == null) {
			return "Our AI assistant is temporarily unavailable. Please try again in a moment.";
		}

		String lowerError = technicalError.toLowerCase();

		// Authentication/authorization errors
		if (lowerError.contains("401") || lowerError.contains("unauthorized") || lowerError.contains("unauthenticated")
				|| lowerError.contains("access_token_expired") || lowerError.contains("invalid authentication")) {
			return "Our AI assistant is experiencing authentication issues. Our team has been notified. Please try again shortly.";
		}

		// Rate limiting errors
		if (lowerError.contains("429") || lowerError.contains("rate limit") || lowerError.contains("quota")) {
			return "Our AI is currently experiencing high demand. Please try again in a few moments.";
		}

		// Service unavailable errors
		if (lowerError.contains("503") || lowerError.contains("service unavailable")
				|| lowerError.contains("temporarily unavailable")) {
			return "Our AI assistant is temporarily unavailable. Please try again in a moment.";
		}

		// Timeout errors
		if (lowerError.contains("timeout") || lowerError.contains("timed out")) {
			return "The AI took too long to respond. Please try a shorter message or try again.";
		}

		// Network errors
		if (lowerError.contains("network") || lowerError.contains("connection") || lowerError.contains("unreachable")) {
			return "Unable to reach our AI service. Please check your connection and try again.";
		}

		// Model-specific errors (don't expose which model failed)
		if (lowerError.contains("failed to call") || lowerError.contains("api error")) {
			return "Our AI assistant encountered an error. Please try again or contact support if the issue persists.";
		}

		// Generic fallback
		return "Our AI assistant is temporarily unavailable. Please try again in a moment.";
	}

}
