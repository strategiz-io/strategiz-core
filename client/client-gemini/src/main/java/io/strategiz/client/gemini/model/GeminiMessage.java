package io.strategiz.client.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a message in a conversation with Gemini AI
 */
public class GeminiMessage {

	@JsonProperty("role")
	private String role; // "user" or "model"

	@JsonProperty("parts")
	private Parts parts;

	public GeminiMessage() {
	}

	public GeminiMessage(String role, String content) {
		this.role = role;
		this.parts = new Parts(content);
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Parts getParts() {
		return parts;
	}

	public void setParts(Parts parts) {
		this.parts = parts;
	}

	public String getContent() {
		return parts != null ? parts.text : null;
	}

	public static class Parts {

		@JsonProperty("text")
		private String text;

		public Parts() {
		}

		public Parts(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

}
