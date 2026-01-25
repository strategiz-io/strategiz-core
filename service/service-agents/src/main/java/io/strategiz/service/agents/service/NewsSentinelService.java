package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.service.agents.context.NewsContextProvider;
import io.strategiz.client.fmp.client.FmpNewsClient;
import io.strategiz.client.fmp.dto.FmpNewsArticle;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.dto.NewsItemDto;
import io.strategiz.service.agents.context.NewsContextProvider;
import io.strategiz.service.agents.prompts.NewsSentinelPrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for News Sentinel agent - real-time news and sentiment analysis.
 * Uses FMP API for stock news, market news, and press releases.
 * Uses Finnhub API for SEC filings and earnings calendar data.
 */
@Service
@ConditionalOnProperty(name = "strategiz.fmp.enabled", havingValue = "true")
public class NewsSentinelService extends BaseService {

    private static final String AGENT_ID = "newsSentinel";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AIChatBusiness aiChatBusiness;
    private final NewsContextProvider newsContextProvider;
    private final FmpNewsClient fmpNewsClient;

    public NewsSentinelService(AIChatBusiness aiChatBusiness, NewsContextProvider newsContextProvider,
            FmpNewsClient fmpNewsClient) {
        this.aiChatBusiness = aiChatBusiness;
        this.newsContextProvider = newsContextProvider;
        this.fmpNewsClient = fmpNewsClient;
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

    /**
     * Get news items for the insights panel.
     * @param limit Maximum number of news items to return
     * @return List of news items
     */
    public List<NewsItemDto> getNewsInsights(int limit) {
        log.debug("Fetching news insights, limit: {}", limit);
        List<NewsItemDto> newsItems = new ArrayList<>();

        if (fmpNewsClient == null || !fmpNewsClient.isConfigured()) {
            log.warn("FMP News client not configured");
            return newsItems;
        }

        try {
            List<FmpNewsArticle> articles = fmpNewsClient.getGeneralNews();
            int count = 0;
            for (FmpNewsArticle article : articles) {
                if (count >= limit) {
                    break;
                }
                NewsItemDto dto = new NewsItemDto();
                dto.setTitle(article.getTitle());
                dto.setSummary(article.getText() != null && article.getText().length() > 200
                        ? article.getText().substring(0, 200) + "..."
                        : article.getText());
                dto.setSource(article.getSite());
                dto.setUrl(article.getUrl());
                dto.setPublishedAt(article.getPublishedDate());
                dto.setSymbols(article.getSymbol() != null ? List.of(article.getSymbol()) : null);
                dto.setCategory("market");
                // Simple sentiment based on keywords
                if (article.getTitle() != null) {
                    String titleLower = article.getTitle().toLowerCase();
                    if (titleLower.contains("surge") || titleLower.contains("gain") || titleLower.contains("rise")
                            || titleLower.contains("rally") || titleLower.contains("bullish")) {
                        dto.setSentiment("positive");
                    } else if (titleLower.contains("drop") || titleLower.contains("fall") || titleLower.contains("decline")
                            || titleLower.contains("crash") || titleLower.contains("bearish")) {
                        dto.setSentiment("negative");
                    } else {
                        dto.setSentiment("neutral");
                    }
                }
                newsItems.add(dto);
                count++;
            }
        } catch (Exception e) {
            log.error("Failed to fetch news insights: {}", e.getMessage());
        }

        return newsItems;
    }

    private ChatContext buildContext(AgentChatRequest request, String userId) {
        ChatContext context = new ChatContext();
        context.setUserId(userId);
        context.setFeature("news-sentinel");

        // Build news context with real FMP data
        String newsContext = buildNewsContext(request);
        context.setSystemPrompt(NewsSentinelPrompts.buildSystemPrompt(newsContext));

        if (request.getAdditionalContext() != null) {
            context.setAdditionalContext(request.getAdditionalContext());
        }

        return context;
    }

    @SuppressWarnings("unchecked")
    private String buildNewsContext(AgentChatRequest request) {
        // Extract parameters from request
        List<String> symbols = null;
        String newsType = null;
        String sector = null;
        String focusSymbol = null;

        if (request.getAdditionalContext() != null) {
            Object symbolsObj = request.getAdditionalContext().get("symbols");
            if (symbolsObj instanceof List<?>) {
                symbols = ((List<?>) symbolsObj).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }

            Object newsTypeObj = request.getAdditionalContext().get("newsType");
            if (newsTypeObj != null) {
                newsType = newsTypeObj.toString();
            }

            Object sectorObj = request.getAdditionalContext().get("sector");
            if (sectorObj != null) {
                sector = sectorObj.toString();
            }

            Object focusObj = request.getAdditionalContext().get("focusSymbol");
            if (focusObj != null) {
                focusSymbol = focusObj.toString();
            }
        }

        // Check if user is asking about a specific symbol
        String extractedSymbol = extractSymbolFromMessage(request.getMessage());
        if (extractedSymbol != null) {
            focusSymbol = extractedSymbol;
        }

        // Build appropriate context based on request type
        if (focusSymbol != null) {
            // User is asking about a specific symbol
            log.debug("Building symbol-focused news context for: {}", focusSymbol);
            return newsContextProvider.buildSymbolNewsContext(focusSymbol);
        } else if (symbols != null && !symbols.isEmpty()) {
            // User has a watchlist
            log.debug("Building watchlist news context for {} symbols", symbols.size());
            return newsContextProvider.buildNewsContext(symbols, newsType, sector);
        } else {
            // General market news
            log.debug("Building general market news context");
            return newsContextProvider.buildMarketNewsContext();
        }
    }

    /**
     * Extract stock symbol from user message
     * Simple extraction - could be enhanced with NLP
     */
    private String extractSymbolFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String upperMessage = message.toUpperCase();

        // Common patterns: "news for AAPL", "AAPL news", "what about TSLA", etc.
        String[] words = upperMessage.split("\\s+");
        for (String word : words) {
            // Remove common punctuation
            word = word.replaceAll("[^A-Z]", "");
            // Check if it looks like a stock symbol (1-5 uppercase letters)
            if (word.length() >= 1 && word.length() <= 5 && word.matches("[A-Z]+")) {
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
            case "A", "I", "THE", "FOR", "AND", "OR", "IS", "IT", "TO", "OF", "IN", "ON", "AT",
                 "NEWS", "SEC", "ANY", "ALL", "NEW", "NOW", "UP", "DOWN", "GO", "BE", "DO",
                 "HAS", "HAD", "WAS", "ARE", "CAN", "MAY", "GET", "PUT", "CALL", "BUY", "SELL" -> true;
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
