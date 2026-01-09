package io.strategiz.service.console.accessibility.model;

import java.util.List;

/**
 * Request DTO for triggering on-demand accessibility scan.
 */
public class OnDemandScanRequest {

	private List<String> targetIds;

	private boolean includeAxe = true;

	private boolean includeLighthouse = true;

	public OnDemandScanRequest() {
	}

	public List<String> getTargetIds() {
		return targetIds;
	}

	public void setTargetIds(List<String> targetIds) {
		this.targetIds = targetIds;
	}

	public boolean isIncludeAxe() {
		return includeAxe;
	}

	public void setIncludeAxe(boolean includeAxe) {
		this.includeAxe = includeAxe;
	}

	public boolean isIncludeLighthouse() {
		return includeLighthouse;
	}

	public void setIncludeLighthouse(boolean includeLighthouse) {
		this.includeLighthouse = includeLighthouse;
	}

}
