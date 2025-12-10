package io.strategiz.service.marketdata.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for market data service
 * Uses Ehcache with configuration defined in ehcache.xml
 *
 * Cache definitions:
 * - marketDataBars: Historical OHLCV data (5 min TTL)
 * - latestMarketData: Latest price for a symbol (1 min TTL)
 * - availableSymbols: List of available symbols (1 hour TTL)
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Ehcache configuration is loaded from ehcache.xml
    // Spring Boot auto-configures JCache when ehcache is on classpath
}
