package io.strategiz.client.finnhub.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for Finnhub API responses
 *
 * Cache TTLs (managed via scheduled cleanup):
 * - finnhubMarketNews: 5 minutes (news changes frequently)
 * - finnhubCompanyNews: 5 minutes
 * - finnhubEarningsCalendar: 15 minutes (earnings dates don't change often)
 * - finnhubFilings: 1 hour (filings are historical)
 */
@Configuration
@EnableCaching
public class FinnhubCacheConfig {

    public static final String MARKET_NEWS_CACHE = "finnhubMarketNews";
    public static final String COMPANY_NEWS_CACHE = "finnhubCompanyNews";
    public static final String EARNINGS_CALENDAR_CACHE = "finnhubEarningsCalendar";
    public static final String FILINGS_CACHE = "finnhubFilings";

    @Bean(name = "finnhubCacheManager")
    public CacheManager finnhubCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache(MARKET_NEWS_CACHE),
                new ConcurrentMapCache(COMPANY_NEWS_CACHE),
                new ConcurrentMapCache(EARNINGS_CALENDAR_CACHE),
                new ConcurrentMapCache(FILINGS_CACHE)
        ));
        return cacheManager;
    }
}
