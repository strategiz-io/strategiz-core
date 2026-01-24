package io.strategiz.service.agents.dto;

import java.math.BigDecimal;

/**
 * DTO for market signals shown in the Scout Agent insights panel.
 */
public class MarketSignalDto {

	private String type; // momentum, oversold, overbought, sentiment, breakout

	private String direction; // bullish, bearish, neutral

	private String asset; // Symbol or sector name

	private String detail; // Description of the signal

	private String confidence; // High, Medium, Low

	private BigDecimal value; // RSI value, price change %, etc.

	public MarketSignalDto() {
	}

	public MarketSignalDto(String type, String direction, String asset, String detail, String confidence) {
		this.type = type;
		this.direction = direction;
		this.asset = asset;
		this.detail = detail;
		this.confidence = confidence;
	}

	public MarketSignalDto(String type, String direction, String asset, String detail, String confidence,
			BigDecimal value) {
		this.type = type;
		this.direction = direction;
		this.asset = asset;
		this.detail = detail;
		this.confidence = confidence;
		this.value = value;
	}

	// Getters and setters

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getAsset() {
		return asset;
	}

	public void setAsset(String asset) {
		this.asset = asset;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getConfidence() {
		return confidence;
	}

	public void setConfidence(String confidence) {
		this.confidence = confidence;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

}
