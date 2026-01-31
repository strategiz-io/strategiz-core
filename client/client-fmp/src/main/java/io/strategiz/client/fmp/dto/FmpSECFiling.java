package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * DTO representing an SEC filing from FMP API.
 *
 * <p>
 * FMP Endpoint: GET /api/v3/sec_filings/{symbol}?type={type}&limit={limit}
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpSECFiling {

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("cik")
	private String cik;

	@JsonProperty("type")
	private String formType;

	@JsonProperty("fillingDate")
	private String filedDate;

	@JsonProperty("acceptedDate")
	private String acceptedDate;

	@JsonProperty("link")
	private String reportUrl;

	@JsonProperty("finalLink")
	private String filingUrl;

	@JsonProperty("accessNumber")
	private String accessNumber;

	// Getters and setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getCik() {
		return cik;
	}

	public void setCik(String cik) {
		this.cik = cik;
	}

	public String getFormType() {
		return formType;
	}

	public void setFormType(String formType) {
		this.formType = formType;
	}

	public String getFiledDate() {
		return filedDate;
	}

	public void setFiledDate(String filedDate) {
		this.filedDate = filedDate;
	}

	public String getAcceptedDate() {
		return acceptedDate;
	}

	public void setAcceptedDate(String acceptedDate) {
		this.acceptedDate = acceptedDate;
	}

	public String getReportUrl() {
		return reportUrl;
	}

	public void setReportUrl(String reportUrl) {
		this.reportUrl = reportUrl;
	}

	public String getFilingUrl() {
		return filingUrl;
	}

	public void setFilingUrl(String filingUrl) {
		this.filingUrl = filingUrl;
	}

	public String getAccessNumber() {
		return accessNumber;
	}

	public void setAccessNumber(String accessNumber) {
		this.accessNumber = accessNumber;
	}

	/**
	 * Get filed date as LocalDate.
	 */
	public LocalDate getFiledDateAsLocalDate() {
		if (filedDate == null) {
			return null;
		}
		// FMP date may include time portion, take only the date part
		String datePart = filedDate.length() > 10 ? filedDate.substring(0, 10) : filedDate;
		return LocalDate.parse(datePart);
	}

	/**
	 * Get human-readable form description.
	 */
	public String getFormDescription() {
		if (formType == null) {
			return "Unknown Filing";
		}
		return switch (formType.toUpperCase()) {
			case "10-K" -> "Annual Report (10-K)";
			case "10-Q" -> "Quarterly Report (10-Q)";
			case "8-K" -> "Current Report (8-K)";
			case "4" -> "Insider Trading (Form 4)";
			case "SC 13G" -> "Beneficial Ownership (13G)";
			case "SC 13D" -> "Activist Ownership (13D)";
			case "DEF 14A" -> "Proxy Statement";
			case "S-1" -> "IPO Registration";
			case "424B4" -> "Prospectus";
			default -> formType;
		};
	}

	/**
	 * Check if this is a major filing type.
	 */
	public boolean isMajorFiling() {
		if (formType == null) {
			return false;
		}
		String upperForm = formType.toUpperCase();
		return upperForm.equals("10-K") || upperForm.equals("10-Q") || upperForm.equals("8-K")
				|| upperForm.equals("DEF 14A");
	}

	/**
	 * Format for AI context injection.
	 */
	public String toContextString() {
		return String.format("[%s] %s - %s\nFiled: %s | URL: %s", symbol != null ? symbol : "N/A", getFormDescription(),
				formType, filedDate != null ? filedDate : "Unknown", reportUrl != null ? reportUrl : filingUrl);
	}

	@Override
	public String toString() {
		return "FmpSECFiling{" + "symbol='" + symbol + '\'' + ", formType='" + formType + '\'' + ", filedDate='"
				+ filedDate + '\'' + '}';
	}

}
