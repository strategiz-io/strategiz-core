package io.strategiz.service.agents.service;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.aichat.model.ChatContext;
import io.strategiz.business.aichat.model.ChatMessage;
import io.strategiz.business.aichat.model.ChatResponse;
import io.strategiz.client.fmp.client.FmpQuoteClient;
import io.strategiz.client.fmp.client.FmpTechnicalClient;
import io.strategiz.client.fmp.dto.FmpQuote;
import io.strategiz.client.fmp.dto.FmpTechnicalIndicator;
import io.strategiz.service.agents.context.MarketSignalsContextProvider;
import io.strategiz.service.agents.dto.AgentChatMessage;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.dto.MarketSignalDto;
import io.strategiz.service.agents.prompts.SignalScoutPrompts;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private static final List<String> INDEX_SYMBOLS = List.of("SPY", "QQQ", "IWM", "DIA");

    private final AIChatBusiness aiChatBusiness;
    private final MarketSignalsContextProvider marketSignalsContextProvider;
    private final FmpQuoteClient fmpQuoteClient;
    private final FmpTechnicalClient fmpTechnicalClient;

    public SignalScoutService(AIChatBusiness aiChatBusiness, MarketSignalsContextProvider marketSignalsContextProvider,
            FmpQuoteClient fmpQuoteClient, FmpTechnicalClient fmpTechnicalClient) {
        this.aiChatBusiness = aiChatBusiness;
        this.marketSignalsContextProvider = marketSignalsContextProvider;
        this.fmpQuoteClient = fmpQuoteClient;
        this.fmpTechnicalClient = fmpTechnicalClient;
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

    /**
     * Get market signals for the insights panel.
     * @param limit Maximum number of signals to return
     * @return List of market signals
     */
    public List<MarketSignalDto> getMarketSignalsInsights(int limit) {
        log.debug("Fetching market signals insights, limit: {}", limit);
        List<MarketSignalDto> signals = new ArrayList<>();

        // Get sector ETF performance for momentum signals
        if (fmpQuoteClient != null && fmpQuoteClient.isConfigured()) {
            try {
                List<FmpQuote> sectorQuotes = fmpQuoteClient.getSectorETFQuotes();
                if (!sectorQuotes.isEmpty()) {
                    // Sort by performance
                    sectorQuotes.sort((a, b) -> {
                        if (b.getChangePercent() == null) return -1;
                        if (a.getChangePercent() == null) return 1;
                        return b.getChangePercent().compareTo(a.getChangePercent());
                    });

                    // Top performer (momentum)
                    FmpQuote topSector = sectorQuotes.get(0);
                    if (topSector.getChangePercent() != null && topSector.getChangePercent().compareTo(BigDecimal.ZERO) > 0) {
                        signals.add(new MarketSignalDto(
                                "momentum",
                                "bullish",
                                getSectorName(topSector.getSymbol()) + " Sector",
                                String.format("%s leading with %+.2f%% today",
                                        topSector.getSymbol(), topSector.getChangePercent()),
                                "High",
                                topSector.getChangePercent()
                        ));
                    }

                    // Bottom performer (potential oversold)
                    FmpQuote bottomSector = sectorQuotes.get(sectorQuotes.size() - 1);
                    if (bottomSector.getChangePercent() != null && bottomSector.getChangePercent().compareTo(new BigDecimal("-1")) < 0) {
                        signals.add(new MarketSignalDto(
                                "oversold",
                                "bearish",
                                getSectorName(bottomSector.getSymbol()) + " Sector",
                                String.format("%s lagging with %.2f%% - potential oversold",
                                        bottomSector.getSymbol(), bottomSector.getChangePercent()),
                                "Medium",
                                bottomSector.getChangePercent()
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch sector quotes for signals: {}", e.getMessage());
            }
        }

        // Get RSI signals for indices
        if (fmpTechnicalClient != null && fmpTechnicalClient.isConfigured()) {
            for (String symbol : INDEX_SYMBOLS) {
                try {
                    FmpTechnicalIndicator rsi = fmpTechnicalClient.getRSI(symbol);
                    if (rsi != null && rsi.getRsi() != null) {
                        double rsiValue = rsi.getRsi().doubleValue();
                        if (rsiValue >= 70) {
                            signals.add(new MarketSignalDto(
                                    "overbought",
                                    "bearish",
                                    symbol,
                                    String.format("RSI at %.1f - overbought territory", rsiValue),
                                    "Medium",
                                    rsi.getRsi()
                            ));
                        } else if (rsiValue <= 30) {
                            signals.add(new MarketSignalDto(
                                    "oversold",
                                    "bullish",
                                    symbol,
                                    String.format("RSI at %.1f - oversold, potential bounce", rsiValue),
                                    "High",
                                    rsi.getRsi()
                            ));
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to get RSI for {}: {}", symbol, e.getMessage());
                }

                if (signals.size() >= limit) break;
            }
        }

        // Add VIX sentiment if available
        if (fmpQuoteClient != null && fmpQuoteClient.isConfigured()) {
            try {
                FmpQuote vix = fmpQuoteClient.getVixQuote();
                if (vix != null && vix.getPrice() != null) {
                    double vixValue = vix.getPrice().doubleValue();
                    String sentiment, direction, confidence;
                    if (vixValue < 15) {
                        sentiment = "Low volatility - complacency in market";
                        direction = "neutral";
                        confidence = "Medium";
                    } else if (vixValue < 20) {
                        sentiment = "Normal volatility levels";
                        direction = "neutral";
                        confidence = "Low";
                    } else if (vixValue < 30) {
                        sentiment = "Elevated volatility - caution advised";
                        direction = "bearish";
                        confidence = "Medium";
                    } else {
                        sentiment = "High fear - potential capitulation";
                        direction = "bearish";
                        confidence = "High";
                    }
                    signals.add(new MarketSignalDto(
                            "sentiment",
                            direction,
                            "VIX",
                            String.format("VIX at %.2f - %s", vixValue, sentiment),
                            confidence,
                            vix.getPrice()
                    ));
                }
            } catch (Exception e) {
                log.debug("Failed to get VIX quote: {}", e.getMessage());
            }
        }

        return signals.stream().limit(limit).collect(Collectors.toList());
    }

    private String getSectorName(String symbol) {
        return switch (symbol) {
            case "XLK" -> "Technology";
            case "XLF" -> "Financials";
            case "XLE" -> "Energy";
            case "XLV" -> "Healthcare";
            case "XLI" -> "Industrials";
            case "XLY" -> "Consumer Disc.";
            case "XLP" -> "Consumer Staples";
            case "XLRE" -> "Real Estate";
            case "XLU" -> "Utilities";
            case "XLB" -> "Materials";
            case "XLC" -> "Communication";
            default -> symbol;
        };
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
