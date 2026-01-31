package io.strategiz.client.kraken;

import io.strategiz.client.kraken.model.KrakenAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.net.URI;
import java.util.Base64;

/**
 * Client for interacting with the Kraken exchange API This class handles all low-level
 * API communication with Kraken
 */
@Component
public class KrakenClient {

	private static final Logger log = LoggerFactory.getLogger(KrakenClient.class);

	private static final String ACCOUNT_BALANCE_PATH = "/0/private/Balance";

	@Value("${kraken.api.url:https://api.kraken.com}")
	private String baseUrl;

	private final RestTemplate restTemplate;

	public KrakenClient(@Qualifier("krakenRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("KrakenClient initialized with API URL: {}", baseUrl);
	}

	/**
	 * Get account information from Kraken API
	 * @param apiKey API key
	 * @param secretKey Secret key
	 * @return Account information
	 */
	public KrakenAccount getAccount(String apiKey, String secretKey) {
		log.info("Getting account information from Kraken API");

		if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
			log.error("API Key and Secret Key are required");
			KrakenAccount errorAccount = new KrakenAccount();
			errorAccount.setError(new String[] { "API Key and Secret Key are required" });
			return errorAccount;
		}

		try {
			// Generate nonce (timestamp in milliseconds)
			String nonce = String.valueOf(System.currentTimeMillis());

			// Prepare request data with nonce
			String requestData = "nonce=" + nonce;

			// Generate signature
			String signature = generateSignature(ACCOUNT_BALANCE_PATH, nonce, requestData, secretKey);

			// Set headers
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.set("API-Key", apiKey);
			headers.set("API-Sign", signature);

			// Create request entity
			HttpEntity<String> requestEntity = new HttpEntity<>(requestData, headers);

			// Make API call
			URI uri = URI.create(baseUrl + ACCOUNT_BALANCE_PATH);
			ResponseEntity<KrakenAccount> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity,
					KrakenAccount.class);

			return response.getBody();
		}
		catch (Exception e) {
			log.error("Error getting account information from Kraken API", e);
			KrakenAccount errorAccount = new KrakenAccount();
			errorAccount.setError(new String[] { e.getMessage() });
			return errorAccount;
		}
	}

	/**
	 * Test connection to Kraken API
	 * @param apiKey API key
	 * @param secretKey Secret key
	 * @return true if successful, false otherwise
	 */
	public boolean testConnection(String apiKey, String secretKey) {
		try {
			KrakenAccount account = getAccount(apiKey, secretKey);
			return account != null && (account.getError() == null || account.getError().length == 0);
		}
		catch (Exception e) {
			log.error("Error testing Kraken API connection", e);
			return false;
		}
	}

	/**
	 * Generate signature for Kraken API requests
	 * @param path API endpoint path
	 * @param nonce Nonce value
	 * @param data POST data
	 * @param secret API secret key
	 * @return Signature
	 * @throws Exception if there's an error generating the signature
	 */
	private String generateSignature(String path, String nonce, String data, String secret) throws Exception {
		// Decode base64 secret
		byte[] decodedSecret = Base64.getDecoder().decode(secret);

		// Create message to sign: nonce + POST data
		byte[] message = (nonce + data).getBytes();

		// Create SHA-256 hash of the message
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		byte[] messageHash = sha256.digest(message);

		// Create HMAC-SHA512 signature using the decoded secret and (path + SHA-256 hash)
		Mac hmacSha512 = Mac.getInstance("HmacSHA512");
		hmacSha512.init(new SecretKeySpec(decodedSecret, "HmacSHA512"));
		byte[] signature = hmacSha512.doFinal(concat(path.getBytes(), messageHash));

		// Encode signature as base64
		return Base64.getEncoder().encodeToString(signature);
	}

	/**
	 * Concatenate two byte arrays
	 * @param a First array
	 * @param b Second array
	 * @return Concatenated array
	 */
	private byte[] concat(byte[] a, byte[] b) {
		byte[] result = new byte[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	/**
	 * Check if the Kraken public API is available without authentication Used for health
	 * monitoring
	 * @return true if the API is available, false otherwise
	 */
	public boolean checkPublicApiAvailability() {
		log.debug("Checking Kraken public API availability");
		try {
			// Use the public Time endpoint which doesn't require authentication
			String url = baseUrl + "/0/public/Time";
			ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
			boolean available = response.getStatusCode().is2xxSuccessful();
			log.debug("Kraken public API available: {}", available);
			return available;
		}
		catch (Exception e) {
			log.debug("Kraken public API unavailable: {}", e.getMessage());
			return false;
		}
	}

}