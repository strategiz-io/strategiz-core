package io.strategiz.business.aichat;

import io.strategiz.business.aichat.context.MarketContextProvider;
import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.business.aichat.prompt.LabsPrompts;
import io.strategiz.business.aichat.prompt.LearnPrompts;
import io.strategiz.client.gemini.GeminiClient;
import io.strategiz.client.gemini.model.GeminiRequest;
import io.strategiz.client.gemini.model.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for AI-powered chat across Learn, Labs, and other features
 */
@Service
public class AIChatBusiness {

	private static final Logger logger = LoggerFactory.getLogger(AIChatBusiness.class);

	private final GeminiClient geminiClient;

	private final MarketContextProvider marketContextProvider;

	private final PortfolioContextProvider portfolioContextProvider;

	public AIChatBusiness(GeminiClient geminiClient, MarketContextProvider marketContextProvider,
			PortfolioContextProvider portfolioContextProvider) {
		this.geminiClient = geminiClient;
		this.marketContextProvider = marketContextProvider;
		this.portfolioContextProvider = portfolioContextProvider;
	}

	/**
	 * Send a chat message and get AI response
	 * @param userMessage the user's message
	 * @param context the chat context (feature, page, user data)
	 * @param conversationHistory previous messages in the conversation
	 * @return ChatResponse containing AI's reply
	 */
	public Mono<ChatResponse> chat(String userMessage, ChatContext context, List<ChatMessage> conversationHistory) {
		logger.info("Processing chat message for feature: {}, page: {}", context.getFeature(),
				context.getCurrentPage());

		try {
			// Build the enhanced prompt with system context
			String systemPrompt = buildSystemPrompt(context);
			String enhancedMessage = buildEnhancedMessage(userMessage, context);

			// Convert conversation history to Gemini format
			List<GeminiRequest.Content> geminiHistory = convertToGeminiHistory(conversationHistory, systemPrompt);

			// Call Gemini API
			return geminiClient.generateContent(enhancedMessage, geminiHistory).map(geminiResponse -> {
				ChatResponse response = new ChatResponse();
				response.setId(UUID.randomUUID().toString());
				response.setContent(geminiResponse.getText());
				response.setSuccess(true);

				// Extract token usage if available
				if (geminiResponse.getUsageMetadata() != null) {
					response.setTokensUsed(geminiResponse.getUsageMetadata().getTotalTokenCount());
				}

				logger.info("Chat response generated successfully, tokens used: {}", response.getTokensUsed());
				return response;
			})
				.onErrorResume(error -> {
					logger.error("Error generating chat response", error);
					return Mono.just(ChatResponse.error("Failed to generate response: " + error.getMessage()));
				});
		}
		catch (Exception e) {
			logger.error("Error in chat business logic", e);
			return Mono.just(ChatResponse.error("An error occurred: " + e.getMessage()));
		}
	}

	/**
	 * Send a chat message with streaming response
	 * @param userMessage the user's message
	 * @param context the chat context
	 * @param conversationHistory previous messages
	 * @return Flux of ChatResponse chunks
	 */
	public Flux<ChatResponse> chatStream(String userMessage, ChatContext context,
			List<ChatMessage> conversationHistory) {
		logger.info("Processing streaming chat message for feature: {}", context.getFeature());

		try {
			String systemPrompt = buildSystemPrompt(context);
			String enhancedMessage = buildEnhancedMessage(userMessage, context);
			List<GeminiRequest.Content> geminiHistory = convertToGeminiHistory(conversationHistory, systemPrompt);

			return geminiClient.generateContentStream(enhancedMessage, geminiHistory).map(geminiResponse -> {
				ChatResponse response = new ChatResponse();
				response.setId(UUID.randomUUID().toString());
				response.setContent(geminiResponse.getText());
				response.setSuccess(true);
				return response;
			})
				.onErrorResume(error -> {
					logger.error("Error in streaming chat", error);
					return Flux.just(ChatResponse.error("Stream error: " + error.getMessage()));
				});
		}
		catch (Exception e) {
			logger.error("Error in streaming chat business logic", e);
			return Flux.just(ChatResponse.error("An error occurred: " + e.getMessage()));
		}
	}

	/**
	 * Build system prompt based on feature and context
	 */
	private String buildSystemPrompt(ChatContext context) {
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
	 * Convert chat message history to Gemini API format
	 */
	private List<GeminiRequest.Content> convertToGeminiHistory(List<ChatMessage> conversationHistory,
			String systemPrompt) {
		List<GeminiRequest.Content> geminiHistory = new ArrayList<>();

		// Add system prompt as first message
		if (systemPrompt != null && !systemPrompt.isEmpty()) {
			geminiHistory.add(new GeminiRequest.Content("user", systemPrompt));
			geminiHistory.add(new GeminiRequest.Content("model",
					"Understood. I'm ready to assist with trading education and development."));
		}

		// Add conversation history
		if (conversationHistory != null) {
			for (ChatMessage message : conversationHistory) {
				String role = "user".equals(message.getRole()) ? "user" : "model";
				geminiHistory.add(new GeminiRequest.Content(role, message.getContent()));
			}
		}

		return geminiHistory;
	}

}
