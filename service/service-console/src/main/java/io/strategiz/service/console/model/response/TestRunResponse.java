package io.strategiz.service.console.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Response from running Playwright E2E tests.
 */
@Schema(description = "Response from running Playwright E2E tests")
public class TestRunResponse {

	@Schema(description = "Test suite ID", example = "system")
	private String suite;

	@Schema(description = "Test execution status", example = "passed",
			allowableValues = { "running", "passed", "failed", "pending" })
	private String status;

	@Schema(description = "Number of tests that passed", example = "8")
	private int passed;

	@Schema(description = "Number of tests that failed", example = "0")
	private int failed;

	@Schema(description = "Total number of tests", example = "8")
	private int total;

	@Schema(description = "Test duration in milliseconds", example = "15000")
	private Long duration;

	@Schema(description = "Test start time")
	private Instant startedAt;

	@Schema(description = "Test progress percentage (0-100)", example = "100")
	private Integer progress;

	public TestRunResponse() {
	}

	public String getSuite() {
		return suite;
	}

	public void setSuite(String suite) {
		this.suite = suite;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getPassed() {
		return passed;
	}

	public void setPassed(int passed) {
		this.passed = passed;
	}

	public int getFailed() {
		return failed;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

}
