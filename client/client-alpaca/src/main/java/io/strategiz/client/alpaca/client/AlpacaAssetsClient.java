package io.strategiz.client.alpaca.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.alpaca.error.AlpacaErrorDetails;
import io.strategiz.client.alpaca.model.AlpacaAsset;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Alpaca Assets Client - Fetches asset metadata
 *
 * API Documentation: https://docs.alpaca.markets/reference/get-v2-assets-1
 *
 * Features:
 * - Fetch all assets or filter by status/exchange
 * - Get asset metadata (name, exchange, tradability, etc.)
 * - Filter by asset class (us_equity, crypto)
 */
@Component
public class AlpacaAssetsClient {

    private static final Logger log = LoggerFactory.getLogger(AlpacaAssetsClient.class);
    private static final String MODULE_NAME = "AlpacaAssetsClient";

    @Autowired(required = false)
    private VaultSecretService vaultSecretService;

    private String apiUrl;
    private String apiKey;
    private String apiSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlpacaAssetsClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        log.info("AlpacaAssetsClient initializing...");

        // Load credentials from Vault ONLY - no fallback to properties files
        if (vaultSecretService == null) {
            log.error("VaultSecretService not available! Cannot load Alpaca credentials.");
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "VaultSecretService is required but not available");
        }

        try {
            // Assets API uses Trading API endpoint, not Market Data API
            // Must use trading (paper) credentials, not market data credentials
            apiKey = vaultSecretService.readSecret("alpaca.paper.api-key");
            apiSecret = vaultSecretService.readSecret("alpaca.paper.api-secret");

            // Try to load URL from Vault first, but fall back to paper trading URL
            try {
                apiUrl = vaultSecretService.readSecret("alpaca.paper.api-url");
            } catch (Exception ignored) {
                // Use paper trading API as default for Assets endpoint
                apiUrl = "https://paper-api.alpaca.markets";
            }

            log.info("Successfully loaded Alpaca trading credentials from Vault for Assets API");
        } catch (Exception e) {
            log.error("Failed to load Alpaca trading credentials from Vault: {}", e.getMessage());
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, e, "Failed to load required Alpaca trading credentials from Vault");
        }

        // Set default URL if not in Vault
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://paper-api.alpaca.markets";
            log.info("Using default trading API URL: {}", apiUrl);
        }

        log.info("AlpacaAssetsClient initialized");
        log.info("API URL: {}", apiUrl);
        log.info("API Key: {}", apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "NOT SET");

        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            log.error("Alpaca API credentials are not configured!");
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "Alpaca API credentials must be configured in Vault");
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("APCA-API-KEY-ID", apiKey);
        headers.add("APCA-API-SECRET-KEY", apiSecret);
        headers.add("Accept", "application/json");
        return headers;
    }

    /**
     * Get all active assets
     */
    public List<AlpacaAsset> getAllAssets() {
        return getAssets("active", null);
    }

    /**
     * Get all active US equity assets
     */
    public List<AlpacaAsset> getUsEquities() {
        List<AlpacaAsset> allAssets = getAssets("active", "us_equity");
        return allAssets.stream()
                .filter(asset -> asset.getTradable() != null && asset.getTradable())
                .collect(Collectors.toList());
    }

    /**
     * Get assets filtered by NYSE and NASDAQ exchanges
     */
    public List<AlpacaAsset> getNyseNasdaqStocks() {
        List<AlpacaAsset> equities = getUsEquities();
        return equities.stream()
                .filter(asset -> {
                    String exchange = asset.getExchange();
                    return exchange != null &&
                           (exchange.equals("NASDAQ") || exchange.equals("NYSE") || exchange.equals("ARCA"));
                })
                .collect(Collectors.toList());
    }

    /**
     * Get assets by status and asset class
     */
    public List<AlpacaAsset> getAssets(String status, String assetClass) {
        log.debug("Fetching assets with status={}, class={}", status, assetClass);

        StringBuilder path = new StringBuilder("/v2/assets?");
        if (status != null && !status.isEmpty()) {
            path.append("status=").append(status).append("&");
        }
        if (assetClass != null && !assetClass.isEmpty()) {
            path.append("asset_class=").append(assetClass).append("&");
        }

        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                apiUrl + path.toString(),
                HttpMethod.GET,
                entity,
                String.class
            );

            String responseBody = responseEntity.getBody();

            if (responseBody == null || responseBody.isEmpty()) {
                throw new StrategizException(
                    AlpacaErrorDetails.NO_DATA_AVAILABLE,
                    MODULE_NAME,
                    "Empty response from assets API"
                );
            }

            List<AlpacaAsset> assets = objectMapper.readValue(
                responseBody,
                new TypeReference<List<AlpacaAsset>>() {}
            );

            log.info("Fetched {} assets (status={}, class={})", assets.size(), status, assetClass);
            return assets;

        } catch (Exception e) {
            log.error("Error fetching assets: {}", e.getMessage());
            throw new StrategizException(
                AlpacaErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Failed to fetch assets"
            );
        }
    }

    /**
     * Get metadata for a specific symbol
     */
    public AlpacaAsset getAsset(String symbol) {
        log.debug("Fetching asset metadata for {}", symbol);

        String path = String.format("/v2/assets/%s", symbol);

        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                apiUrl + path,
                HttpMethod.GET,
                entity,
                String.class
            );

            String responseBody = responseEntity.getBody();

            if (responseBody == null || responseBody.isEmpty()) {
                throw new StrategizException(
                    AlpacaErrorDetails.NO_DATA_AVAILABLE,
                    MODULE_NAME,
                    String.format("No asset data for symbol: %s", symbol)
                );
            }

            AlpacaAsset asset = objectMapper.readValue(responseBody, AlpacaAsset.class);
            log.debug("Fetched asset metadata for {}: {}", symbol, asset);
            return asset;

        } catch (Exception e) {
            log.error("Error fetching asset for {}: {}", symbol, e.getMessage());
            throw new StrategizException(
                AlpacaErrorDetails.INVALID_SYMBOL,
                MODULE_NAME,
                e,
                String.format("Invalid or unsupported symbol: %s", symbol)
            );
        }
    }
}
