package io.strategiz.business.aichat.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response from AI chat
 */
public class ChatResponse {

	private String id;

	private String content;

	private LocalDateTime timestamp;

	private Integer tokensUsed;

	private String model;

	private boolean success;

	private String error;

	/**
	 * Additional metadata for the response (type, title, summary, riskLevel, etc.)
	 * Used primarily for portfolio insights caching
	 */
	private Map<String, Object> metadata;

	public ChatResponse() {
		this.timestamp = LocalDateTime.now();
		this.success = true;
	}

	public ChatResponse(String content) {
		this();
		this.content = content;
	}

	public static ChatResponse error(String errorMessage) {
		ChatResponse response = new ChatResponse();
		response.success = false;
		response.error = errorMessage;
		return response;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getTokensUsed() {
		return tokensUsed;
	}

	public void setTokensUsed(Integer tokensUsed) {
		this.tokensUsed = tokensUsed;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}
