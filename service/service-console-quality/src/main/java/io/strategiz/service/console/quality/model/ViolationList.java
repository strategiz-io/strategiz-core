package io.strategiz.service.console.quality.model;

import java.util.List;

/**
 * List of compliance violations with total count.
 */
public class ViolationList {

	private List<ComplianceViolation> violations;

	private int total;

	public ViolationList() {
	}

	public ViolationList(List<ComplianceViolation> violations, int total) {
		this.violations = violations;
		this.total = total;
	}

	public List<ComplianceViolation> getViolations() {
		return violations;
	}

	public void setViolations(List<ComplianceViolation> violations) {
		this.violations = violations;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

}
