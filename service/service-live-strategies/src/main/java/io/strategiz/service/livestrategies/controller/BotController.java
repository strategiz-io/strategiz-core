package io.strategiz.service.livestrategies.controller;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyBot;
import io.strategiz.data.strategy.repository.ReadStrategyBotRepository;
import io.strategiz.data.strategy.repository.CreateStrategyBotRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyBotRepository;
import io.strategiz.data.strategy.repository.DeleteStrategyBotRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.livestrategies.exception.LiveStrategiesErrorDetails;
import io.strategiz.service.livestrategies.model.request.CreateBotRequest;
import io.strategiz.service.livestrategies.model.request.UpdateBotStatusRequest;
import io.strategiz.service.livestrategies.model.response.BotResponse;
import io.strategiz.service.livestrategies.model.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing strategy bots (automated trading).
 * Provides CRUD operations for bot deployments.
 *
 * NOTE: Bot deployment is currently marked as "Coming Soon" in the UI.
 * This controller provides the backend infrastructure for when bot trading is enabled.
 */
@RestController
@RequestMapping("/v1/bots")
@Tag(name = "Strategy Bots", description = "Manage automated trading strategy bots")
public class BotController {

    private static final Logger logger = LoggerFactory.getLogger(BotController.class);

    private final ReadStrategyBotRepository readBotRepository;
    private final CreateStrategyBotRepository createBotRepository;
    private final UpdateStrategyBotRepository updateBotRepository;
    private final DeleteStrategyBotRepository deleteBotRepository;
    private final ReadStrategyRepository readStrategyRepository;

    @Autowired
    public BotController(
            ReadStrategyBotRepository readBotRepository,
            CreateStrategyBotRepository createBotRepository,
            UpdateStrategyBotRepository updateBotRepository,
            DeleteStrategyBotRepository deleteBotRepository,
            ReadStrategyRepository readStrategyRepository) {
        this.readBotRepository = readBotRepository;
        this.createBotRepository = createBotRepository;
        this.updateBotRepository = updateBotRepository;
        this.deleteBotRepository = deleteBotRepository;
        this.readStrategyRepository = readStrategyRepository;
    }

    /**
     * GET /v1/bots - List all user's bots
     * Used by Live Strategies screen to display bot cards
     */
    @GetMapping
    @Operation(summary = "Get all bots", description = "Retrieve all bots for the authenticated user")
    public ResponseEntity<List<BotResponse>> getAllBots(Authentication authentication) {
        String userId = authentication.getName();
        logger.info("Fetching all bots for user: {}", userId);

        try {
            List<StrategyBot> bots = readBotRepository.findByUserId(userId);
            List<BotResponse> responses = bots.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch bots for user: {}", userId, e);
            throw new StrategizException(LiveStrategiesErrorDetails.BOT_FETCH_FAILED, "service-live-strategies", e);
        }
    }

