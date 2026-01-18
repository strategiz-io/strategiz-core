package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.client.stripe.StripeConnectService.ConnectAccountStatus;

import java.util.List;

/**
 * Response DTO for Stripe Connect account status.
 */
public class StripeConnectStatusResponse {

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("status")
    private String status;  // not_started, pending, active, restricted

    @JsonProperty("detailsSubmitted")
    private boolean detailsSubmitted;

    @JsonProperty("chargesEnabled")
    private boolean chargesEnabled;

    @JsonProperty("payoutsEnabled")
    private boolean payoutsEnabled;

    @JsonProperty("pendingRequirements")
    private List<String> pendingRequirements;

    @JsonProperty("currentlyDue")
    private List<String> currentlyDue;

    // Default constructor
    public StripeConnectStatusResponse() {
    }

    // Full constructor
    public StripeConnectStatusResponse(
            String accountId,
            String status,
            boolean detailsSubmitted,
            boolean chargesEnabled,
            boolean payoutsEnabled,
            List<String> pendingRequirements,
            List<String> currentlyDue) {
        this.accountId = accountId;
        this.status = status;
        this.detailsSubmitted = detailsSubmitted;
        this.chargesEnabled = chargesEnabled;
        this.payoutsEnabled = payoutsEnabled;
        this.pendingRequirements = pendingRequirements;
        this.currentlyDue = currentlyDue;
    }

    /**
     * Create a response from ConnectAccountStatus.
     */
    public static StripeConnectStatusResponse fromConnectStatus(ConnectAccountStatus status) {
        return new StripeConnectStatusResponse(
                status.accountId(),
                status.status(),
                status.detailsSubmitted(),
                status.chargesEnabled(),
                status.payoutsEnabled(),
                status.pendingRequirements(),
                status.currentlyDue()
        );
    }

    // Getters and setters
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDetailsSubmitted() {
        return detailsSubmitted;
    }

    public void setDetailsSubmitted(boolean detailsSubmitted) {
        this.detailsSubmitted = detailsSubmitted;
    }

    public boolean isChargesEnabled() {
        return chargesEnabled;
    }

    public void setChargesEnabled(boolean chargesEnabled) {
        this.chargesEnabled = chargesEnabled;
    }

    public boolean isPayoutsEnabled() {
        return payoutsEnabled;
    }

    public void setPayoutsEnabled(boolean payoutsEnabled) {
        this.payoutsEnabled = payoutsEnabled;
    }

    public List<String> getPendingRequirements() {
        return pendingRequirements;
    }

    public void setPendingRequirements(List<String> pendingRequirements) {
        this.pendingRequirements = pendingRequirements;
    }

    public List<String> getCurrentlyDue() {
        return currentlyDue;
    }

    public void setCurrentlyDue(List<String> currentlyDue) {
        this.currentlyDue = currentlyDue;
    }

    /**
     * Check if account is ready to receive payments.
     */
    public boolean isReady() {
        return "active".equals(status) && payoutsEnabled;
    }

    /**
     * Check if onboarding needs to be completed.
     */
    public boolean needsOnboarding() {
        return "not_started".equals(status) || "pending".equals(status) ||
                (currentlyDue != null && !currentlyDue.isEmpty());
    }
}
