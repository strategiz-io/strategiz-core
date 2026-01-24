package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * DTO for FMP technical indicator data from the /technical_indicator endpoint.
 *
 * <p>
 * Used for RSI, SMA, EMA, MACD and other technical indicators.
 * </p>
 *
 * <p>
 * FMP API endpoints:
 * <ul>
 * <li>RSI: /api/v3/technical_indicator/daily/{symbol}?type=rsi&period=14</li>
 * <li>SMA: /api/v3/technical_indicator/daily/{symbol}?type=sma&period=20</li>
 * <li>EMA: /api/v3/technical_indicator/daily/{symbol}?type=ema&period=20</li>
 * <li>MACD: /api/v3/technical_indicator/daily/{symbol}?type=macd</li>
 * </ul>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpTechnicalIndicator {

	private String date;

	private BigDecimal open;

	private BigDecimal high;

	private BigDecimal low;

	private BigDecimal close;

	private Long volume;

	// Technical indicator values - these vary by indicator type
	private BigDecimal rsi;

	private BigDecimal sma;

	private BigDecimal ema;

	// MACD components
	private BigDecimal macd;

	private BigDecimal macdSignal;

	private BigDecimal macdHist;

	// Getters and Setters

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public void setOpen(BigDecimal open) {
		this.open = open;
	}

	public BigDecimal getHigh() {
		return high;
	}

	public void setHigh(BigDecimal high) {
		this.high = high;
	}

	public BigDecimal getLow() {
		return low;
	}

	public void setLow(BigDecimal low) {
		this.low = low;
	}

	public BigDecimal getClose() {
		return close;
	}

	public void setClose(BigDecimal close) {
		this.close = close;
	}

	public Long getVolume() {
		return volume;
	}

	public void setVolume(Long volume) {
		this.volume = volume;
	}

	public BigDecimal getRsi() {
		return rsi;
	}

	public void setRsi(BigDecimal rsi) {
		this.rsi = rsi;
	}

	public BigDecimal getSma() {
		return sma;
	}

	public void setSma(BigDecimal sma) {
		this.sma = sma;
	}

	public BigDecimal getEma() {
		return ema;
	}

	public void setEma(BigDecimal ema) {
		this.ema = ema;
	}

	public BigDecimal getMacd() {
		return macd;
	}

	public void setMacd(BigDecimal macd) {
		this.macd = macd;
	}

	public BigDecimal getMacdSignal() {
		return macdSignal;
	}

	public void setMacdSignal(BigDecimal macdSignal) {
		this.macdSignal = macdSignal;
	}

	public BigDecimal getMacdHist() {
		return macdHist;
	}

	public void setMacdHist(BigDecimal macdHist) {
		this.macdHist = macdHist;
	}

	/**
	 * Interpret RSI value for context.
	 * @return interpretation string (e.g., "overbought", "neutral", "oversold")
	 */
	public String interpretRsi() {
		if (rsi == null) {
			return "N/A";
		}
		double val = rsi.doubleValue();
		if (val >= 70) {
			return "overbought";
		}
		else if (val <= 30) {
			return "oversold";
		}
		else if (val >= 60) {
			return "bullish";
		}
		else if (val <= 40) {
			return "bearish";
		}
		return "neutral";
	}

	/**
	 * Check if MACD is bullish (histogram positive).
	 * @return true if MACD histogram is positive
	 */
	public boolean isMacdBullish() {
		return macdHist != null && macdHist.compareTo(BigDecimal.ZERO) > 0;
	}

	/**
	 * Check if price is above a moving average.
	 * @return true if close is above the SMA or EMA
	 */
	public boolean isPriceAboveSma() {
		return close != null && sma != null && close.compareTo(sma) > 0;
	}

	/**
	 * Check if price is above EMA.
	 * @return true if close is above the EMA
	 */
	public boolean isPriceAboveEma() {
		return close != null && ema != null && close.compareTo(ema) > 0;
	}

	/**
	 * Format RSI for context display.
	 * @return formatted RSI string with interpretation
	 */
	public String toRsiContextString() {
		if (rsi == null) {
			return "RSI: N/A";
		}
		return String.format("RSI: %.1f (%s)", rsi.doubleValue(), interpretRsi());
	}

	/**
	 * Format MACD for context display.
	 * @return formatted MACD string
	 */
	public String toMacdContextString() {
		if (macd == null) {
			return "MACD: N/A";
		}
		String signal = isMacdBullish() ? "bullish" : "bearish";
		return String.format("MACD: %.3f, Signal: %.3f, Hist: %.3f (%s)", macd.doubleValue(),
				macdSignal != null ? macdSignal.doubleValue() : 0, macdHist != null ? macdHist.doubleValue() : 0,
				signal);
	}

}
