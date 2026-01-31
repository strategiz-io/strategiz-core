package io.strategiz.service.base.utils;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility class for handling rate limiting in service modules.
 *
 * Provides functionality for: - Adding standard rate limiting headers to HTTP responses -
 * Following HTTP conventions (RFC 7231 for Retry-After header)
 *
 * Note: Error response creation should be handled by throwing appropriate exceptions that
 * will be converted by GlobalExceptionHandler
 */
public class RateLimitUtils {

	private static final String RETRY_AFTER_HEADER = "Retry-After";

	private static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";

	private static final String RATE_LIMIT_RESET_HEADER = "X-Rate-Limit-Reset";

	/**
	 * Add rate limiting headers to response when rate limit is exceeded
	 * @param response HTTP response
	 * @param retryAfterSeconds Seconds to wait before retrying
	 * @param resetTimestamp Unix timestamp when rate limit resets
	 */
	public static void addRateLimitExceededHeaders(HttpServletResponse response, int retryAfterSeconds,
			long resetTimestamp) {
		response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
		response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
		response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(resetTimestamp));
	}

	/**
	 * Add rate limiting headers to response for successful requests
	 * @param response HTTP response
	 * @param remaining Number of requests remaining in current window
	 * @param resetTimestamp Unix timestamp when rate limit resets
	 */
	public static void addRateLimitHeaders(HttpServletResponse response, int remaining, long resetTimestamp) {
		response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
		response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(resetTimestamp));
	}

	/**
	 * Calculate Unix timestamp for rate limit reset
	 * @param secondsFromNow Seconds from now when rate limit resets
	 * @return Unix timestamp
	 */
	public static long calculateResetTimestamp(int secondsFromNow) {
		return (System.currentTimeMillis() / 1000) + secondsFromNow;
	}

	/**
	 * Create error context map for rate limiting exceptions
	 * @param retryAfterSeconds Seconds to wait before retrying
	 * @param limit The rate limit that was exceeded (optional)
	 * @param window The time window for the rate limit (optional)
	 * @return Context map for StrategizException
	 */
	public static java.util.Map<String, Object> createRateLimitContext(int retryAfterSeconds, Integer limit,
			String window) {
		java.util.Map<String, Object> context = new java.util.HashMap<>();
		context.put("retryAfterSeconds", retryAfterSeconds);
		if (limit != null) {
			context.put("limit", limit);
		}
		if (window != null) {
			context.put("window", window);
		}
		return context;
	}

}