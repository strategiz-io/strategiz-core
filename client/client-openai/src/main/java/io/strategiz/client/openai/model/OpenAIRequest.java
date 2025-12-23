package io.strategiz.client.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request object for OpenAI Chat Completions API.
 */
public class OpenAIRequest {

	@JsonProperty("model")
	private String model;

	@JsonProperty("messages")
	private List<Message> messages;

	@JsonProperty("temperature")
	private Double temperature;

	@JsonProperty("max_tokens")
	private Integer maxTokens;

	@JsonProperty("stream")
	private Boolean stream;

	public OpenAIRequest() {
	}

	public OpenAIRequest(String model, List<Message> messages, Double temperature, Integer maxTokens, Boolean stream) {
		this.model = model;
		this.messages = messages;
		this.temperature = temperature;
		this.maxTokens = maxTokens;
		this.stream = stream;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Boolean getStream() {
		return stream;
	}

	public void setStream(Boolean stream) {
		this.stream = stream;
	}

	/**
	 * Message object for OpenAI API.
	 */
	public static class Message {

		@JsonProperty("role")
		private String role; // "system", "user", "assistant"

		@JsonProperty("content")
		private String content;

		public Message() {
		}

		public Message(String role, String content) {
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

	}

}
