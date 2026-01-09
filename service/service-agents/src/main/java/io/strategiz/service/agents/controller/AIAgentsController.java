package io.strategiz.service.agents.controller;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.agents.dto.AgentChatRequest;
import io.strategiz.service.agents.dto.AgentChatResponse;
import io.strategiz.service.agents.service.EarningsEdgeService;
import io.strategiz.service.agents.service.NewsSentinelService;
import io.strategiz.service.agents.service.PortfolioInsightsService;
import io.strategiz.service.agents.service.SignalScoutService;
import io.strategiz.service.agents.service.StrategyOptimizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for AI Agents (Signal Scout, Strategy Optimizer, Earnings Edge, News Sentinel)
 */
@RestController
@RequestMapping("/v1/agents")
@Tag(name = "AI Agents", description = "AI-powered trading agents for signals, strategy optimization, earnings analysis, and news")
public class AIAgentsController {

    private static final Logger logger = LoggerFactory.getLogger(AIAgentsController.class);

    private final SignalScoutService signalScoutService;
    private final StrategyOptimizerService strategyOptimizerService;
    private final EarningsEdgeService earningsEdgeService;
    private final NewsSentinelService newsSentinelService;
    private final PortfolioInsightsService portfolioInsightsService;
    private final SubscriptionService subscriptionService;
    private final FeatureFlagService featureFlagService;

    public AIAgentsController(
            SignalScoutService signalScoutService,
            StrategyOptimizerService strategyOptimizerService,
            EarningsEdgeService earningsEdgeService,
            NewsSentinelService newsSentinelService,
            PortfolioInsightsService portfolioInsightsService,
            SubscriptionService subscriptionService,
            FeatureFlagService featureFlagService) {
        this.signalScoutService = signalScoutService;
        this.strategyOptimizerService = strategyOptimizerService;
        this.earningsEdgeService = earningsEdgeService;
        this.newsSentinelService = newsSentinelService;
        this.portfolioInsightsService = portfolioInsightsService;
        this.subscriptionService = subscriptionService;
        this.featureFlagService = featureFlagService;
    }

    // ==================== Signal Scout ====================

    @PostMapping("/signal-scout/chat")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Signal Scout chat", description = "Chat with Signal Scout for market signals and trading opportunities")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentChatResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Mono<ResponseEntity<AgentChatResponse>> signalScoutChat(
            @Valid @RequestBody AgentChatRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Signal Scout chat request from user: {}", userId);

