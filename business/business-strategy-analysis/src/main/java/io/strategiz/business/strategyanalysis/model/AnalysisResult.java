package io.strategiz.business.strategyanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Result of strategy analysis, unified response for all modes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResult {

	@JsonProperty("mode")
	private AnalysisMode mode;

	@JsonProperty("context")
	private AnalysisContext context;

	@JsonProperty("suggestions")
	private List<Suggestion> suggestions;

	@JsonProperty("summary")
	private String summary; // Overall explanation

	@JsonProperty("metadata")
	private Map<String, Object> metadata; // Extensible for future data

	@JsonProperty("success")
	private boolean success = true;

	@JsonProperty("error")
	private String error; // Error message if analysis failed

	public AnalysisResult() {
	}

	// Getters and setters

	public AnalysisMode getMode() {
		return mode;
	}

	public void setMode(AnalysisMode mode) {
		this.mode = mode;
	}

	public AnalysisContext getContext() {
		return context;
	}

	public void setContext(AnalysisContext context) {
		this.context = context;
	}

	public List<Suggestion> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<Suggestion> suggestions) {
		this.suggestions = suggestions;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
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

	// Builder-style helpers

	public static AnalysisResult error(String errorMessage) {
		AnalysisResult result = new AnalysisResult();
		result.setSuccess(false);
		result.setError(errorMessage);
		return result;
	}

}
