package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * DTO representing a press release from FMP API.
 *
 * <p>
 * Press releases are official company announcements, typically more significant than
 * regular news.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpPressRelease {

	private static final DateTimeFormatter FMP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("date")
	private String date;

	@JsonProperty("title")
	private String title;

	@JsonProperty("text")
	private String text;

	// Getters and setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get date as Instant.
	 */
	public Instant getDateAsInstant() {
		if (date == null || date.isBlank()) {
			return null;
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(date, FMP_DATE_FORMAT);
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
		sb.append("[PRESS RELEASE] [").append(date != null ? date : "Unknown").append("] ");
		sb.append(title != null ? title : "No title");

		if (text != null && !text.isBlank()) {
			sb.append("\n").append(text.substring(0, Math.min(text.length(), 500)));
			if (text.length() > 500) {
				sb.append("...");
			}
		}

		if (symbol != null && !symbol.isBlank()) {
			sb.append("\nSymbol: ").append(symbol);
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return "FmpPressRelease{" + "symbol='" + symbol + '\'' + ", title='" + title + '\'' + ", date='" + date + '\''
				+ '}';
	}

}
