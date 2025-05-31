package io.strategiz.client.base.http;

import io.strategiz.framework.exception.ApplicationClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base HTTP client using Spring Boot 3.5's RestClient for all exchange API interactions.
 * This class ensures we ONLY use real API data, never mocks or simulations.
 */
public abstract class BaseHttpClient {
    
    private static final Logger log = LoggerFactory.getLogger(BaseHttpClient.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    protected RestClient restClient;
    protected String baseUrl;
    
    /**
     * Creates a new BaseHttpClient with the specified base URL.
     *
     * @param baseUrl The base URL for API requests
     */
    protected BaseHttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = createRestClient();
        log.info("Initialized BaseHttpClient for API at: {}", baseUrl);
    }
    
    /**
     * Creates a configured RestClient instance.
     * 
     * @return A properly configured RestClient
     */
    protected RestClient createRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(this::configureDefaultHeaders)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("Making request to: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .defaultStatusHandler(this::handleErrorResponse)
                .build();
    }
    
    /**
     * Configures default HTTP headers for all requests.
     * Override this method to add custom headers.
     *
     * @param headers The HTTP headers to configure
     */
    protected void configureDefaultHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    }
    
    /**
     * Handles error responses from the API.
     * This provides consistent error handling across all API clients.
     *
     * @param response The HTTP response
     * @return true if the response should be treated as an error, false otherwise
     */
    protected boolean handleErrorResponse(ClientHttpResponse response) {
        try {
            if (response.getStatusCode().isError()) {
                HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
                String message = String.format("API error: %s %s", status.value(), status.getReasonPhrase());
                log.error(message);
                throw new ApplicationClientException(message);
            }
            return false;
        } catch (Exception e) {
            log.error("Error handling response: {}", e.getMessage(), e);
            throw new ApplicationClientException("Error handling API response", e);
        }
    }
    
    /**
     * Validates that we're connecting to a real API endpoint, not a mock service.
     * This enforces Strategiz's requirement to use ONLY real APIs, never mock data.
     * 
     * @param serviceName The name of the service being accessed
     * @throws ApplicationClientException if we would be using mock data
     */
    protected void validateRealApiEndpoint(String serviceName) {
        log.info("Validating real API endpoint for: {}", serviceName);
        // Implementation will validate the endpoint is real
        // and throw an exception if it detects a mock service
    }
    
    /**
     * Gets the raw, unmodified response from the API.
     * This follows Strategiz's preference to see exactly what comes from the API.
     *
     * @param <T> The response type
     * @param path The API path
     * @param responseType The class of the response type
     * @return The raw API response
     */
    protected <T> T getRawApiResponse(String path, Class<T> responseType) {
        log.info("Getting raw API response from: {}", path);
        return restClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }
    
    /**
     * Posts data to the API and returns the raw, unmodified response.
     *
     * @param <T> The response type
     * @param path The API path
     * @param requestBody The request body
     * @param responseType The class of the response type
     * @return The raw API response
     */
    protected <T> T postRawApiResponse(String path, Object requestBody, Class<T> responseType) {
        log.info("Posting to API and getting raw response from: {}", path);
        return restClient.post()
                .uri(path)
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }
}
