package io.strategiz.service.base.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.UUID;

/**
 * Interceptor that adds standard headers to ALL responses (success and error).
 *
 * Standard Headers Added: - X-Trace-ID: Unique trace for this request/response cycle -
 * X-Request-ID: Client-provided or server-generated request ID - X-Timestamp: Server
 * timestamp when response was generated - X-API-Version: API version being used -
 * X-Server-ID: Which server instance handled the request - X-Response-Time: How long
 * server took to process (added in postHandle)
 */
@Component
public class StandardHeadersInterceptor implements HandlerInterceptor {

	private static final String API_VERSION = "v1";

	private static final String SERVER_ID = getServerInstanceId();

	private static final String TRACE_ID_HEADER = "X-Trace-ID";

	private static final String REQUEST_ID_HEADER = "X-Request-ID";

	private static final String TIMESTAMP_HEADER = "X-Timestamp";

	private static final String API_VERSION_HEADER = "X-API-Version";

	private static final String SERVER_ID_HEADER = "X-Server-ID";

	private static final String RESPONSE_TIME_HEADER = "X-Response-Time";

	private static final String START_TIME_ATTR = "requestStartTime";

	/**
	 * Before request processing - set up tracing and initial headers
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// Record start time for response time calculation
		request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

		// Get or generate trace ID
		String traceId = getOrGenerateTraceId(request);
		MDC.put("traceId", traceId);

		// Get or generate request ID
		String requestId = getOrGenerateRequestId(request);
		MDC.put("requestId", requestId);

		// Add initial headers (timestamp will be updated in postHandle)
		response.setHeader(TRACE_ID_HEADER, traceId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		response.setHeader(API_VERSION_HEADER, API_VERSION);
		response.setHeader(SERVER_ID_HEADER, SERVER_ID);

		return true;
	}

	/**
	 * After request processing - add final headers with timing
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		try {
			// Add final timestamp
			response.setHeader(TIMESTAMP_HEADER, Instant.now().toString());

			// Calculate and add response time
			Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
			if (startTime != null) {
				long responseTime = System.currentTimeMillis() - startTime;
				response.setHeader(RESPONSE_TIME_HEADER, responseTime + "ms");
			}
		}
		finally {
			// Clean up MDC
			MDC.clear();
		}
	}

	/**
	 * Get trace ID from request header or generate new one
	 */
	private String getOrGenerateTraceId(HttpServletRequest request) {
		String traceId = request.getHeader(TRACE_ID_HEADER);
		if (traceId == null || traceId.trim().isEmpty()) {
			traceId = generateTraceId();
		}
		return traceId;
	}

	/**
	 * Get request ID from request header or generate new one
	 */
	private String getOrGenerateRequestId(HttpServletRequest request) {
		String requestId = request.getHeader(REQUEST_ID_HEADER);
		if (requestId == null || requestId.trim().isEmpty()) {
			requestId = generateRequestId();
		}
		return requestId;
	}

	/**
	 * Generate a new trace ID
	 */
	private String generateTraceId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Generate a new request ID
	 */
	private String generateRequestId() {
		return "req-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Get server instance identifier
	 */
	private static String getServerInstanceId() {
		// Try to get from environment variables first
		String serverId = System.getenv("SERVER_INSTANCE_ID");
		if (serverId != null && !serverId.trim().isEmpty()) {
			return serverId;
		}

		// Fallback to hostname + short UUID
		try {
			String hostname = java.net.InetAddress.getLocalHost().getHostName();
			String shortId = UUID.randomUUID().toString().substring(0, 8);
			return hostname + "-" + shortId;
		}
		catch (Exception e) {
			// Ultimate fallback
			return "api-server-" + UUID.randomUUID().toString().substring(0, 8);
		}
	}

}