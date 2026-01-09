package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.agents.context.StrategyOptimizationContextProvider;
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
 * Enhanced with comprehensive optimization frameworks and market context
 */
@Service
public class StrategyOptimizerService extends BaseService {

    private static final String AGENT_ID = "strategyOptimizer";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AIChatBusiness aiChatBusiness;
    private final StrategyOptimizationContextProvider optimizationContextProvider;

    public StrategyOptimizerService(
            AIChatBusiness aiChatBusiness,
            StrategyOptimizationContextProvider optimizationContextProvider) {
        this.aiChatBusiness = aiChatBusiness;
        this.optimizationContextProvider = optimizationContextProvider;
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

        // Build optimization context with comprehensive frameworks
        String strategyContext = buildStrategyContext(userId, request);
        context.setSystemPrompt(StrategyOptimizerPrompts.buildSystemPrompt(strategyContext));

        if (request.getAdditionalContext() != null) {
            context.setAdditionalContext(request.getAdditionalContext());
        }

        return context;
    }

    private String buildStrategyContext(String userId, AgentChatRequest request) {
        // Extract parameters from request
        String strategyCode = null;
        String strategyType = null;

        if (request.getAdditionalContext() != null) {
            Object codeObj = request.getAdditionalContext().get("strategyCode");
            if (codeObj != null) {
                strategyCode = codeObj.toString();
            }

            Object typeObj = request.getAdditionalContext().get("strategyType");
            if (typeObj != null) {
                strategyType = typeObj.toString();
            }
        }

        // Detect strategy type from message if not provided
        if (strategyType == null) {
            strategyType = detectStrategyType(request.getMessage());
        }

        // Build appropriate context based on request
        if (strategyCode != null && !strategyCode.isBlank()) {
            // User provided strategy code - full optimization context
            log.debug("Building optimization context with strategy code");
            return optimizationContextProvider.buildOptimizationContext(userId, strategyCode, strategyType);
        } else if (strategyType != null) {
            // User asking about a specific strategy type
            log.debug("Building strategy type context for: {}", strategyType);
            return optimizationContextProvider.buildStrategyTypeContext(strategyType);
        } else {
            // General strategy analysis context
            log.debug("Building general strategy analysis context");
            return optimizationContextProvider.buildStrategyAnalysisContext(userId);
        }
    }

    /**
     * Detect strategy type from user message
     */
    private String detectStrategyType(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String messageLower = message.toLowerCase();

        if (messageLower.contains("momentum")) {
            return "momentum";
        } else if (messageLower.contains("mean reversion") || messageLower.contains("reversal")) {
            return "mean-reversion";
        } else if (messageLower.contains("breakout")) {
            return "breakout";
        } else if (messageLower.contains("trend follow")) {
            return "trend-following";
        } else if (messageLower.contains("pairs") || messageLower.contains("arbitrage")) {
            return "pairs-trading";
        } else if (messageLower.contains("scalp")) {
            return "scalping";
        }

        return null;
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
