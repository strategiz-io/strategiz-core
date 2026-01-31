package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.client.fmp.client.FmpFundamentalsClient;
import io.strategiz.client.fmp.dto.FmpEarningsEvent;
import io.strategiz.service.agents.context.EarningsContextProvider;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.dto.EarningsInsightDto;
import io.strategiz.service.agents.prompts.EarningsEdgePrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Earnings Edge agent - earnings analysis and trading. Uses FMP API for real
 * earnings calendar data.
 */
@Service
public class EarningsEdgeService extends BaseService {

	private static final String AGENT_ID = "earningsEdge";

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final AIChatBusiness aiChatBusiness;

	private final EarningsContextProvider earningsContextProvider;

	private final FmpFundamentalsClient fmpFundamentalsClient;

	public EarningsEdgeService(AIChatBusiness aiChatBusiness, EarningsContextProvider earningsContextProvider,
			FmpFundamentalsClient fmpFundamentalsClient) {
		this.aiChatBusiness = aiChatBusiness;
		this.earningsContextProvider = earningsContextProvider;
		this.fmpFundamentalsClient = fmpFundamentalsClient;
	}

	@Override
	protected String getModuleName() {
		return "service-agents-earnings-edge";
	}

	public Mono<AgentChatResponse> chat(AgentChatRequest request, String userId) {
		log.info("Earnings Edge chat request from user: {}, model: {}", userId, request.getModel());

		try {
			ChatContext context = buildContext(request, userId);
			List<ChatMessage> history = convertHistory(request.getConversationHistory());

			return aiChatBusiness.chat(request.getMessage(), context, history, request.getModel())
				.map(response -> convertToDto(response, AGENT_ID));
		}
		catch (Exception e) {
			log.error("Error processing Earnings Edge chat request", e);
			return Mono.just(AgentChatResponse.error(AGENT_ID, "Failed to process chat: " + e.getMessage()));
		}
	}

	public Flux<AgentChatResponse> chatStream(AgentChatRequest request, String userId) {
		log.info("Earnings Edge streaming chat request from user: {}, model: {}", userId, request.getModel());

		try {
			ChatContext context = buildContext(request, userId);
			List<ChatMessage> history = convertHistory(request.getConversationHistory());

			return aiChatBusiness.chatStream(request.getMessage(), context, history, request.getModel())
				.map(response -> convertToDto(response, AGENT_ID));
		}
		catch (Exception e) {
			log.error("Error processing Earnings Edge streaming chat request", e);
			return Flux.just(AgentChatResponse.error(AGENT_ID, "Failed to process streaming chat: " + e.getMessage()));
		}
	}

	/**
	 * Get upcoming earnings for the insights panel.
	 * @param limit Maximum number of earnings to return
	 * @return List of upcoming earnings events
	 */
	public List<EarningsInsightDto> getEarningsInsights(int limit) {
		log.debug("Fetching earnings insights, limit: {}", limit);
		List<EarningsInsightDto> results = new ArrayList<>();

		if (fmpFundamentalsClient == null || !fmpFundamentalsClient.isConfigured()) {
			log.warn("FMP client not configured for earnings");
			return results;
		}

		try {
			// Get upcoming earnings for next 7 days
			List<FmpEarningsEvent> events = fmpFundamentalsClient.getUpcomingEarnings(7);

			for (FmpEarningsEvent event : events) {
				if (results.size() >= limit) {
					break;
				}
				// Skip events without a symbol
				if (event.getSymbol() == null || event.getSymbol().isBlank()) {
					continue;
				}

				EarningsInsightDto dto = new EarningsInsightDto();
				dto.setSymbol(event.getSymbol());
				dto.setDate(event.getDate());
				dto.setTiming(event.getTimingDescription());
				dto.setEpsEstimate(event.getEpsEstimated());
				dto.setEpsActual(event.getEpsActual());
				dto.setRevenueEstimate(event.getRevenueEstimated());
				dto.setRevenueActual(event.getRevenueActual());
				results.add(dto);
			}
		}
		catch (Exception e) {
			log.error("Failed to fetch earnings insights: {}", e.getMessage());
		}

		return results;
	}

