package io.strategiz.service.agents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for AI Agent chat response
 */
public class AgentChatResponse {

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

	@JsonProperty("agentId")
	private String agentId;

	@JsonProperty("success")
	private boolean success;

	@JsonProperty("error")
	private String error;

	public AgentChatResponse() {
		this.success = true;
	}

	public AgentChatResponse(String content) {
		this();
		this.content = content;
	}

	public static AgentChatResponse error(String errorMessage) {
		AgentChatResponse response = new AgentChatResponse();
		response.success = false;
		response.error = errorMessage;
		return response;
	}

	public static AgentChatResponse error(String agentId, String errorMessage) {
		AgentChatResponse response = error(errorMessage);
		response.agentId = agentId;
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

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
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
