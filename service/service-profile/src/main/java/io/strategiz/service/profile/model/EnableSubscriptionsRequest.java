package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for enabling owner subscriptions.
 */
public class EnableSubscriptionsRequest {

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "5.00", message = "Monthly price must be at least $5.00")
    @JsonProperty("monthlyPrice")
    private BigDecimal monthlyPrice;

    @Size(min = 20, max = 500, message = "Profile pitch must be between 20 and 500 characters")
    @JsonProperty("profilePitch")
    private String profilePitch;

    // Default constructor
    public EnableSubscriptionsRequest() {
    }

    // Constructor with all fields
    public EnableSubscriptionsRequest(BigDecimal monthlyPrice, String profilePitch) {
        this.monthlyPrice = monthlyPrice;
        this.profilePitch = profilePitch;
    }

    // Getters and setters
    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public String getProfilePitch() {
        return profilePitch;
    }

    public void setProfilePitch(String profilePitch) {
        this.profilePitch = profilePitch;
    }
}
