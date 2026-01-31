package io.strategiz.client.base.http;

import io.strategiz.client.base.exception.ClientErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Specialized HTTP client for financial service providers (exchanges, brokerages, etc.).
 * Provides common authentication and request signing methods. Ensures we only use real
 * API data, never mocks or simulations.
 */
public abstract class ProviderClient extends BaseHttpClient {

	private static final Logger log = LoggerFactory.getLogger(ProviderClient.class);

	protected String apiKey;

	protected String privateKey;

	/**
	 * Creates a new ProviderClient with the specified base URL and credentials.
	 * @param baseUrl The base URL for API requests
	 * @param apiKey The API key for authentication
	 * @param privateKey The private key (secret) for request signing
	 */
	protected ProviderClient(String baseUrl, String apiKey, String privateKey) {
		super(baseUrl);
		this.apiKey = apiKey;
		this.privateKey = privateKey;
		log.info("Initialized ProviderClient for provider at: {}", baseUrl);
	}

	/**
	 * Adds authentication headers for provider API requests. Override this in specific
	 * provider clients to match their auth requirements.
	 * @param headers The HTTP headers to configure
	 */
	@Override
	protected void configureDefaultHeaders(HttpHeaders headers) {
		super.configureDefaultHeaders(headers);
		if (apiKey != null && !apiKey.isEmpty()) {
			headers.set("X-API-Key", apiKey);
			log.debug("Added API key header to request");
		}
	}

	/**
	 * Validates that we're connecting to a real provider account, not test/demo accounts.
	 * This method should be overridden by implementations to ensure we always use real
	 * data.
	 * @throws RuntimeException if validation fails
	 */
	protected abstract void validateRealProviderAccount();

	/**
	 * Signs request data using HMAC-SHA256 with the private key.
	 * @param data The data to sign
	 * @return Base64 encoded signature
	 * @throws RuntimeException if signing fails
	 */
	protected String signRequest(String data) {
		try {
			Mac hmac = Mac.getInstance("HmacSHA256");
			SecretKeySpec keySpec = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			hmac.init(keySpec);
			byte[] signature = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(signature);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Failed to sign request: {}", e.getMessage());
			throw new StrategizException(ClientErrorDetails.SIGNATURE_GENERATION_FAILED, "client-base", e);
		}
	}

	/**
	 * Check if the provider API is available.
	 * @return true if the API is available, false otherwise
	 */
	public abstract boolean checkPublicApiAvailability();

}
