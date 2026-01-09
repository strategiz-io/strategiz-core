package io.strategiz.service.console.accessibility.model;

import java.util.List;

/**
 * Paginated list of accessibility violations.
 */
public class AxeViolationList {

	private List<AxeViolation> violations;

	private int total;

	public AxeViolationList() {
	}

	public AxeViolationList(List<AxeViolation> violations, int total) {
		this.violations = violations;
		this.total = total;
	}

	public List<AxeViolation> getViolations() {
		return violations;
	}

	public void setViolations(List<AxeViolation> violations) {
		this.violations = violations;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

}