	private ChatContext buildContext(AgentChatRequest request, String userId) {
		ChatContext context = new ChatContext();
		context.setUserId(userId);
		context.setFeature("earnings-edge");

		// Build earnings context with real FMP data
		String earningsContext = buildEarningsContext(request);
		context.setSystemPrompt(EarningsEdgePrompts.buildSystemPrompt(earningsContext));

		if (request.getAdditionalContext() != null) {
			context.setAdditionalContext(request.getAdditionalContext());
		}

		return context;
	}

	@SuppressWarnings("unchecked")
	private String buildEarningsContext(AgentChatRequest request) {
		// Extract parameters from request
		List<String> symbols = null;
		String focusType = null;
		String focusSymbol = null;

		if (request.getAdditionalContext() != null) {
			Object symbolsObj = request.getAdditionalContext().get("symbols");
			if (symbolsObj instanceof List<?>) {
				symbols = ((List<?>) symbolsObj).stream().map(Object::toString).collect(Collectors.toList());
			}

			Object focusObj = request.getAdditionalContext().get("focusType");
			if (focusObj != null) {
				focusType = focusObj.toString();
			}

			Object symbolObj = request.getAdditionalContext().get("focusSymbol");
			if (symbolObj != null) {
				focusSymbol = symbolObj.toString();
			}
		}

		// Check if user is asking about a specific symbol
		String extractedSymbol = extractSymbolFromMessage(request.getMessage());
		if (extractedSymbol != null) {
			focusSymbol = extractedSymbol;
		}

		// Build appropriate context based on request type
		if (focusSymbol != null) {
			// User is asking about a specific symbol's earnings
			log.debug("Building symbol-focused earnings context for: {}", focusSymbol);
			return earningsContextProvider.buildSymbolEarningsContext(focusSymbol);
		}
		else if (symbols != null && !symbols.isEmpty()) {
			// User has a watchlist
			log.debug("Building watchlist earnings context for {} symbols", symbols.size());
			return earningsContextProvider.buildEarningsContext(symbols, focusType);
		}
		else {
			// General upcoming earnings
			log.debug("Building general upcoming earnings context");
			return earningsContextProvider.buildUpcomingEarningsContext();
		}
	}

	/**
	 * Extract stock symbol from user message
	 */
	private String extractSymbolFromMessage(String message) {
		if (message == null || message.isBlank()) {
			return null;
		}

		String upperMessage = message.toUpperCase();

		// Common patterns: "earnings for AAPL", "AAPL earnings", "when does TSLA report",
		// etc.
		String[] words = upperMessage.split("\\s+");
		for (String word : words) {
			// Remove common punctuation
			word = word.replaceAll("[^A-Z]", "");
			// Check if it looks like a stock symbol (1-5 uppercase letters)
			if (word.length() >= 2 && word.length() <= 5 && word.matches("[A-Z]+")) {
				// Exclude common words that might look like symbols
				if (!isCommonWord(word)) {
					return word;
				}
			}
		}

		return null;
	}

	private boolean isCommonWord(String word) {
		// Common words that might be mistaken for symbols
		return switch (word) {
			case "THE", "FOR", "AND", "OR", "IS", "IT", "TO", "OF", "IN", "ON", "AT", "EPS", "ANY", "ALL", "NEW", "NOW",
					"UP", "DOWN", "GO", "BE", "DO", "HAS", "HAD", "WAS", "ARE", "CAN", "MAY", "GET", "WHEN", "WHAT",
					"WILL", "NEXT", "WEEK", "THIS", "LAST", "BEAT", "MISS", "DATE", "TIME" ->
				true;
			default -> false;
		};
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
