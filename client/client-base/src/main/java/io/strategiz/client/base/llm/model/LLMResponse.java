package io.strategiz.client.base.llm.model;

/**
 * Unified response format from LLM providers.
 * Provider-agnostic representation of an LLM response.
 */
public class LLMResponse {

	private String content;

	private String model;

	private String provider;

	private Integer promptTokens;

	private Integer completionTokens;

	private Integer totalTokens;

	private boolean success;

	private String error;

	public LLMResponse() {
		this.success = true;
	}

	public LLMResponse(String content) {
		this();
		this.content = content;
	}

	public static LLMResponse success(String content, String model, String provider) {
		LLMResponse response = new LLMResponse(content);
		response.setModel(model);
		response.setProvider(provider);
		return response;
	}

	public static LLMResponse error(String errorMessage) {
		LLMResponse response = new LLMResponse();
		response.setSuccess(false);
		response.setError(errorMessage);
		return response;
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

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
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
