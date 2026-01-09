package io.strategiz.service.console.accessibility.model;

import java.time.Instant;

/**
 * Response DTO for on-demand accessibility scan status.
 */
public class OnDemandScanResponse {

	private String scanId;

	private String status; // PENDING, RUNNING, COMPLETED, FAILED

	private int progress; // 0-100

	private Instant startedAt;

	private Instant completedAt;

	private String error;

	public OnDemandScanResponse() {
	}

	public OnDemandScanResponse(String scanId, String status, int progress) {
		this.scanId = scanId;
		this.status = status;
		this.progress = progress;
	}

	public String getScanId() {
		return scanId;
	}

	public void setScanId(String scanId) {
		this.scanId = scanId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
