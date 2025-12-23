package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response model for scheduled job information.
 */
public class JobResponse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status; // RUNNING, PAUSED, IDLE

    @JsonProperty("schedule")
    private String schedule; // Cron expression

    @JsonProperty("lastRunTime")
    private Instant lastRunTime;

    @JsonProperty("nextRunTime")
    private Instant nextRunTime;

    @JsonProperty("lastRunStatus")
    private String lastRunStatus; // SUCCESS, FAILED, RUNNING

    @JsonProperty("lastRunDurationMs")
    private Long lastRunDurationMs;

    @JsonProperty("enabled")
    private boolean enabled = true;

    // Constructors
    public JobResponse() {
    }

    public JobResponse(String name, String description, String status, String schedule) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.schedule = schedule;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public Instant getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(Instant lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public Instant getNextRunTime() {
        return nextRunTime;
    }

    public void setNextRunTime(Instant nextRunTime) {
        this.nextRunTime = nextRunTime;
    }

    public String getLastRunStatus() {
        return lastRunStatus;
    }

    public void setLastRunStatus(String lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }

    public Long getLastRunDurationMs() {
        return lastRunDurationMs;
    }

    public void setLastRunDurationMs(Long lastRunDurationMs) {
        this.lastRunDurationMs = lastRunDurationMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
