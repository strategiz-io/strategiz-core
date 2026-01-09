package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.prompts.StrategyOptimizerPrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Strategy Optimizer agent - trading strategy optimization
 */
@Service
public class StrategyOptimizerService extends BaseService {

    private static final String AGENT_ID = "strategyOptimizer";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AIChatBusiness aiChatBusiness;
    private final PortfolioContextProvider portfolioContextProvider;

    public StrategyOptimizerService(AIChatBusiness aiChatBusiness, PortfolioContextProvider portfolioContextProvider) {
        this.aiChatBusiness = aiChatBusiness;
        this.portfolioContextProvider = portfolioContextProvider;
    }

    @Override
    protected String getModuleName() {
        return "service-agents-strategy-optimizer";
    }

    public Mono<AgentChatResponse> chat(AgentChatRequest request, String userId) {
        log.info("Strategy Optimizer chat request from user: {}, model: {}", userId, request.getModel());

        try {
            ChatContext context = buildContext(request, userId);
            List<ChatMessage> history = convertHistory(request.getConversationHistory());

            return aiChatBusiness.chat(request.getMessage(), context, history, request.getModel())
                .map(response -> convertToDto(response, AGENT_ID));
        } catch (Exception e) {
            log.error("Error processing Strategy Optimizer chat request", e);
            return Mono.just(AgentChatResponse.error(AGENT_ID, "Failed to process chat: " + e.getMessage()));
        }
    }

    public Flux<AgentChatResponse> chatStream(AgentChatRequest request, String userId) {
        log.info("Strategy Optimizer streaming chat request from user: {}, model: {}", userId, request.getModel());

        try {
            ChatContext context = buildContext(request, userId);
            List<ChatMessage> history = convertHistory(request.getConversationHistory());

            return aiChatBusiness.chatStream(request.getMessage(), context, history, request.getModel())
                .map(response -> convertToDto(response, AGENT_ID));
        } catch (Exception e) {
            log.error("Error processing Strategy Optimizer streaming chat request", e);
            return Flux.just(AgentChatResponse.error(AGENT_ID, "Failed to process streaming chat: " + e.getMessage()));
        }
    }

    private ChatContext buildContext(AgentChatRequest request, String userId) {
        ChatContext context = new ChatContext();
        context.setUserId(userId);
        context.setFeature("strategy-optimizer");

        // Build strategy context from portfolio/strategies data
        String strategyContext = buildStrategyContext(userId, request);
        context.setSystemPrompt(StrategyOptimizerPrompts.buildSystemPrompt(strategyContext));

        if (request.getAdditionalContext() != null) {
            context.setAdditionalContext(request.getAdditionalContext());
        }

        return context;
    }

    private String buildStrategyContext(String userId, AgentChatRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add portfolio context if available
        var portfolioContextMap = portfolioContextProvider.getPortfolioContext(userId);
        if (portfolioContextMap != null && !portfolioContextMap.isEmpty()) {
            sb.append("Portfolio Context:\n");
            portfolioContextMap.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
            sb.append("\n");
        }

        // Add any strategy-specific context from the request
        if (request.getAdditionalContext() != null) {
            Object strategyId = request.getAdditionalContext().get("strategyId");
            if (strategyId != null) {
                sb.append("Strategy ID: ").append(strategyId).append("\n");
            }
            Object strategyCode = request.getAdditionalContext().get("strategyCode");
            if (strategyCode != null) {
                sb.append("Strategy Code:\n").append(strategyCode).append("\n");
            }
        }

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
