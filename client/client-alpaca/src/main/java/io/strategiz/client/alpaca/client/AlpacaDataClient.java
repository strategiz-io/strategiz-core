package io.strategiz.client.alpaca.client;

import io.strategiz.client.alpaca.error.AlpacaErrors;
import io.strategiz.framework.exception.StrategizException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching data from Alpaca using OAuth access tokens.
 * This class handles all account and position data retrieval operations.
 */
@Component
public class AlpacaDataClient {

    private static final Logger log = LoggerFactory.getLogger(AlpacaDataClient.class);

    private final RestTemplate restTemplate;

    @Value("${oauth.providers.alpaca.api-url:https://api.alpaca.markets}")
    private String apiUrl;

    public AlpacaDataClient(@Qualifier("alpacaRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("AlpacaDataClient initialized");
    }

    /**
     * Get current account information
     * @param accessToken OAuth access token
     * @return Account information
     */
    public Map<String, Object> getAccount(String accessToken) {
        return makeAuthenticatedRequest(HttpMethod.GET, "/v2/account", accessToken, null);
    }

    /**
     * Get all open positions
     * @param accessToken OAuth access token
     * @return List of positions
     */
    public List<Map<String, Object>> getPositions(String accessToken) {
        Object response = makeAuthenticatedRequest(HttpMethod.GET, "/v2/positions", accessToken, null);
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return Collections.emptyList();
    }

    /**
     * Get specific position by symbol
     * @param accessToken OAuth access token
     * @param symbol Stock symbol
     * @return Position details
     */
    public Map<String, Object> getPosition(String accessToken, String symbol) {
        return makeAuthenticatedRequest(HttpMethod.GET, "/v2/positions/" + symbol, accessToken, null);
    }

    /**
     * Get portfolio history
     * @param accessToken OAuth access token
     * @param period Time period (e.g., "1D", "1W", "1M")
     * @param timeframe Resolution (e.g., "1Min", "5Min", "1H", "1D")
     * @return Portfolio history data
     */
    public Map<String, Object> getPortfolioHistory(String accessToken, String period, String timeframe) {
        Map<String, String> params = Map.of(
            "period", period != null ? period : "1M",
            "timeframe", timeframe != null ? timeframe : "1D"
        );
        return makeAuthenticatedRequest(HttpMethod.GET, "/v2/account/portfolio/history", accessToken, params);
    }

    /**
     * Get account activities (trades, fills, etc.)
     * @param accessToken OAuth access token
     * @param activityType Type of activity (e.g., "FILL", "TRANS", "DIV")
     * @return List of activities
     */
    public List<Map<String, Object>> getAccountActivities(String accessToken, String activityType) {
        String endpoint = "/v2/account/activities";
        if (activityType != null && !activityType.isEmpty()) {
            endpoint += "/" + activityType;
        }

        Object response = makeAuthenticatedRequest(HttpMethod.GET, endpoint, accessToken, null);
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return Collections.emptyList();
    }

    /**
     * Get all orders
     * @param accessToken OAuth access token
     * @param status Order status (e.g., "open", "closed", "all")
     * @param limit Maximum number of orders to return
     * @return List of orders
     */
    public List<Map<String, Object>> getOrders(String accessToken, String status, Integer limit) {
        Map<String, String> params = new java.util.HashMap<>();
        if (status != null) {
            params.put("status", status);
        }
        if (limit != null) {
            params.put("limit", limit.toString());
        }

        Object response = makeAuthenticatedRequest(HttpMethod.GET, "/v2/orders", accessToken,
            params.isEmpty() ? null : params);
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return Collections.emptyList();
    }

    /**
     * Get assets (tradable securities)
     * @param accessToken OAuth access token
     * @param status Asset status (e.g., "active")
     * @return List of assets
     */
    public List<Map<String, Object>> getAssets(String accessToken, String status) {
        Map<String, String> params = null;
        if (status != null) {
            params = Map.of("status", status);
        }

        Object response = makeAuthenticatedRequest(HttpMethod.GET, "/v2/assets", accessToken, params);
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return Collections.emptyList();
    }

    /**
     * Get specific asset by symbol
     * @param accessToken OAuth access token
     * @param symbol Asset symbol
     * @return Asset details
     */
    public Map<String, Object> getAsset(String accessToken, String symbol) {
        return makeAuthenticatedRequest(HttpMethod.GET, "/v2/assets/" + symbol, accessToken, null);
    }

    /**
     * Get clock information (market status)
     * @param accessToken OAuth access token
     * @return Clock data (is_open, next_open, next_close, etc.)
     */
    public Map<String, Object> getClock(String accessToken) {
        return makeAuthenticatedRequest(HttpMethod.GET, "/v2/clock", accessToken, null);
    }

    /**
     * Get calendar (trading days)
     * @param accessToken OAuth access token
     * @param start Start date (YYYY-MM-DD)
     * @param end End date (YYYY-MM-DD)
     * @return List of trading days
     */
    public List<Map<String, Object>> getCalendar(String accessToken, String start, String end) {
        Map<String, String> params = new java.util.HashMap<>();
        if (start != null) {
            params.put("start", start);
        }
        if (end != null) {
            params.put("end", end);
        }

        Object response = makeAuthenticatedRequest(HttpMethod.GET, "/v2/calendar", accessToken,
            params.isEmpty() ? null : params);
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return Collections.emptyList();
    }

    /**
     * Make an authenticated request to Alpaca API
     */
    private <T> T makeAuthenticatedRequest(HttpMethod method, String endpoint,
                                          String accessToken, Map<String, String> params) {
        try {
            // Validate access token
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new StrategizException(AlpacaErrors.ALPACA_AUTH_FAILED,
                    "Access token is required");
            }

            // Build the URL
            URIBuilder uriBuilder = new URIBuilder(apiUrl + endpoint);
            if (params != null) {
                params.forEach(uriBuilder::addParameter);
            }

            URI uri = uriBuilder.build();

            // Create headers with OAuth Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            log.debug("Making authenticated request to Alpaca API: {} {}", method, uri);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                uri,
                method,
                entity,
                Object.class
            );

            if (response.getBody() == null) {
                throw new StrategizException(AlpacaErrors.ALPACA_INVALID_RESPONSE,
                    "Empty response from Alpaca API");
            }

            return (T) response.getBody();

        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();

            log.error("Alpaca API request error - HTTP Status {}: {}", statusCode, responseBody);

            // Handle token expiration
            if (statusCode == 401) {
                throw new StrategizException(AlpacaErrors.ALPACA_TOKEN_EXPIRED,
                    "Access token expired or invalid. Please reconnect your Alpaca account.");
            }

            // Handle rate limiting
            if (statusCode == 429) {
                throw new StrategizException(AlpacaErrors.ALPACA_RATE_LIMIT,
                    "Alpaca API rate limit exceeded. Please try again later.");
            }

            throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR,
                "Alpaca API error: " + responseBody);

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error making authenticated request to {}: {}", endpoint, e.getMessage());
            throw new StrategizException(AlpacaErrors.ALPACA_NETWORK_ERROR,
                "Failed to communicate with Alpaca: " + e.getMessage());
        }
    }
}
