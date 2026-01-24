package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.agents.context.MarketSignalsContextProvider;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.prompts.SignalScoutPrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Signal Scout agent - market signal analysis. Enhanced with Financial Modeling Prep
 * (FMP) API for real-time market data including quotes, technical indicators (RSI, MACD, SMA),
 * sector performance, and market news.
 */
@Service
public class SignalScoutService extends BaseService {

    private static final String AGENT_ID = "signalScout";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AIChatBusiness aiChatBusiness;
    private final MarketSignalsContextProvider marketSignalsContextProvider;

    public SignalScoutService(AIChatBusiness aiChatBusiness, MarketSignalsContextProvider marketSignalsContextProvider) {
        this.aiChatBusiness = aiChatBusiness;
        this.marketSignalsContextProvider = marketSignalsContextProvider;
    }

    @Override
    protected String getModuleName() {
        return "service-agents-signal-scout";
    }

    public Mono<AgentChatResponse> chat(AgentChatRequest request, String userId) {
        log.info("Signal Scout chat request from user: {}, model: {}", userId, request.getModel());

        try {
            ChatContext context = buildContext(request, userId);
            List<ChatMessage> history = convertHistory(request.getConversationHistory());

            return aiChatBusiness.chat(request.getMessage(), context, history, request.getModel())
                .map(response -> convertToDto(response, AGENT_ID));
        } catch (Exception e) {
            log.error("Error processing Signal Scout chat request", e);
            return Mono.just(AgentChatResponse.error(AGENT_ID, "Failed to process chat: " + e.getMessage()));
        }
    }

    public Flux<AgentChatResponse> chatStream(AgentChatRequest request, String userId) {
        log.info("Signal Scout streaming chat request from user: {}, model: {}", userId, request.getModel());

        try {
            ChatContext context = buildContext(request, userId);
            List<ChatMessage> history = convertHistory(request.getConversationHistory());

            return aiChatBusiness.chatStream(request.getMessage(), context, history, request.getModel())
                .map(response -> convertToDto(response, AGENT_ID));
        } catch (Exception e) {
            log.error("Error processing Signal Scout streaming chat request", e);
            return Flux.just(AgentChatResponse.error(AGENT_ID, "Failed to process streaming chat: " + e.getMessage()));
        }
    }

    private ChatContext buildContext(AgentChatRequest request, String userId) {
        ChatContext context = new ChatContext();
        context.setUserId(userId);
        context.setFeature("signal-scout");

        // Build market signals context with real data
        String marketContext = buildMarketContext(request);
        context.setSystemPrompt(SignalScoutPrompts.buildSystemPrompt(marketContext));

        if (request.getAdditionalContext() != null) {
            context.setAdditionalContext(request.getAdditionalContext());
        }

        return context;
    }

    @SuppressWarnings("unchecked")
    private String buildMarketContext(AgentChatRequest request) {
        // Extract parameters from request
        List<String> symbols = null;
        String focusArea = null;

        if (request.getAdditionalContext() != null) {
            Object symbolsObj = request.getAdditionalContext().get("symbols");
            if (symbolsObj instanceof List<?>) {
                symbols = ((List<?>) symbolsObj).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }

            Object focusObj = request.getAdditionalContext().get("focusArea");
            if (focusObj != null) {
                focusArea = focusObj.toString();
            }
        }

        // Check message intent for specific focus
        String messageLower = request.getMessage() != null ? request.getMessage().toLowerCase() : "";

        if (messageLower.contains("sector") || messageLower.contains("rotation")) {
            log.debug("Building sector rotation context");
            return marketSignalsContextProvider.buildSectorRotationContext();
        } else if (messageLower.contains("market") && (messageLower.contains("condition") || messageLower.contains("overview"))) {
            log.debug("Building market conditions context");
            return marketSignalsContextProvider.buildMarketConditionsContext();
        } else {
            // Default: comprehensive market signals context
            log.debug("Building comprehensive market signals context");
            return marketSignalsContextProvider.buildMarketSignalsContext(symbols);
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
