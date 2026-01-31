package io.strategiz.service.agents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for chat message in conversation history
 */
public class AgentChatMessage {

	@JsonProperty("role")
	private String role;

	@JsonProperty("content")
	private String content;

	@JsonProperty("timestamp")
	private String timestamp;

	public AgentChatMessage() {
	}

	public AgentChatMessage(String role, String content) {
		this.role = role;
		this.content = content;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
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

}
