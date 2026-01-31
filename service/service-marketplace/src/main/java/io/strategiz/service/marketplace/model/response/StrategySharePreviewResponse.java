package io.strategiz.service.marketplace.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Minimal strategy preview response for social media sharing. Used to generate Open Graph
 * metadata for social media platforms.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategySharePreviewResponse {

	@JsonProperty("strategyId")
	private String strategyId;

	@JsonProperty("name")
	private String name;

	@JsonProperty("description")
	private String description; // Truncated to 200 chars

	@JsonProperty("performanceSummary")
	private String performanceSummary; // e.g., "+45% return, 65% win rate"

	@JsonProperty("thumbnailUrl")
	private String thumbnailUrl; // Chart image or logo

	@JsonProperty("pageUrl")
	private String pageUrl; // Full URL to strategy detail page

	@JsonProperty("creator")
	private CreatorInfo creator;

	/**
	 * Creator information for social sharing
	 */
	public static class CreatorInfo {

		@JsonProperty("name")
		private String name;

		@JsonProperty("photoURL")
		private String photoURL;

		// Constructors
		public CreatorInfo() {
		}

		public CreatorInfo(String name, String photoURL) {
			this.name = name;
			this.photoURL = photoURL;
		}

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPhotoURL() {
			return photoURL;
		}

		public void setPhotoURL(String photoURL) {
			this.photoURL = photoURL;
		}

	}

	// Constructors
	public StrategySharePreviewResponse() {
	}

	// Getters and Setters
	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

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

	public String getPerformanceSummary() {
		return performanceSummary;
	}

	public void setPerformanceSummary(String performanceSummary) {
		this.performanceSummary = performanceSummary;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	public CreatorInfo getCreator() {
		return creator;
	}

	public void setCreator(CreatorInfo creator) {
		this.creator = creator;
	}

}
