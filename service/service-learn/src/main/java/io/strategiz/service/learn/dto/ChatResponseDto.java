package io.strategiz.service.learn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for chat API response
 */
public class ChatResponseDto {

	@JsonProperty("id")
	private String id;

	@JsonProperty("content")
	private String content;

	@JsonProperty("timestamp")
	private String timestamp;

	@JsonProperty("tokensUsed")
	private Integer tokensUsed;

	@JsonProperty("model")
	private String model;

	@JsonProperty("success")
	private boolean success;

	@JsonProperty("error")
	private String error;

	public ChatResponseDto() {
		this.success = true;
	}

	public ChatResponseDto(String content) {
		this();
		this.content = content;
	}

	public static ChatResponseDto error(String errorMessage) {
		ChatResponseDto response = new ChatResponseDto();
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

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
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

}
