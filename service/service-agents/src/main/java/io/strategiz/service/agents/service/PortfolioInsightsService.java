package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.agents.context.PortfolioAnalysisContextProvider;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.prompts.PortfolioInsightsPrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Portfolio Insights agent - comprehensive portfolio analysis Enhanced with
 * benchmark comparison, risk analysis, and holdings news
 */
@Service
public class PortfolioInsightsService extends BaseService {

	private static final String AGENT_ID = "portfolioInsights";

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final AIChatBusiness aiChatBusiness;

	private final PortfolioAnalysisContextProvider portfolioAnalysisContextProvider;

	public PortfolioInsightsService(AIChatBusiness aiChatBusiness,
			PortfolioAnalysisContextProvider portfolioAnalysisContextProvider) {
		this.aiChatBusiness = aiChatBusiness;
		this.portfolioAnalysisContextProvider = portfolioAnalysisContextProvider;
	}

	@Override
	protected String getModuleName() {
		return "service-agents-portfolio-insights";
	}

	public Mono<AgentChatResponse> chat(AgentChatRequest request, String userId) {
		log.info("Portfolio Insights chat request from user: {}, model: {}", userId, request.getModel());

		try {
			ChatContext context = buildContext(request, userId);
			List<ChatMessage> history = convertHistory(request.getConversationHistory());

			return aiChatBusiness.chat(request.getMessage(), context, history, request.getModel())
				.map(response -> convertToDto(response, AGENT_ID));
		}
		catch (Exception e) {
			log.error("Error processing Portfolio Insights chat request", e);
			return Mono.just(AgentChatResponse.error(AGENT_ID, "Failed to process chat: " + e.getMessage()));
		}
	}

	public Flux<AgentChatResponse> chatStream(AgentChatRequest request, String userId) {
		log.info("Portfolio Insights streaming chat request from user: {}, model: {}", userId, request.getModel());

		try {
			ChatContext context = buildContext(request, userId);
			List<ChatMessage> history = convertHistory(request.getConversationHistory());

			return aiChatBusiness.chatStream(request.getMessage(), context, history, request.getModel())
				.map(response -> convertToDto(response, AGENT_ID));
		}
		catch (Exception e) {
			log.error("Error processing Portfolio Insights streaming chat request", e);
			return Flux.just(AgentChatResponse.error(AGENT_ID, "Failed to process streaming chat: " + e.getMessage()));
		}
	}

	private ChatContext buildContext(AgentChatRequest request, String userId) {
		ChatContext context = new ChatContext();
		context.setUserId(userId);
		context.setFeature("portfolio-insights");

		// Build portfolio context with comprehensive analysis
		String portfolioContext = buildPortfolioContext(request, userId);
		context.setSystemPrompt(PortfolioInsightsPrompts.buildSystemPrompt(portfolioContext));

		if (request.getAdditionalContext() != null) {
			context.setAdditionalContext(request.getAdditionalContext());
		}

		return context;
	}

	private String buildPortfolioContext(AgentChatRequest request, String userId) {
		// Check message intent for specific focus
		String messageLower = request.getMessage() != null ? request.getMessage().toLowerCase() : "";

		if (messageLower.contains("risk") || messageLower.contains("concentration")
				|| messageLower.contains("diversif")) {
			log.debug("Building risk-focused portfolio context");
			return portfolioAnalysisContextProvider.buildRiskAnalysisContext(userId);
		}
		else if (messageLower.contains("rebalance") || messageLower.contains("allocation")
				|| messageLower.contains("rebalancing")) {
			log.debug("Building rebalancing-focused portfolio context");
			return portfolioAnalysisContextProvider.buildRebalancingContext(userId);
		}
		else {
			// Default: comprehensive portfolio analysis
			log.debug("Building comprehensive portfolio analysis context");
			return portfolioAnalysisContextProvider.buildPortfolioAnalysisContext(userId);
		}
	}

	private List<ChatMessage> convertHistory(List<AgentChatMessage> historyDto) {
		if (historyDto == null || historyDto.isEmpty()) {
			return List.of();
		}

		return historyDto.stream()
			.map(dto -> new ChatMessage(dto.getRole(), dto.getContent()))
			.collect(Collectors.toList());
	}

	private AgentChatResponse convertToDto(ChatResponse response, String agentId) {
		AgentChatResponse dto = new AgentChatResponse();
		dto.setId(response.getId());
		dto.setContent(response.getContent());
		dto.setTimestamp(response.getTimestamp() != null ? response.getTimestamp().format(DATE_FORMATTER) : null);
		dto.setTokensUsed(response.getTokensUsed());
		dto.setModel(response.getModel());
		dto.setAgentId(agentId);
		dto.setSuccess(response.isSuccess());
		dto.setError(response.getError());
		return dto;
	}

}
