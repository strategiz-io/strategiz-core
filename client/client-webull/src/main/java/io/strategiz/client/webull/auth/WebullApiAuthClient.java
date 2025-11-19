package io.strategiz.client.webull.auth;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Webull API Authentication Client
 * Handles API key authentication and request signing for Webull API
 *
 * Webull uses HMAC-SHA1 signature authentication:
 * 1. Construct source parameters from URI, query params, body, and headers
 * 2. Sort parameters alphabetically and concatenate as key-value pairs
 * 3. Generate MD5 hash of request body (if present)
 * 4. Create signature string by concatenating URI, sorted params, and body hash
 * 5. Apply HMAC-SHA1 encryption using app secret with appended "&"
 * 6. Encode result in Base64
 */
@Service
public class WebullApiAuthClient {

    private static final Logger log = LoggerFactory.getLogger(WebullApiAuthClient.class);

    private static final String WEBULL_API_BASE_URL = "https://api.webull.com";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA1";
    private static final String SIGNATURE_VERSION = "1.0";
    private static final DateTimeFormatter ISO8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final WebClient webClient;

    public WebullApiAuthClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(WEBULL_API_BASE_URL).build();
    }

    /**
     * Creates a Webull API signature for authentication using HMAC-SHA1
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path API endpoint path
     * @param queryParams Query parameters
     * @param body Request body (for POST requests)
     * @param timestamp ISO8601 timestamp
     * @param nonce Unique request nonce
     * @param appSecret App secret key
     * @return Base64 encoded signature
     */
    public String createSignature(String method, String path, Map<String, String> queryParams,
                                   String body, String timestamp, String nonce, String appSecret) {
        try {
            if (appSecret == null || appSecret.trim().isEmpty()) {
                log.error("App secret is null or empty");
                throw new IllegalArgumentException("App secret is null or empty");
            }

            log.debug("Creating signature for path: {}, method: {}", path, method);

            // Step 1: Build sorted parameter string
            Map<String, String> allParams = new TreeMap<>();

            // Add signature-related headers as parameters
            allParams.put("x-signature-algorithm", SIGNATURE_ALGORITHM);
            allParams.put("x-signature-version", SIGNATURE_VERSION);
            allParams.put("x-timestamp", timestamp);
            allParams.put("x-signature-nonce", nonce);

            // Add query parameters
            if (queryParams != null) {
                allParams.putAll(queryParams);
            }

            // Build parameter string (sorted alphabetically)
            StringBuilder paramString = new StringBuilder();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (paramString.length() > 0) {
                    paramString.append("&");
                }
                paramString.append(entry.getKey()).append("=").append(entry.getValue());
            }

            // Step 2: Generate MD5 hash of body if present
            String bodyHash = "";
            if (body != null && !body.isEmpty()) {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] bodyBytes = md5.digest(body.getBytes(StandardCharsets.UTF_8));
                bodyHash = bytesToHex(bodyBytes).toLowerCase();
            }

            // Step 3: Create signature base string
            // Format: METHOD + URI + sorted_params + body_hash
            StringBuilder signatureBase = new StringBuilder();
            signatureBase.append(method.toUpperCase());
            signatureBase.append(path);
            if (paramString.length() > 0) {
                signatureBase.append(paramString);
            }
            if (!bodyHash.isEmpty()) {
                signatureBase.append(bodyHash);
            }

            log.debug("Signature base string length: {}", signatureBase.length());

            // Step 4: Calculate HMAC-SHA1
            // Webull requires appending "&" to the secret
            String secretWithSuffix = appSecret + "&";
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretWithSuffix.getBytes(StandardCharsets.UTF_8),
                    SIGNATURE_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacResult = mac.doFinal(signatureBase.toString().getBytes(StandardCharsets.UTF_8));

            // Step 5: Encode in Base64
            String signature = Base64.encodeBase64String(hmacResult);

            log.debug("Successfully created signature");
            return signature;

        } catch (Exception e) {
            log.error("Failed to create signature: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Webull API signature", e);
        }
    }

    /**
     * Get account list for the authenticated user
     *
     * @param appKey App key
     * @param appSecret App secret
     * @return Account list response
     */
    public Mono<Map<String, Object>> getAccountList(String appKey, String appSecret) {
        String path = "/api/trade/account/list";
        String timestamp = ISO8601_FORMATTER.format(Instant.now());
        String nonce = UUID.randomUUID().toString();

        String signature = createSignature("GET", path, null, null, timestamp, nonce, appSecret);

        return webClient.get()
                .uri(path)
                .header("x-app-key", appKey)
                .header("x-signature", signature)
                .header("x-signature-algorithm", SIGNATURE_ALGORITHM)
                .header("x-signature-version", SIGNATURE_VERSION)
                .header("x-signature-nonce", nonce)
                .header("x-timestamp", timestamp)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .doOnSuccess(response -> log.debug("Account list response received"))
                .doOnError(error -> log.error("Failed to get account list: {}", error.getMessage()));
    }

    /**
     * Get account balance/positions for a specific account
     *
     * @param appKey App key
     * @param appSecret App secret
     * @param accountId Account ID
     * @return Account positions response
     */
    public Mono<Map<String, Object>> getAccountPositions(String appKey, String appSecret, String accountId) {
        String path = "/api/trade/account/positions";
        String timestamp = ISO8601_FORMATTER.format(Instant.now());
        String nonce = UUID.randomUUID().toString();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("account_id", accountId);

        String signature = createSignature("GET", path, queryParams, null, timestamp, nonce, appSecret);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("account_id", accountId)
                        .build())
                .header("x-app-key", appKey)
                .header("x-signature", signature)
                .header("x-signature-algorithm", SIGNATURE_ALGORITHM)
                .header("x-signature-version", SIGNATURE_VERSION)
                .header("x-signature-nonce", nonce)
                .header("x-timestamp", timestamp)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .doOnSuccess(response -> log.debug("Account positions response received for account: {}", accountId))
                .doOnError(error -> log.error("Failed to get account positions: {}", error.getMessage()));
    }

    /**
     * Get account balance
     *
     * @param appKey App key
     * @param appSecret App secret
     * @param accountId Account ID
     * @return Account balance response
     */
    public Mono<Map<String, Object>> getAccountBalance(String appKey, String appSecret, String accountId) {
        String path = "/api/trade/account/balance";
        String timestamp = ISO8601_FORMATTER.format(Instant.now());
        String nonce = UUID.randomUUID().toString();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("account_id", accountId);

        String signature = createSignature("GET", path, queryParams, null, timestamp, nonce, appSecret);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("account_id", accountId)
                        .build())
                .header("x-app-key", appKey)
                .header("x-signature", signature)
                .header("x-signature-algorithm", SIGNATURE_ALGORITHM)
                .header("x-signature-version", SIGNATURE_VERSION)
                .header("x-signature-nonce", nonce)
                .header("x-timestamp", timestamp)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .doOnSuccess(response -> log.debug("Account balance response received for account: {}", accountId))
                .doOnError(error -> log.error("Failed to get account balance: {}", error.getMessage()));
    }

    /**
     * Test API connection by getting account list
     *
     * @param appKey App key
     * @param appSecret App secret
     * @return true if connection is successful
     */
    public Mono<Boolean> testConnection(String appKey, String appSecret) {
        return getAccountList(appKey, appSecret)
                .map(response -> {
                    // Check for error in response
                    if (response.containsKey("error") || response.containsKey("code")) {
                        Object code = response.get("code");
                        if (code != null && !"0".equals(code.toString())) {
                            log.warn("Webull API returned error: {}", response);
                            return false;
                        }
                    }
                    return true;
                })
                .onErrorReturn(false);
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
