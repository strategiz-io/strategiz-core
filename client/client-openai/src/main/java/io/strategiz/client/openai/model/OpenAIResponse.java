package io.strategiz.client.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response object from OpenAI Chat Completions API.
 */
public class OpenAIResponse {

	@JsonProperty("id")
	private String id;

	@JsonProperty("object")
	private String object;

	@JsonProperty("created")
	private Long created;

	@JsonProperty("model")
	private String model;

	@JsonProperty("choices")
	private List<Choice> choices;

	@JsonProperty("usage")
	private Usage usage;

	public OpenAIResponse() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<Choice> getChoices() {
		return choices;
	}

	public void setChoices(List<Choice> choices) {
		this.choices = choices;
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	/**
	 * Choice object from OpenAI response.
	 */
	public static class Choice {

		@JsonProperty("index")
		private Integer index;

		@JsonProperty("message")
		private Message message;

		@JsonProperty("delta")
		private Delta delta; // For streaming responses

		@JsonProperty("finish_reason")
		private String finishReason;

		public Choice() {
		}

		public Integer getIndex() {
			return index;
		}

		public void setIndex(Integer index) {
			this.index = index;
		}

		public Message getMessage() {
			return message;
		}

		public void setMessage(Message message) {
			this.message = message;
		}

		public Delta getDelta() {
			return delta;
		}

		public void setDelta(Delta delta) {
			this.delta = delta;
		}

		public String getFinishReason() {
			return finishReason;
		}

		public void setFinishReason(String finishReason) {
			this.finishReason = finishReason;
		}

	}

	/**
	 * Message object from OpenAI response.
	 */
	public static class Message {

		@JsonProperty("role")
		private String role;

		@JsonProperty("content")
		private String content;

		public Message() {
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

	/**
	 * Delta object for streaming responses.
	 */
	public static class Delta {

		@JsonProperty("role")
		private String role;

		@JsonProperty("content")
		private String content;

		public Delta() {
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

	/**
	 * Usage statistics from OpenAI response.
	 */
	public static class Usage {

		@JsonProperty("prompt_tokens")
		private Integer promptTokens;

		@JsonProperty("completion_tokens")
		private Integer completionTokens;

		@JsonProperty("total_tokens")
		private Integer totalTokens;

		public Usage() {
		}

		public Integer getPromptTokens() {
			return promptTokens;
		}

		public void setPromptTokens(Integer promptTokens) {
			this.promptTokens = promptTokens;
		}

		public Integer getCompletionTokens() {
			return completionTokens;
		}

		public void setCompletionTokens(Integer completionTokens) {
			this.completionTokens = completionTokens;
		}

		public Integer getTotalTokens() {
			return totalTokens;
		}

		public void setTotalTokens(Integer totalTokens) {
			this.totalTokens = totalTokens;
		}

	}

}
