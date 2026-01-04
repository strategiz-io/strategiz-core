package io.strategiz.service.console.logging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Structured log event for streaming to frontend via SSE.
 *
 * Represents a single log entry from a batch job execution with all metadata
 * necessary for display and filtering in the admin console.
 */
public class LogEvent {

	@JsonProperty("timestamp")
	private final String timestamp;

	@JsonProperty("level")
	private final String level;

	@JsonProperty("message")
	private final String message;

	@JsonProperty("logger")
	private final String logger;

	@JsonProperty("thread")
	private final String thread;

	@JsonProperty("jobName")
	private final String jobName;

	@JsonProperty("mdc")
	private final Map<String, String> mdc;

	public LogEvent(String timestamp, String level, String message, String logger, String thread, String jobName,
			Map<String, String> mdc) {
		this.timestamp = timestamp;
		this.level = level;
		this.message = message;
		this.logger = logger;
		this.thread = thread;
		this.jobName = jobName;
		this.mdc = mdc;
	}

	// Getters for Jackson serialization
	public String getTimestamp() {
		return timestamp;
	}

	public String getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public String getLogger() {
		return logger;
	}

	public String getThread() {
		return thread;
	}

	public String getJobName() {
		return jobName;
	}

	public Map<String, String> getMdc() {
		return mdc;
	}

}
