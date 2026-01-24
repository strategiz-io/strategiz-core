package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * DTO representing a news article from FMP API.
 *
 * <p>
 * Supports both stock news and general market news endpoints.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpNewsArticle {

	private static final DateTimeFormatter FMP_DATE_FORMAT = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss");

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("publishedDate")
	private String publishedDate;

	@JsonProperty("title")
	private String title;

	@JsonProperty("image")
	private String image;

	@JsonProperty("site")
	private String site;

	@JsonProperty("text")
	private String text;

	@JsonProperty("url")
	private String url;

	// Getters and setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getPublishedDate() {
		return publishedDate;
	}

	public void setPublishedDate(String publishedDate) {
		this.publishedDate = publishedDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get published date as Instant.
	 */
	public Instant getPublishedDateAsInstant() {
		if (publishedDate == null || publishedDate.isBlank()) {
			return null;
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(publishedDate, FMP_DATE_FORMAT);
			return ldt.toInstant(ZoneOffset.UTC);
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Format for AI context injection.
	 */
	public String toContextString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(publishedDate != null ? publishedDate : "Unknown").append("] ");
		sb.append(title != null ? title : "No title");

		if (text != null && !text.isBlank()) {
			sb.append("\n").append(text.substring(0, Math.min(text.length(), 300)));
			if (text.length() > 300) {
				sb.append("...");
			}
		}

		sb.append("\nSource: ").append(site != null ? site : "Unknown");

		if (symbol != null && !symbol.isBlank()) {
			sb.append(" | Symbol: ").append(symbol);
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return "FmpNewsArticle{" + "symbol='" + symbol + '\'' + ", title='" + title + '\'' + ", site='" + site + '\''
				+ ", publishedDate='" + publishedDate + '\'' + '}';
	}

}
