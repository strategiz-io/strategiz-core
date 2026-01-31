package io.strategiz.client.etrade.auth;

import io.strategiz.client.etrade.error.EtradeErrors;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth 1.0a signature generation utility for E*TRADE API. Implements HMAC-SHA1 signing
 * as required by E*TRADE.
 *
 * OAuth 1.0a signature process: 1. Collect all OAuth parameters 2. Collect all request
 * parameters 3. Create signature base string 4. Sign with HMAC-SHA1 using consumer secret
 * and token secret 5. Base64 encode the result
 */
public class OAuth1aSignature {

	private static final Logger log = LoggerFactory.getLogger(OAuth1aSignature.class);

	private static final String HMAC_SHA1 = "HmacSHA1";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private OAuth1aSignature() {
		// Utility class
	}

	/**
	 * Generate OAuth 1.0a signature for a request.
	 * @param httpMethod HTTP method (GET, POST, etc.)
	 * @param baseUrl The base URL without query parameters
	 * @param oauthParams OAuth parameters (oauth_consumer_key, oauth_token, etc.)
	 * @param requestParams Additional request parameters (query string or body)
	 * @param consumerSecret Consumer secret from E*TRADE app registration
	 * @param tokenSecret Token secret (empty string for request token, actual secret for
	 * access token)
	 * @return Base64-encoded HMAC-SHA1 signature
	 */
	public static String sign(String httpMethod, String baseUrl, Map<String, String> oauthParams,
			Map<String, String> requestParams, String consumerSecret, String tokenSecret) {
		try {
			// 1. Combine all parameters (OAuth + request)
			Map<String, String> allParams = new TreeMap<>();
			if (oauthParams != null) {
				allParams.putAll(oauthParams);
			}
			if (requestParams != null) {
				allParams.putAll(requestParams);
			}

			// 2. Create parameter string (sorted alphabetically)
			String parameterString = allParams.entrySet()
				.stream()
				.map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
				.collect(Collectors.joining("&"));

			// 3. Create signature base string
			String signatureBaseString = httpMethod.toUpperCase() + "&" + percentEncode(baseUrl) + "&"
					+ percentEncode(parameterString);

			log.debug("Signature base string: {}", signatureBaseString);

			// 4. Create signing key (consumer_secret&token_secret)
			String signingKey = percentEncode(consumerSecret) + "&"
					+ (tokenSecret != null ? percentEncode(tokenSecret) : "");

			// 5. Generate HMAC-SHA1 signature
			Mac mac = Mac.getInstance(HMAC_SHA1);
			SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA1);
			mac.init(keySpec);
			byte[] signatureBytes = mac.doFinal(signatureBaseString.getBytes(StandardCharsets.UTF_8));

			// 6. Base64 encode
			return Base64.getEncoder().encodeToString(signatureBytes);

		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Failed to generate OAuth signature", e);
			throw new StrategizException(EtradeErrors.ETRADE_SIGNATURE_ERROR,
					"Failed to generate OAuth signature: " + e.getMessage());
		}
	}

	/**
	 * Generate a random nonce for OAuth requests.
	 * @return A random alphanumeric string
	 */
	public static String generateNonce() {
		byte[] bytes = new byte[16];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * Generate OAuth timestamp (seconds since epoch).
	 * @return Current Unix timestamp as string
	 */
	public static String generateTimestamp() {
		return String.valueOf(System.currentTimeMillis() / 1000);
	}

	/**
	 * Build Authorization header value from OAuth parameters and signature.
	 * @param oauthParams OAuth parameters including signature
	 * @return Authorization header value (e.g., "OAuth oauth_consumer_key=...,
	 * oauth_signature=...")
	 */
	public static String buildAuthorizationHeader(Map<String, String> oauthParams) {
		String params = oauthParams.entrySet()
			.stream()
			.map(e -> percentEncode(e.getKey()) + "=\"" + percentEncode(e.getValue()) + "\"")
			.collect(Collectors.joining(", "));

		return "OAuth " + params;
	}

	/**
	 * Create base OAuth parameters for a request.
	 * @param consumerKey E*TRADE consumer key
	 * @param token OAuth token (request token or access token, can be null)
	 * @return Map of OAuth parameters (without signature)
	 */
	public static Map<String, String> createBaseOAuthParams(String consumerKey, String token) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("oauth_consumer_key", consumerKey);
		params.put("oauth_signature_method", "HMAC-SHA1");
		params.put("oauth_timestamp", generateTimestamp());
		params.put("oauth_nonce", generateNonce());
		params.put("oauth_version", "1.0");

		if (token != null && !token.isEmpty()) {
			params.put("oauth_token", token);
		}

		return params;
	}

	/**
	 * Percent-encode a string according to RFC 3986.
	 * @param value The string to encode
	 * @return Percent-encoded string
	 */
	public static String percentEncode(String value) {
		if (value == null) {
			return "";
		}
		return URLEncoder.encode(value, StandardCharsets.UTF_8)
			.replace("+", "%20")
			.replace("*", "%2A")
			.replace("%7E", "~");
	}

}
