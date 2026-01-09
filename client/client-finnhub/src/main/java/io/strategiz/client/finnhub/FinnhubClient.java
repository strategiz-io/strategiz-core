package io.strategiz.client.finnhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.finnhub.config.FinnhubConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Base client for Finnhub API with rate limiting and error handling
 */
@Component
public class FinnhubClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubClient.class);
    private static final int RATE_LIMIT_WAIT_SECONDS = 5;

    private final FinnhubConfig config;
    private final Bucket rateLimiter;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public FinnhubClient(
            FinnhubConfig config,
            @Qualifier("finnhubRateLimiter") Bucket rateLimiter,
            ObjectMapper objectMapper) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Execute a GET request to Finnhub API
     * @param endpoint API endpoint (e.g., "/company-news")
     * @param params Query parameters
     * @param responseType Response class type
     * @return Optional containing the response, or empty if request failed
     */
    protected <T> Optional<T> get(String endpoint, Map<String, String> params, Class<T> responseType) {
        if (!config.isConfigured()) {
            log.warn("Finnhub API key not configured. Skipping request to {}", endpoint);
            return Optional.empty();
        }

        // Wait for rate limiter
        try {
            if (!rateLimiter.tryConsume(1)) {
                log.warn("Rate limit reached for Finnhub API. Waiting...");
                boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(RATE_LIMIT_WAIT_SECONDS));
                if (!acquired) {
                    log.error("Failed to acquire rate limit token after waiting");
                    return Optional.empty();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for rate limiter", e);
            return Optional.empty();
        }

        String url = buildUrl(endpoint, params);
        HttpGet request = new HttpGet(url);
        request.addHeader("X-Finnhub-Token", config.getApiKey());

        try {
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String body = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    try {
                        T result = objectMapper.readValue(body, responseType);
                        return Optional.of(result);
                    } catch (Exception e) {
                        log.error("Failed to parse Finnhub response: {}", body, e);
                        return Optional.empty();
                    }
                } else if (statusCode == 429) {
                    log.warn("Finnhub API rate limited (429). Response: {}", body);
                    return Optional.empty();
                } else {
                    log.error("Finnhub API error. Status: {}, Response: {}", statusCode, body);
                    return Optional.empty();
                }
            });
        } catch (IOException e) {
            log.error("Failed to execute Finnhub API request to {}", endpoint, e);
            return Optional.empty();
        }
    }

    /**
     * Build URL with query parameters
     */
    private String buildUrl(String endpoint, Map<String, String> params) {
        StringBuilder url = new StringBuilder(config.getBaseUrl());
        url.append(endpoint);

        if (params != null && !params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                url.append("=");
                url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }

        return url.toString();
    }

    /**
     * Check if the client is properly configured
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }
}
