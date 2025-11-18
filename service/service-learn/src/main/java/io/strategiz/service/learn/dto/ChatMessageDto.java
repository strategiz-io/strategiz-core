package io.strategiz.service.learn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for chat message in API requests/responses
 */
public class ChatMessageDto {

	@JsonProperty("role")
	@NotBlank(message = "Role is required")
	private String role; // "user" or "assistant"

	@JsonProperty("content")
	@NotBlank(message = "Content is required")
	private String content;

	@JsonProperty("timestamp")
	private String timestamp;

	public ChatMessageDto() {
	}

	public ChatMessageDto(String role, String content) {
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
