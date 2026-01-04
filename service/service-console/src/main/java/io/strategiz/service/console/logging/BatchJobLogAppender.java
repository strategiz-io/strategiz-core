package io.strategiz.service.console.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.strategiz.service.console.service.JobLogStreamService;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Custom Logback appender that captures batch job logs and streams them to connected SSE clients.
 *
 * Thread-safe design:
 * - Appenders are called synchronously by Logback, so append() must be fast
 * - Uses async broadcast to avoid blocking the logging thread
 * - Circular buffer prevents unbounded memory growth
 *
 * Integration:
 * - Configured in logback-spring.xml with filter for batch jobs
 * - Auto-detects job context via MDC or thread name
 * - Lazy initialization of JobLogStreamService via ApplicationContext
 */
public class BatchJobLogAppender extends AppenderBase<ILoggingEvent> {

	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
		.withZone(ZoneId.systemDefault());

	// Job name patterns to monitor (configured via logback-spring.xml)
	private String jobNamePatterns = "MarketData.*Job,Fundamentals.*Job";

	// Lazy-loaded service (Spring beans not available during Logback initialization)
	private volatile JobLogStreamService logStreamService;

	private ApplicationContext applicationContext;

	@Override
	protected void append(ILoggingEvent event) {
		// Fast path: check if this log is from a batch job
		String jobName = extractJobName(event);
		if (jobName == null) {
			return; // Not a batch job log, skip
		}

		// Lazy initialize service
		if (logStreamService == null) {
			initializeService();
		}

		if (logStreamService == null) {
			return; // Service not available yet
		}

		// Format log event to JSON
		LogEvent logEvent = formatLogEvent(event, jobName);

		// Broadcast to all connected clients (async, non-blocking)
		logStreamService.broadcastLog(jobName, logEvent);
	}

	/**
	 * Extract job name from MDC context or thread name.
	 * Priority: MDC["jobName"] > MDC["jobExecutionId"] > thread name pattern
	 */
	private String extractJobName(ILoggingEvent event) {
		Map<String, String> mdcProperties = event.getMDCPropertyMap();

		// Check MDC for explicit job name
		if (mdcProperties.containsKey("jobName")) {
			return mdcProperties.get("jobName");
		}

		// Check thread name for job pattern
		String threadName = event.getThreadName();
		String[] patterns = jobNamePatterns.split(",");
		for (String pattern : patterns) {
			if (threadName.contains(pattern.trim())) {
				return extractJobNameFromThreadName(threadName);
			}
		}

		// Check logger name as fallback
		String loggerName = event.getLoggerName();
		if (loggerName.contains("batch.marketdata") || loggerName.contains("batch.fundamentals")) {
			return extractJobNameFromLogger(loggerName);
		}

		return null;
	}

	private String extractJobNameFromThreadName(String threadName) {
		// Example: "pool-2-thread-1-marketDataBackfill" -> "marketDataBackfill"
		if (threadName.contains("marketDataBackfill"))
			return "marketDataBackfill";
		if (threadName.contains("marketDataIncremental"))
			return "marketDataIncremental";
		if (threadName.contains("fundamentalsBackfill"))
			return "fundamentalsBackfill";
		if (threadName.contains("fundamentalsIncremental"))
			return "fundamentalsIncremental";
		return null;
	}

	private String extractJobNameFromLogger(String loggerName) {
		if (loggerName.contains("MarketDataBackfillJob"))
			return "marketDataBackfill";
		if (loggerName.contains("MarketDataIncrementalJob"))
			return "marketDataIncremental";
		if (loggerName.contains("FundamentalsBackfillJob"))
			return "fundamentalsBackfill";
		if (loggerName.contains("FundamentalsIncrementalJob"))
			return "fundamentalsIncremental";
		return null;
	}

	/**
	 * Format ILoggingEvent to structured LogEvent JSON.
	 */
	private LogEvent formatLogEvent(ILoggingEvent event, String jobName) {
		return new LogEvent(TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())),
				event.getLevel().toString(), event.getFormattedMessage(), event.getLoggerName(),
				event.getThreadName(), jobName, event.getMDCPropertyMap());
	}

	/**
	 * Lazy initialize JobLogStreamService from Spring ApplicationContext.
	 * Called only once when first log event arrives.
	 */
	private synchronized void initializeService() {
		if (logStreamService != null) {
			return;
		}

		try {
			// Get ApplicationContext from LogbackConfigurationHelper
			applicationContext = LogbackConfigurationHelper.getApplicationContext();
			if (applicationContext != null) {
				logStreamService = applicationContext.getBean(JobLogStreamService.class);
				addInfo("BatchJobLogAppender initialized with JobLogStreamService");
			}
			else {
				addWarn("ApplicationContext not available, log streaming disabled");
			}
		}
		catch (Exception e) {
			addError("Failed to initialize JobLogStreamService", e);
		}
	}

	// Setters for Logback configuration
	public void setJobNamePatterns(String patterns) {
		this.jobNamePatterns = patterns;
	}

}
