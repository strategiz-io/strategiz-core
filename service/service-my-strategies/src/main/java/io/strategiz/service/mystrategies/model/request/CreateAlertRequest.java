package io.strategiz.service.mystrategies.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request model for deploying a new strategy alert. Corresponds to the "Deploy Alert
 * Dialog" in the UX spec.
 */
public class CreateAlertRequest {

	@NotBlank(message = "Strategy ID is required")
	@JsonProperty("strategyId")
	private String strategyId;

	@NotBlank(message = "Alert name is required")
	@Size(max = 50, message = "Alert name must not exceed 50 characters")
	@JsonProperty("alertName")
	private String alertName;

	@NotEmpty(message = "At least one symbol is required")
	@Size(max = 10, message = "Maximum 10 symbols allowed")
	@JsonProperty("symbols")
	private List<String> symbols;

	@NotBlank(message = "Provider ID is required")
	@JsonProperty("providerId")
	private String providerId;

	@NotBlank(message = "Exchange is required")
	@JsonProperty("exchange")
	private String exchange; // NYSE, NASDAQ, CRYPTO

	@NotEmpty(message = "At least one notification channel is required")
	@JsonProperty("notificationChannels")
	private List<String> notificationChannels; // email, push, in-app, sms

	// Contact info override (optional - defaults from user profile when
	// useDefaultContact=true)
	@JsonProperty("useDefaultContact")
	private Boolean useDefaultContact = true;

	@JsonProperty("emailOverride")
	private String emailOverride;

	@JsonProperty("phoneOverride")
	private String phoneOverride;

	// Getters and Setters
	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getAlertName() {
		return alertName;
	}

	public void setAlertName(String alertName) {
		this.alertName = alertName;
	}

	public List<String> getSymbols() {
		return symbols;
	}

	public void setSymbols(List<String> symbols) {
		this.symbols = symbols;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public List<String> getNotificationChannels() {
		return notificationChannels;
	}

	public void setNotificationChannels(List<String> notificationChannels) {
		this.notificationChannels = notificationChannels;
	}

	public Boolean getUseDefaultContact() {
		return useDefaultContact;
	}

	public void setUseDefaultContact(Boolean useDefaultContact) {
		this.useDefaultContact = useDefaultContact;
	}

	public String getEmailOverride() {
		return emailOverride;
	}

	public void setEmailOverride(String emailOverride) {
		this.emailOverride = emailOverride;
	}

	public String getPhoneOverride() {
		return phoneOverride;
	}

	public void setPhoneOverride(String phoneOverride) {
		this.phoneOverride = phoneOverride;
	}

}
