package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wrapper for alert and bot deployments of a strategy.
 */
public class DeploymentsDTO {

    @JsonProperty("alerts")
    private List<AlertDeploymentDTO> alerts;

    @JsonProperty("bots")
    private List<BotDeploymentDTO> bots;

    // Getters and Setters
    public List<AlertDeploymentDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertDeploymentDTO> alerts) {
        this.alerts = alerts;
    }

    public List<BotDeploymentDTO> getBots() {
        return bots;
    }

    public void setBots(List<BotDeploymentDTO> bots) {
        this.bots = bots;
    }
}
