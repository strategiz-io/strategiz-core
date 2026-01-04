package io.strategiz.service.portfolio.model.response;

/**
 * Response DTO for portfolio AI chat.
 * Contains the AI's reply to a user's question about their portfolio.
 */
public class PortfolioChatResponseDto {

	private String id;

	private String content;

	private String model;

	private Boolean success;

	private String error;

	private Integer tokensUsed;

	private Long timestamp; // Unix timestamp

	// Getters and Setters
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

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Integer getTokensUsed() {
		return tokensUsed;
	}

	public void setTokensUsed(Integer tokensUsed) {
		this.tokensUsed = tokensUsed;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

}
