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

        // Get index quotes for market overview
        if (fmpQuoteClient != null && fmpQuoteClient.isConfigured()) {
            try {
                List<FmpQuote> indexQuotes = fmpQuoteClient.getIndexQuotes();
                if (!indexQuotes.isEmpty()) {
                    // Sort by performance to find the leader
                    indexQuotes.sort((a, b) -> {
                        if (b.getChangePercent() == null) return -1;
                        if (a.getChangePercent() == null) return 1;
                        return b.getChangePercent().compareTo(a.getChangePercent());
                    });

                    // Top index - market leader
                    FmpQuote topIndex = indexQuotes.get(0);
                    if (topIndex.getChangePercent() != null) {
                        String direction = topIndex.getChangePercent().compareTo(BigDecimal.ZERO) >= 0 ? "bullish" : "bearish";
                        String sign = topIndex.getChangePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                        signals.add(new MarketSignalDto(
                                "momentum",
                                direction,
                                getIndexName(topIndex.getSymbol()),
                                String.format("%s %s%.2f%% ($%.2f)",
                                        topIndex.getSymbol(), sign, topIndex.getChangePercent(), topIndex.getPrice()),
                                "High",
                                topIndex.getChangePercent()
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch index quotes for signals: {}", e.getMessage());
            }
        }

        // Get sector ETF performance for rotation signals
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

                    // Top sector - always show
                    FmpQuote topSector = sectorQuotes.get(0);
                    if (topSector.getChangePercent() != null) {
                        String direction = topSector.getChangePercent().compareTo(BigDecimal.ZERO) >= 0 ? "bullish" : "bearish";
                        String sign = topSector.getChangePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                        signals.add(new MarketSignalDto(
                                "momentum",
                                direction,
                                getSectorName(topSector.getSymbol()),
                                String.format("Leading sector %s%.2f%%", sign, topSector.getChangePercent()),
                                "Medium",
                                topSector.getChangePercent()
                        ));
                    }

                    // Bottom sector - always show for rotation insight
                    FmpQuote bottomSector = sectorQuotes.get(sectorQuotes.size() - 1);
                    if (bottomSector.getChangePercent() != null && !bottomSector.getSymbol().equals(topSector.getSymbol())) {
                        String direction = bottomSector.getChangePercent().compareTo(BigDecimal.ZERO) >= 0 ? "bullish" : "bearish";
                        String sign = bottomSector.getChangePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                        signals.add(new MarketSignalDto(
                                "oversold",
                                direction,
                                getSectorName(bottomSector.getSymbol()),
                                String.format("Lagging sector %s%.2f%%", sign, bottomSector.getChangePercent()),
                                "Low",
                                bottomSector.getChangePercent()
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch sector quotes for signals: {}", e.getMessage());
            }
        }

        // Get RSI signals for major index - always show SPY RSI
        if (fmpTechnicalClient != null && fmpTechnicalClient.isConfigured()) {
            try {
                FmpTechnicalIndicator rsi = fmpTechnicalClient.getRSI("SPY");
                if (rsi != null && rsi.getRsi() != null) {
                    double rsiValue = rsi.getRsi().doubleValue();
                    String type, direction, detail, confidence;

                    if (rsiValue >= 70) {
                        type = "overbought";
                        direction = "bearish";
                        detail = String.format("S&P 500 RSI at %.1f - overbought", rsiValue);
                        confidence = "High";
                    } else if (rsiValue <= 30) {
                        type = "oversold";
                        direction = "bullish";
                        detail = String.format("S&P 500 RSI at %.1f - oversold bounce likely", rsiValue);
                        confidence = "High";
                    } else if (rsiValue >= 60) {
                        type = "momentum";
                        direction = "bullish";
                        detail = String.format("S&P 500 RSI at %.1f - strong momentum", rsiValue);
                        confidence = "Medium";
                    } else if (rsiValue <= 40) {
                        type = "momentum";
                        direction = "bearish";
                        detail = String.format("S&P 500 RSI at %.1f - weakening momentum", rsiValue);
                        confidence = "Medium";
                    } else {
                        type = "momentum";
                        direction = "neutral";
                        detail = String.format("S&P 500 RSI at %.1f - neutral range", rsiValue);
                        confidence = "Low";
                    }

                    signals.add(new MarketSignalDto(type, direction, "SPY", detail, confidence, rsi.getRsi()));
                }
            } catch (Exception e) {
                log.debug("Failed to get RSI for SPY: {}", e.getMessage());
            }
        }

        // Add VIX sentiment - always include for volatility context
        if (fmpQuoteClient != null && fmpQuoteClient.isConfigured()) {
            try {
                FmpQuote vix = fmpQuoteClient.getVixQuote();
                if (vix != null && vix.getPrice() != null) {
                    double vixValue = vix.getPrice().doubleValue();
                    String sentiment, direction, confidence;
                    if (vixValue < 15) {
                        sentiment = "Low volatility - complacency";
                        direction = "bullish";
                        confidence = "Medium";
                    } else if (vixValue < 20) {
                        sentiment = "Normal volatility";
                        direction = "neutral";
                        confidence = "Low";
                    } else if (vixValue < 25) {
                        sentiment = "Elevated volatility";
                        direction = "bearish";
                        confidence = "Medium";
                    } else if (vixValue < 30) {
                        sentiment = "High volatility - caution";
                        direction = "bearish";
                        confidence = "High";
                    } else {
                        sentiment = "Extreme fear - capitulation";
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

    private String getIndexName(String symbol) {
        return switch (symbol) {
            case "SPY" -> "S&P 500";
            case "QQQ" -> "NASDAQ 100";
            case "IWM" -> "Russell 2000";
            case "DIA" -> "Dow Jones";
            default -> symbol;
        };
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
