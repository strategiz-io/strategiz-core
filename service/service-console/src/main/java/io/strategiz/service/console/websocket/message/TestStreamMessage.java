package io.strategiz.service.console.websocket.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for all test streaming WebSocket messages
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestStreamMessage {

	public enum MessageType {

		LOG, // Raw log line
		TEST_START, // Test started
		TEST_RESULT, // Individual test result
		SUITE_START, // Suite started
		SUITE_RESULT, // Suite completed
		PROGRESS, // Progress update
		COMPLETION, // Test run completed
		ERROR // Error occurred

	}

	@JsonProperty("type")
	private MessageType type;

	@JsonProperty("runId")
	private String runId;

	@JsonProperty("timestamp")
	private long timestamp;

	public TestStreamMessage() {
		this.timestamp = System.currentTimeMillis();
	}

	public TestStreamMessage(MessageType type, String runId) {
		this.type = type;
		this.runId = runId;
		this.timestamp = System.currentTimeMillis();
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
