package io.strategiz.framework.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Strategiz Framework Logging.
 *
 * Allows customization of logging behavior through application properties.
 *
 * Example configuration:
 *
 * strategiz.logging: performance: enabled: true slow-request-threshold: 2000 security:
 * enabled: true mask-sensitive-data: true correlation: enabled: true header-name:
 * X-Correlation-ID
 */
@ConfigurationProperties(prefix = "strategiz.logging")
public class LoggingProperties {

	/**
	 * Whether structured logging is enabled
	 */
	private boolean enabled = true;

	/**
	 * Performance logging configuration
	 */
	private Performance performance = new Performance();

	/**
	 * Security logging configuration
	 */
	private Security security = new Security();

	/**
	 * Correlation ID configuration
	 */
	private Correlation correlation = new Correlation();

	// Getters and Setters
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Performance getPerformance() {
		return performance;
	}

	public void setPerformance(Performance performance) {
		this.performance = performance;
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
	}

	public Correlation getCorrelation() {
		return correlation;
	}

	public void setCorrelation(Correlation correlation) {
		this.correlation = correlation;
	}

	public static class Performance {

		/**
		 * Whether performance logging is enabled
		 */
		private boolean enabled = true;

		/**
		 * Threshold in milliseconds for logging slow requests
		 */
		private long slowRequestThreshold = 1000;

		/**
		 * Whether to log database query performance
		 */
		private boolean logDatabaseQueries = true;

		/**
		 * Whether to log external API call performance
		 */
		private boolean logExternalCalls = true;

		// Getters and Setters
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public long getSlowRequestThreshold() {
			return slowRequestThreshold;
		}

		public void setSlowRequestThreshold(long slowRequestThreshold) {
			this.slowRequestThreshold = slowRequestThreshold;
		}

		public boolean isLogDatabaseQueries() {
			return logDatabaseQueries;
		}

		public void setLogDatabaseQueries(boolean logDatabaseQueries) {
			this.logDatabaseQueries = logDatabaseQueries;
		}

		public boolean isLogExternalCalls() {
			return logExternalCalls;
		}

		public void setLogExternalCalls(boolean logExternalCalls) {
			this.logExternalCalls = logExternalCalls;
		}

	}

	public static class Security {

		/**
		 * Whether security logging is enabled
		 */
		private boolean enabled = true;

		/**
		 * Whether to mask sensitive data in logs
		 */
		private boolean maskSensitiveData = true;

		/**
		 * Whether to log authentication events
		 */
		private boolean logAuthEvents = true;

		/**
		 * Whether to log authorization events
		 */
		private boolean logAuthzEvents = true;

		// Getters and Setters
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isMaskSensitiveData() {
			return maskSensitiveData;
		}

		public void setMaskSensitiveData(boolean maskSensitiveData) {
			this.maskSensitiveData = maskSensitiveData;
		}

		public boolean isLogAuthEvents() {
			return logAuthEvents;
		}

		public void setLogAuthEvents(boolean logAuthEvents) {
			this.logAuthEvents = logAuthEvents;
		}

		public boolean isLogAuthzEvents() {
			return logAuthzEvents;
		}

		public void setLogAuthzEvents(boolean logAuthzEvents) {
			this.logAuthzEvents = logAuthzEvents;
		}

	}

	public static class Correlation {

		/**
		 * Whether correlation ID tracking is enabled
		 */
		private boolean enabled = true;

		/**
		 * Header name for correlation ID
		 */
		private String headerName = "X-Correlation-ID";

		/**
		 * Header name for request ID
		 */
		private String requestIdHeaderName = "X-Request-ID";

		/**
		 * Whether to generate correlation ID if not present
		 */
		private boolean generateIfMissing = true;

		// Getters and Setters
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getHeaderName() {
			return headerName;
		}

		public void setHeaderName(String headerName) {
			this.headerName = headerName;
		}

		public String getRequestIdHeaderName() {
			return requestIdHeaderName;
		}

		public void setRequestIdHeaderName(String requestIdHeaderName) {
			this.requestIdHeaderName = requestIdHeaderName;
		}

		public boolean isGenerateIfMissing() {
			return generateIfMissing;
		}

		public void setGenerateIfMissing(boolean generateIfMissing) {
			this.generateIfMissing = generateIfMissing;
		}

	}

}