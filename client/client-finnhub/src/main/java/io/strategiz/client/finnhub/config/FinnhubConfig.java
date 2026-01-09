package io.strategiz.client.finnhub.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Finnhub API client
 */
@Configuration
public class FinnhubConfig {

    @Value("${finnhub.api.key:}")
    private String apiKey;

    @Value("${finnhub.api.base-url:https://finnhub.io/api/v1}")
    private String baseUrl;

    @Value("${finnhub.api.rate-limit:60}")
    private int rateLimit;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Rate limiter bucket for Finnhub API (60 requests per minute)
     */
    @Bean(name = "finnhubRateLimiter")
    public Bucket finnhubRateLimiter() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimit)
                .refillGreedy(rateLimit, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
