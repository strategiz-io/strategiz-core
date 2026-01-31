package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal response for strategy create/update operations Returns only essential fields,
 * not the full code or performance data
 */
public class CreateStrategyResponse {

	@JsonProperty("id")
	private String id;

	@JsonProperty("name")
	private String name;

	@JsonProperty("isPublished")
	private Boolean isPublished;

	@JsonProperty("isPublic")
	private Boolean isPublic;

	@JsonProperty("isListed")
	private Boolean isListed;

	@JsonProperty("createdDate")
	private String createdDate;

	@JsonProperty("modifiedDate")
	private String modifiedDate;

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	public String getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

}