        if (!checkRateLimits(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AgentChatResponse.error("signalScout", "Daily message limit exceeded.")));
        }

        return signalScoutService.chat(request, userId)
            .doOnSuccess(response -> recordUsage(userId, response))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AgentChatResponse.error("signalScout", "Failed to generate response")));
    }

    @GetMapping(value = "/signal-scout/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Signal Scout streaming chat", description = "Streaming chat with Signal Scout")
    public Flux<AgentChatResponse> signalScoutChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String model,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Signal Scout streaming chat from user: {}, model: {}", userId, model);

        if (!checkRateLimits(userId)) {
            return Flux.just(AgentChatResponse.error("signalScout", "Daily message limit exceeded."));
        }

        subscriptionService.recordMessageUsage(userId);

        AgentChatRequest request = new AgentChatRequest();
        request.setMessage(message);
        request.setModel(model);
        request.setAgentId("signalScout");

        return signalScoutService.chatStream(request, userId);
    }

    // ==================== Strategy Optimizer ====================

    @PostMapping("/strategy-optimizer/chat")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Strategy Optimizer chat", description = "Chat with Strategy Optimizer for trading strategy improvements")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentChatResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Mono<ResponseEntity<AgentChatResponse>> strategyOptimizerChat(
            @Valid @RequestBody AgentChatRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Strategy Optimizer chat request from user: {}", userId);

        if (!checkRateLimits(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AgentChatResponse.error("strategyOptimizer", "Daily message limit exceeded.")));
        }

        return strategyOptimizerService.chat(request, userId)
            .doOnSuccess(response -> recordUsage(userId, response))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AgentChatResponse.error("strategyOptimizer", "Failed to generate response")));
    }

    @GetMapping(value = "/strategy-optimizer/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Strategy Optimizer streaming chat", description = "Streaming chat with Strategy Optimizer")
    public Flux<AgentChatResponse> strategyOptimizerChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String model,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Strategy Optimizer streaming chat from user: {}, model: {}", userId, model);

        if (!checkRateLimits(userId)) {
            return Flux.just(AgentChatResponse.error("strategyOptimizer", "Daily message limit exceeded."));
        }

        subscriptionService.recordMessageUsage(userId);

        AgentChatRequest request = new AgentChatRequest();
        request.setMessage(message);
        request.setModel(model);
        request.setAgentId("strategyOptimizer");

        return strategyOptimizerService.chatStream(request, userId);
    }

    // ==================== Earnings Edge ====================

    @PostMapping("/earnings-edge/chat")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Earnings Edge chat", description = "Chat with Earnings Edge for earnings analysis and trading ideas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentChatResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Mono<ResponseEntity<AgentChatResponse>> earningsEdgeChat(
            @Valid @RequestBody AgentChatRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Earnings Edge chat request from user: {}", userId);

        if (!checkRateLimits(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AgentChatResponse.error("earningsEdge", "Daily message limit exceeded.")));
        }

        return earningsEdgeService.chat(request, userId)
            .doOnSuccess(response -> recordUsage(userId, response))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AgentChatResponse.error("earningsEdge", "Failed to generate response")));
    }

    @GetMapping(value = "/earnings-edge/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Earnings Edge streaming chat", description = "Streaming chat with Earnings Edge")
    public Flux<AgentChatResponse> earningsEdgeChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String model,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Earnings Edge streaming chat from user: {}, model: {}", userId, model);

        if (!checkRateLimits(userId)) {
            return Flux.just(AgentChatResponse.error("earningsEdge", "Daily message limit exceeded."));
        }

        subscriptionService.recordMessageUsage(userId);

        AgentChatRequest request = new AgentChatRequest();
        request.setMessage(message);
        request.setModel(model);
        request.setAgentId("earningsEdge");

        return earningsEdgeService.chatStream(request, userId);
    }

    // ==================== News Sentinel ====================

    @PostMapping("/news-sentinel/chat")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "News Sentinel chat", description = "Chat with News Sentinel for market news and sentiment analysis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentChatResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Mono<ResponseEntity<AgentChatResponse>> newsSentinelChat(
            @Valid @RequestBody AgentChatRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("News Sentinel chat request from user: {}", userId);

        if (!checkRateLimits(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AgentChatResponse.error("newsSentinel", "Daily message limit exceeded.")));
        }

        return newsSentinelService.chat(request, userId)
            .doOnSuccess(response -> recordUsage(userId, response))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AgentChatResponse.error("newsSentinel", "Failed to generate response")));
    }

    @GetMapping(value = "/news-sentinel/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireAuth(minAcr = "1")
    @Operation(summary = "News Sentinel streaming chat", description = "Streaming chat with News Sentinel")
    public Flux<AgentChatResponse> newsSentinelChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String model,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("News Sentinel streaming chat from user: {}, model: {}", userId, model);

        if (!checkRateLimits(userId)) {
            return Flux.just(AgentChatResponse.error("newsSentinel", "Daily message limit exceeded."));
        }

        subscriptionService.recordMessageUsage(userId);

        AgentChatRequest request = new AgentChatRequest();
        request.setMessage(message);
        request.setModel(model);
        request.setAgentId("newsSentinel");

        return newsSentinelService.chatStream(request, userId);
    }

    // ==================== Portfolio Insights ====================

    @PostMapping("/portfolio-insights/chat")
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Portfolio Insights chat", description = "Chat with Portfolio Agent for portfolio analysis and recommendations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentChatResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Mono<ResponseEntity<AgentChatResponse>> portfolioInsightsChat(
            @Valid @RequestBody AgentChatRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Portfolio Insights chat request from user: {}", userId);

        if (!checkRateLimits(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AgentChatResponse.error("portfolioInsights", "Daily message limit exceeded.")));
        }

        return portfolioInsightsService.chat(request, userId)
            .doOnSuccess(response -> recordUsage(userId, response))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AgentChatResponse.error("portfolioInsights", "Failed to generate response")));
    }

    @GetMapping(value = "/portfolio-insights/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireAuth(minAcr = "1")
    @Operation(summary = "Portfolio Insights streaming chat", description = "Streaming chat with Portfolio Agent")
    public Flux<AgentChatResponse> portfolioInsightsChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String model,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        logger.info("Portfolio Insights streaming chat from user: {}, model: {}", userId, model);

        if (!checkRateLimits(userId)) {
            return Flux.just(AgentChatResponse.error("portfolioInsights", "Daily message limit exceeded."));
        }

        subscriptionService.recordMessageUsage(userId);

        AgentChatRequest request = new AgentChatRequest();
        request.setMessage(message);
        request.setModel(model);
        request.setAgentId("portfolioInsights");

        return portfolioInsightsService.chatStream(request, userId);
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if AI Agents service is available")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Agents service is healthy");
    }

    // ==================== Helper Methods ====================

    private boolean checkRateLimits(String userId) {
        return subscriptionService.canSendMessage(userId);
    }

    private void recordUsage(String userId, AgentChatResponse response) {
        if (response != null && response.getContent() != null) {
            subscriptionService.recordMessageUsage(userId);
            logger.debug("Recorded message usage for user {}", userId);
        }
    }

}
