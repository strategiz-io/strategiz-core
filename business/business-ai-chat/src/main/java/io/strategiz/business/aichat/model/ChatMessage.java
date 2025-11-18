package io.strategiz.business.aichat.model;

import java.time.LocalDateTime;

/**
 * Represents a chat message between user and AI
 */
public class ChatMessage {

	private String id;

	private String role; // "user" or "assistant"

	private String content;

	private LocalDateTime timestamp;

	private ChatContext context;

	public ChatMessage() {
		this.timestamp = LocalDateTime.now();
	}

	public ChatMessage(String role, String content) {
		this();
		this.role = role;
		this.content = content;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public ChatContext getContext() {
		return context;
	}

	public void setContext(ChatContext context) {
		this.context = context;
	}

}
