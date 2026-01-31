package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.strategy.entity.StrategyPerformance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class CreateStrategyRequest {

	@NotBlank(message = "Strategy name is required")
	@JsonProperty("name")
	private String name;

	@JsonProperty("description")
	private String description;

	@NotBlank(message = "Strategy code is required")
	@JsonProperty("code")
	private String code;

	@NotNull(message = "Language is required")
	@JsonProperty("language")
	private String language; // python, java, pinescript

	@JsonProperty("type")
	private String type = "technical"; // technical, fundamental, hybrid

	@JsonProperty("tags")
	private List<String> tags;

	@JsonProperty("parameters")
	private Map<String, Object> parameters;

	@JsonProperty("isPublished")
	private Boolean isPublished = false; // false = DRAFT, true = PUBLISHED

	@JsonProperty("isPublic")
	private Boolean isPublic = false; // When published: false = SUBSCRIBERS_ONLY, true =
										// PUBLIC

	@JsonProperty("isListed")
	private Boolean isListed = false; // false = NOT_LISTED, true = LISTED in marketplace

	@JsonProperty("performance")
	private StrategyPerformance performance;

	@JsonProperty("seedFundingDate")
	private String seedFundingDate; // ISO timestamp for custom backtest start date

	// Getters and Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public Boolean getIsPublished() {
		return isPublished;
	}

	public void setIsPublished(Boolean isPublished) {
		this.isPublished = isPublished;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Boolean getIsListed() {
		return isListed;
	}

	public void setIsListed(Boolean isListed) {
		this.isListed = isListed;
	}

	public StrategyPerformance getPerformance() {
		return performance;
	}

	public void setPerformance(StrategyPerformance performance) {
		this.performance = performance;
	}

	public String getSeedFundingDate() {
		return seedFundingDate;
	}

	public void setSeedFundingDate(String seedFundingDate) {
		this.seedFundingDate = seedFundingDate;
	}

}