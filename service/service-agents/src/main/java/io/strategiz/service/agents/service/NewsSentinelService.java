package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.context.MarketContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.prompts.NewsSentinelPrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for News Sentinel agent - real-time news and sentiment analysis
 */
@Service
public class NewsSentinelService extends BaseService {

    private static final String AGENT_ID = "newsSentinel";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AIChatBusiness aiChatBusiness;
    private final MarketContextProvider marketContextProvider;

    public NewsSentinelService(AIChatBusiness aiChatBusiness, MarketContextProvider marketContextProvider) {
        this.aiChatBusiness = aiChatBusiness;
        this.marketContextProvider = marketContextProvider;
    }

    @Override
    protected String getModuleName() {
        return "service-agents-news-sentinel";
    }

    public Mono<AgentChatResponse> chat(AgentChatRequest request, String userId) {
        log.info("News Sentinel chat request from user: {}, model: {}", userId, request.getModel());

        try {
            ChatContext context = buildContext(request, userId);
            List<ChatMessage> history = convertHistory(request.getConversationHistory());

            return aiChatBusiness.chat(request.getMessage(), context, history, request.getModel())
                .map(response -> convertToDto(response, AGENT_ID));
        } catch (Exception e) {
            log.error("Error processing News Sentinel chat request", e);
            return Mono.just(AgentChatResponse.error(AGENT_ID, "Failed to process chat: " + e.getMessage()));
        }
    }

    public Flux<AgentChatResponse> chatStream(AgentChatRequest request, String userId) {
        log.info("News Sentinel streaming chat request from user: {}, model: {}", userId, request.getModel());

        try {
            ChatContext context = buildContext(request, userId);
            List<ChatMessage> history = convertHistory(request.getConversationHistory());

            return aiChatBusiness.chatStream(request.getMessage(), context, history, request.getModel())
                .map(response -> convertToDto(response, AGENT_ID));
        } catch (Exception e) {
            log.error("Error processing News Sentinel streaming chat request", e);
            return Flux.just(AgentChatResponse.error(AGENT_ID, "Failed to process streaming chat: " + e.getMessage()));
        }
    }

    private ChatContext buildContext(AgentChatRequest request, String userId) {
        ChatContext context = new ChatContext();
        context.setUserId(userId);
        context.setFeature("news-sentinel");

        // Build news context
        String newsContext = buildNewsContext(userId, request);
        context.setSystemPrompt(NewsSentinelPrompts.buildSystemPrompt(newsContext));

        if (request.getAdditionalContext() != null) {
            context.setAdditionalContext(request.getAdditionalContext());
        }

        return context;
    }

    private String buildNewsContext(String userId, AgentChatRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add market context for sentiment
        var marketContextMap = marketContextProvider.getMarketContext(userId);
        if (marketContextMap != null && !marketContextMap.isEmpty()) {
            sb.append("Current Market Sentiment:\n");
            marketContextMap.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
            sb.append("\n");
        }

        // Check for specific symbols or sectors in the request
        if (request.getAdditionalContext() != null) {
            Object symbols = request.getAdditionalContext().get("symbols");
            if (symbols instanceof List<?> symbolList) {
                sb.append("Watchlist symbols: ").append(symbolList).append("\n");
            }

            Object sector = request.getAdditionalContext().get("sector");
            if (sector != null) {
                sb.append("Focus sector: ").append(sector).append("\n");
            }

            Object newsType = request.getAdditionalContext().get("newsType");
            if (newsType != null) {
                sb.append("News type focus: ").append(newsType).append("\n");
            }
        }

        // Add current timestamp for context
        sb.append("\nCurrent time: ").append(java.time.LocalDateTime.now().format(DATE_FORMATTER));

        return sb.length() > 0 ? sb.toString() : null;
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
