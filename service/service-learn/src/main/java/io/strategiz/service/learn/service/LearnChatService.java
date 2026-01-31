package io.strategiz.service.learn.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.context.MarketContextProvider;
import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.learn.dto.ChatMessageDto;
import io.strategiz.service.learn.dto.ChatRequestDto;
import io.strategiz.service.learn.dto.ChatResponseDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import io.strategiz.service.base.BaseService;

/**
 * Service layer for Learn chat functionality
 */
@Service
public class LearnChatService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-learn";
	}

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final AIChatBusiness aiChatBusiness;

	private final MarketContextProvider marketContextProvider;

	private final PortfolioContextProvider portfolioContextProvider;

	public LearnChatService(AIChatBusiness aiChatBusiness, MarketContextProvider marketContextProvider,
			PortfolioContextProvider portfolioContextProvider) {
		this.aiChatBusiness = aiChatBusiness;
		this.marketContextProvider = marketContextProvider;
		this.portfolioContextProvider = portfolioContextProvider;
	}

	/**
	 * Process a chat message
	 * @param request the chat request
	 * @param userId the authenticated user ID
	 * @return ChatResponseDto
	 */
	public Mono<ChatResponseDto> chat(ChatRequestDto request, String userId) {
		log.info("Processing chat request for user: {}, feature: {}, model: {}", userId, request.getFeature(),
				request.getModel());

		try {
			// Build context
			ChatContext context = buildContext(request, userId);

			// Convert conversation history
			List<ChatMessage> history = convertHistory(request.getConversationHistory());

			// Call business layer with model selection
			return aiChatBusiness.chat(request.getMessage(), context, history, request.getModel())
				.map(this::convertToDto);
		}
		catch (Exception e) {
			log.error("Error processing chat request", e);
			return Mono.just(ChatResponseDto.error("Failed to process chat: " + e.getMessage()));
		}
	}

	/**
	 * Process a chat message with streaming response
	 * @param request the chat request
	 * @param userId the authenticated user ID
	 * @return Flux of ChatResponseDto chunks
	 */
	public Flux<ChatResponseDto> chatStream(ChatRequestDto request, String userId) {
		log.info("Processing streaming chat request for user: {}, feature: {}, model: {}", userId, request.getFeature(),
				request.getModel());

		try {
			ChatContext context = buildContext(request, userId);
			List<ChatMessage> history = convertHistory(request.getConversationHistory());

			return aiChatBusiness.chatStream(request.getMessage(), context, history, request.getModel())
				.map(this::convertToDto);
		}
		catch (Exception e) {
			log.error("Error processing streaming chat request", e);
			return Flux.just(ChatResponseDto.error("Failed to process streaming chat: " + e.getMessage()));
		}
	}

	/**
	 * Build chat context from request
	 */
	private ChatContext buildContext(ChatRequestDto request, String userId) {
		ChatContext context = new ChatContext();
		context.setUserId(userId);
		context.setFeature(request.getFeature() != null ? request.getFeature() : "learn");
		context.setCurrentPage(request.getCurrentPage());

		// Add market context if requested
		if (Boolean.TRUE.equals(request.getIncludeMarketContext())) {
			context.setMarketData(marketContextProvider.getMarketContext(userId));
		}

		// Add portfolio context if requested
		if (Boolean.TRUE.equals(request.getIncludePortfolioContext())) {
			context.setPortfolioData(portfolioContextProvider.getPortfolioContext(userId));
		}

		// Add any additional context
		if (request.getAdditionalContext() != null) {
			context.setAdditionalContext(request.getAdditionalContext());
		}

		return context;
	}

	/**
	 * Convert DTO conversation history to business model
	 */
	private List<ChatMessage> convertHistory(List<ChatMessageDto> historyDto) {
		if (historyDto == null || historyDto.isEmpty()) {
			return List.of();
		}

		return historyDto.stream()
			.map(dto -> new ChatMessage(dto.getRole(), dto.getContent()))
			.collect(Collectors.toList());
	}

	/**
	 * Convert business response to DTO
	 */
	private ChatResponseDto convertToDto(ChatResponse response) {
		ChatResponseDto dto = new ChatResponseDto();
		dto.setId(response.getId());
		dto.setContent(response.getContent());
		dto.setTimestamp(response.getTimestamp() != null ? response.getTimestamp().format(DATE_FORMATTER) : null);
		dto.setTokensUsed(response.getTokensUsed());
		dto.setModel(response.getModel());
		dto.setSuccess(response.isSuccess());
		dto.setError(response.getError());
		return dto;
	}

}
