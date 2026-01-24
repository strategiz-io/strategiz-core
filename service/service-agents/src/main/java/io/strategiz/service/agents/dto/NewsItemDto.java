package io.strategiz.service.agents.dto;

import java.util.List;

/**
 * DTO for news items shown in the News Agent insights panel.
 */
public class NewsItemDto {

	private String title;

	private String summary;

	private String source;

	private String url;

	private String publishedAt;

	private List<String> symbols; // Related stock symbols

	private String sentiment; // positive, negative, neutral

	private String category; // earnings, merger, market, crypto, forex

	public NewsItemDto() {
	}

	public NewsItemDto(String title, String summary, String source, String publishedAt) {
		this.title = title;
		this.summary = summary;
		this.source = source;
		this.publishedAt = publishedAt;
	}

	// Getters and setters

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(String publishedAt) {
		this.publishedAt = publishedAt;
	}

	public List<String> getSymbols() {
		return symbols;
	}

	public void setSymbols(List<String> symbols) {
		this.symbols = symbols;
	}

	public String getSentiment() {
		return sentiment;
	}

	public void setSentiment(String sentiment) {
		this.sentiment = sentiment;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

}
