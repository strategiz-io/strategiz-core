package io.strategiz.service.marketing.config;

import io.strategiz.service.marketing.controller.MarketTickerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Warms up the market ticker cache on application startup.
 * This ensures the first user request is fast instead of waiting for external API calls.
 */
@Component
public class MarketTickerCacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(MarketTickerCacheWarmer.class);

    private final MarketTickerController marketTickerController;

    public MarketTickerCacheWarmer(MarketTickerController marketTickerController) {
        this.marketTickerController = marketTickerController;
    }

    /**
     * Warm up the market ticker cache after the application is fully ready.
     * This runs asynchronously and won't block startup if it fails.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("Warming up market ticker cache...");
        try {
            marketTickerController.getMarketTicker();
            log.info("Market ticker cache warmed up successfully");
        } catch (Exception e) {
            log.warn("Failed to warm up market ticker cache: {}", e.getMessage());
            // Don't fail startup - cache will populate on first request
        }
    }
}
