package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.business.fundamentals.service.FundamentalsQueryService;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.prompts.EarningsEdgePrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Earnings Edge agent - earnings analysis and trading
 */
@Service
public class EarningsEdgeService extends BaseService {

    private static final String AGENT_ID = "earningsEdge";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AIChatBusiness aiChatBusiness;
    private final FundamentalsQueryService fundamentalsQueryService;

    public EarningsEdgeService(AIChatBusiness aiChatBusiness, FundamentalsQueryService fundamentalsQueryService) {
        this.aiChatBusiness = aiChatBusiness;
        this.fundamentalsQueryService = fundamentalsQueryService;
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            log.error("Error processing Earnings Edge streaming chat request", e);
            return Flux.just(AgentChatResponse.error(AGENT_ID, "Failed to process streaming chat: " + e.getMessage()));
        }
    }

    private ChatContext buildContext(AgentChatRequest request, String userId) {
        ChatContext context = new ChatContext();
        context.setUserId(userId);
        context.setFeature("earnings-edge");

        // Build earnings context from fundamentals data
        String earningsContext = buildEarningsContext(request);
        context.setSystemPrompt(EarningsEdgePrompts.buildSystemPrompt(earningsContext));

        if (request.getAdditionalContext() != null) {
            context.setAdditionalContext(request.getAdditionalContext());
        }

        return context;
    }

    private String buildEarningsContext(AgentChatRequest request) {
        StringBuilder sb = new StringBuilder();

        // Check if specific symbols are requested
        if (request.getAdditionalContext() != null) {
            Object symbols = request.getAdditionalContext().get("symbols");
            if (symbols instanceof List<?> symbolList) {
                for (Object symbol : symbolList) {
                    if (symbol instanceof String symbolStr) {
                        try {
                            var fundamentals = fundamentalsQueryService.getLatestFundamentalsOrNull(symbolStr);
                            if (fundamentals != null) {
                                sb.append(String.format("Symbol: %s\n", symbolStr));
                                sb.append(String.format("  EPS (Diluted): %s\n", fundamentals.getEpsDiluted()));
                                sb.append(String.format("  EPS Growth YoY: %s%%\n", fundamentals.getEpsGrowthYoy()));
                                sb.append(String.format("  P/E Ratio: %s\n", fundamentals.getPriceToEarnings()));
                                sb.append("\n");
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get fundamentals for symbol: {}", symbolStr, e);
                        }
                    }
                }
            }
        }

        // Add general earnings guidance if no specific data
        if (sb.length() == 0) {
            sb.append("No specific earnings data requested. ");
            sb.append("Ask about specific symbols to get their earnings data, ");
            sb.append("or ask about upcoming earnings calendar.\n");
            sb.append("\nNote: Earnings calendar data is being integrated. ");
            sb.append("Currently available: historical EPS data, P/E ratios, EPS growth rates.");
        }

        return sb.toString();
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