    /**
     * GET /v1/bots/{id} - Get a specific bot
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get bot by ID", description = "Retrieve a specific bot by its ID")
    public ResponseEntity<BotResponse> getBotById(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        logger.info("Fetching bot {} for user: {}", id, userId);

        try {
            Optional<StrategyBot> bot = readBotRepository.findById(id);
            if (bot.isEmpty() || !userId.equals(bot.get().getUserId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(convertToResponse(bot.get()));
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch bot: {}", id, e);
            throw new StrategizException(LiveStrategiesErrorDetails.BOT_FETCH_FAILED, "service-live-strategies", e);
        }
    }

    /**
     * POST /v1/bots - Deploy new bot
     * Called from "Deploy Bot" dialog in Labs screen
     *
     * NOTE: Currently disabled in UI (Coming Soon)
     */
    @PostMapping
    @Operation(summary = "Deploy new bot", description = "Create and deploy a new strategy bot")
    public ResponseEntity<MessageResponse> createBot(
            @Valid @RequestBody CreateBotRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        logger.info("Creating bot '{}' for user: {}", request.getBotName(), userId);

        try {
            // Validate strategy exists and belongs to user
            Optional<Strategy> strategy = readStrategyRepository.findById(request.getStrategyId());
            if (strategy.isEmpty() || !userId.equals(strategy.get().getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Strategy not found or access denied"));
            }

            // TODO: Check subscription tier limits (FREE: 1 paper only, STARTER: 3, PRO: unlimited)
            int activeCount = readBotRepository.countActiveByUserId(userId);
            // For now, allow limited - will add tier checking later

            // Validate environment for free tier
            // FREE users can only use PAPER trading
            // if ("FREE".equals(userTier) && "LIVE".equals(request.getEnvironment())) {
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //             .body(new MessageResponse("Live trading requires STARTER or PRO subscription"));
            // }

            // Create bot entity
            StrategyBot bot = new StrategyBot();
            bot.setStrategyId(request.getStrategyId());
            bot.setUserId(userId);
            bot.setBotName(request.getBotName());
            bot.setSymbols(request.getSymbols());
            bot.setProviderId(request.getProviderId());
            bot.setExchange(request.getExchange());
            bot.setEnvironment(request.getEnvironment());
            bot.setMaxPositionSize(request.getMaxPositionSize());
            bot.setStopLossPercent(request.getStopLossPercent());
            bot.setTakeProfitPercent(request.getTakeProfitPercent());
            bot.setMaxDailyLoss(request.getMaxDailyLoss());
            bot.setAutoExecute(request.getAutoExecute());
            bot.setStatus("ACTIVE");
            bot.setSubscriptionTier("FREE"); // TODO: Get from user profile

            // Save bot
            StrategyBot created = createBotRepository.createWithUserId(bot, userId);

            String envLabel = "PAPER".equals(request.getEnvironment()) ? "Paper Trading" : "Live Trading";
            MessageResponse response = new MessageResponse(
                    created.getId(),
                    "Bot deployed to " + envLabel + "! Monitoring " + String.join(", ", request.getSymbols())
            );
            response.setStatus("ACTIVE");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create bot", e);
            throw new StrategizException(LiveStrategiesErrorDetails.BOT_CREATE_FAILED, "service-live-strategies", e);
        }
    }

    /**
     * PATCH /v1/bots/{id}/status - Update bot status
     * Used by pause/resume buttons on bot cards
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update bot status", description = "Pause, resume, or stop a bot")
    public ResponseEntity<MessageResponse> updateBotStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBotStatusRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        logger.info("Updating bot {} status to {} for user: {}", id, request.getStatus(), userId);

        try {
            boolean updated = updateBotRepository.updateStatus(id, userId, request.getStatus());

            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Bot not found or access denied"));
            }

            String message = switch (request.getStatus()) {
                case "PAUSED" -> "Bot paused - no trades will be executed";
                case "ACTIVE" -> "Bot resumed - monitoring for signals";
                case "STOPPED" -> "Bot stopped permanently";
                default -> "Bot status updated";
            };

            MessageResponse response = new MessageResponse(message);
            response.setStatus(request.getStatus());

            return ResponseEntity.ok(response);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update bot status", e);
            throw new StrategizException(LiveStrategiesErrorDetails.BOT_UPDATE_FAILED, "service-live-strategies", e);
        }
    }

    /**
     * DELETE /v1/bots/{id} - Delete bot
     * Called from bot card menu → Delete
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete bot", description = "Stop and permanently delete a bot")
    public ResponseEntity<MessageResponse> deleteBot(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        logger.info("Deleting bot {} for user: {}", id, userId);

        try {
            boolean deleted = deleteBotRepository.delete(id, userId);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Bot not found or access denied"));
            }

            return ResponseEntity.ok(new MessageResponse("Bot deleted successfully"));
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete bot", e);
            throw new StrategizException(LiveStrategiesErrorDetails.BOT_DELETE_FAILED, "service-live-strategies", e);
        }
    }

    /**
     * GET /v1/bots/{id}/performance - Get bot performance metrics
     * Returns detailed trading performance
     */
    @GetMapping("/{id}/performance")
    @Operation(summary = "Get bot performance", description = "Retrieve trading performance metrics for a bot")
    public ResponseEntity<BotPerformanceResponse> getBotPerformance(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        logger.info("Fetching performance for bot {} (user: {})", id, userId);

        try {
            Optional<StrategyBot> bot = readBotRepository.findById(id);
            if (bot.isEmpty() || !userId.equals(bot.get().getUserId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            StrategyBot b = bot.get();
            BotPerformanceResponse response = new BotPerformanceResponse();
            response.setBotId(id);
            response.setTotalTrades(b.getTotalTrades());
            response.setProfitableTrades(b.getProfitableTrades());
            response.setTotalPnL(b.getTotalPnL());
            response.setWinRate(b.getWinRate());
            response.setEnvironment(b.getEnvironment());

            return ResponseEntity.ok(response);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch bot performance", e);
            throw new StrategizException(LiveStrategiesErrorDetails.BOT_PERFORMANCE_FETCH_FAILED, "service-live-strategies", e);
        }
    }

    // Helper methods

    private BotResponse convertToResponse(StrategyBot bot) {
        BotResponse response = new BotResponse();
        response.setId(bot.getId());
        response.setUserId(bot.getUserId());
        response.setStrategyId(bot.getStrategyId());
        response.setBotName(bot.getBotName());
        response.setSymbols(bot.getSymbols());
        response.setProviderId(bot.getProviderId());
        response.setExchange(bot.getExchange());
        response.setEnvironment(bot.getEnvironment());
        response.setMaxPositionSize(bot.getMaxPositionSize());
        response.setStopLossPercent(bot.getStopLossPercent());
        response.setTakeProfitPercent(bot.getTakeProfitPercent());
        response.setMaxDailyLoss(bot.getMaxDailyLoss());
        response.setAutoExecute(bot.getAutoExecute());
        response.setStatus(bot.getStatus());
        response.setTotalTrades(bot.getTotalTrades());
        response.setProfitableTrades(bot.getProfitableTrades());
        response.setTotalPnL(bot.getTotalPnL());
        response.setWinRate(bot.getWinRate());
        response.setLastExecutedAt(bot.getLastExecutedAt());
        response.setDeployedAt(bot.getCreatedDate());
        response.setSubscriptionTier(bot.getSubscriptionTier());
        response.setErrorMessage(bot.getErrorMessage());

        // Fetch strategy name
        try {
            Optional<Strategy> strategy = readStrategyRepository.findById(bot.getStrategyId());
            strategy.ifPresent(s -> response.setStrategyName(s.getName()));
        } catch (Exception e) {
            logger.warn("Failed to fetch strategy name for bot: {}", bot.getId(), e);
        }

        return response;
    }

    /**
     * Inner class for bot performance response
     */
    public static class BotPerformanceResponse {
        private String botId;
        private Integer totalTrades;
        private Integer profitableTrades;
        private Double totalPnL;
        private Double winRate;
        private String environment;

        // Getters and Setters
        public String getBotId() {
            return botId;
        }

        public void setBotId(String botId) {
            this.botId = botId;
        }

        public Integer getTotalTrades() {
            return totalTrades;
        }

        public void setTotalTrades(Integer totalTrades) {
            this.totalTrades = totalTrades;
        }

        public Integer getProfitableTrades() {
            return profitableTrades;
        }

        public void setProfitableTrades(Integer profitableTrades) {
            this.profitableTrades = profitableTrades;
        }

        public Double getTotalPnL() {
            return totalPnL;
        }

        public void setTotalPnL(Double totalPnL) {
            this.totalPnL = totalPnL;
        }

        public Double getWinRate() {
            return winRate;
        }

        public void setWinRate(Double winRate) {
            this.winRate = winRate;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }
    }
}
