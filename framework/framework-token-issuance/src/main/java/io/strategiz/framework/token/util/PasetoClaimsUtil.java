package io.strategiz.framework.token.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.paseto4j.commons.PasetoException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for handling PASETO token claims with paseto4j.
 *
 * <p>Since paseto4j is a lower-level library that only handles encryption/decryption,
 * this utility provides:
 * <ul>
 *   <li>JSON serialization of claims for token creation</li>
 *   <li>JSON deserialization of claims from token payload</li>
 *   <li>Expiration validation (which jpaseto did automatically)</li>
 *   <li>Standard claim handling (exp, iat, iss, aud, sub, jti)</li>
 * </ul>
 */
public final class PasetoClaimsUtil {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
	};

	private PasetoClaimsUtil() {
		// Utility class
	}

	/**
	 * Serializes claims map to JSON string for token encryption.
	 * @param claims the claims map
	 * @return JSON string representation
	 * @throws PasetoException if serialization fails
	 */
	public static String serializeClaims(Map<String, Object> claims) {
		try {
			return objectMapper.writeValueAsString(claims);
		}
		catch (JsonProcessingException e) {
			throw new PasetoException("Failed to serialize token claims: " + e.getMessage());
		}
	}

	/**
	 * Deserializes JSON payload to claims map.
	 * @param payload the JSON payload from token decryption
	 * @return claims map
	 * @throws PasetoException if deserialization fails
	 */
	public static Map<String, Object> deserializeClaims(String payload) {
		try {
			return objectMapper.readValue(payload, MAP_TYPE_REF);
		}
		catch (JsonProcessingException e) {
			throw new PasetoException("Failed to deserialize token claims: " + e.getMessage());
		}
	}

	/**
	 * Validates token expiration. Throws exception if token is expired.
	 * @param claims the claims map containing 'exp' claim
	 * @throws PasetoException if token is expired or has no expiration
	 */
	public static void validateExpiration(Map<String, Object> claims) {
		Object expObj = claims.get("exp");
		if (expObj == null) {
			throw new PasetoException("Token has no expiration claim");
		}

		Instant exp = parseInstant(expObj);
		if (exp == null) {
			throw new PasetoException("Invalid expiration claim format");
		}

		if (Instant.now().isAfter(exp)) {
			throw new PasetoException("Token has expired");
		}
	}

	/**
	 * Creates a new claims builder with standard claims pre-configured.
	 * @return a new ClaimsBuilder instance
	 */
	public static ClaimsBuilder builder() {
		return new ClaimsBuilder();
	}

	/**
	 * Parses various numeric formats to Instant.
	 * @param value the value to parse (Long, Number, or Instant)
	 * @return the Instant, or null if unparseable
	 */
	public static Instant parseInstant(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Instant) {
			return (Instant) value;
		}
		if (value instanceof Long) {
			return Instant.ofEpochSecond((Long) value);
		}
		if (value instanceof Number) {
			return Instant.ofEpochSecond(((Number) value).longValue());
		}
		return null;
	}

	/**
	 * Builder for constructing claims map with standard PASETO claims.
	 */
	public static class ClaimsBuilder {

		private final Map<String, Object> claims = new LinkedHashMap<>();

		/**
		 * Sets the subject (sub) claim - typically the user ID.
		 */
		public ClaimsBuilder subject(String subject) {
			claims.put("sub", subject);
			return this;
		}

		/**
		 * Sets the issuer (iss) claim.
		 */
		public ClaimsBuilder issuer(String issuer) {
			claims.put("iss", issuer);
			return this;
		}

		/**
		 * Sets the audience (aud) claim.
		 */
		public ClaimsBuilder audience(String audience) {
			claims.put("aud", audience);
			return this;
		}

		/**
		 * Sets the expiration (exp) claim as epoch seconds.
		 */
		public ClaimsBuilder expiration(Instant expiration) {
			claims.put("exp", expiration.getEpochSecond());
			return this;
		}

		/**
		 * Sets the issued at (iat) claim as epoch seconds.
		 */
		public ClaimsBuilder issuedAt(Instant issuedAt) {
			claims.put("iat", issuedAt.getEpochSecond());
			return this;
		}

		/**
		 * Sets the token ID (jti) claim.
		 */
		public ClaimsBuilder tokenId(String tokenId) {
			claims.put("jti", tokenId);
			return this;
		}

		/**
		 * Sets a custom claim.
		 */
		public ClaimsBuilder claim(String name, Object value) {
			claims.put(name, value);
			return this;
		}

		/**
		 * Builds the claims map.
		 */
		public Map<String, Object> build() {
			return new LinkedHashMap<>(claims);
		}

		/**
		 * Builds and serializes the claims to JSON.
		 */
		public String buildJson() {
			return serializeClaims(claims);
		}

	}

}
