package io.strategiz.service.console.quality.model;

/**
 * Compliance metric for a specific framework pattern.
 */
public class ComplianceMetric {

	private double compliance;

	private int violations;

	private int total;

	private String grade;

	public ComplianceMetric() {
	}

	public ComplianceMetric(double compliance, int violations, int total, String grade) {
		this.compliance = compliance;
		this.violations = violations;
		this.total = total;
		this.grade = grade;
	}

	public double getCompliance() {
		return compliance;
	}

	public void setCompliance(double compliance) {
		this.compliance = compliance;
	}

	public int getViolations() {
		return violations;
	}

	public void setViolations(int violations) {
		this.violations = violations;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	/**
	 * Calculate compliance from counts.
	 * @param compliant number of compliant instances
	 * @param total total instances
	 * @return compliance metric with calculated grade
	 */
	public static ComplianceMetric from(int compliant, int total) {
		if (total == 0) {
			return new ComplianceMetric(100.0, 0, 0, QualityGrade.A_PLUS.getLabel());
		}

		int violations = total - compliant;
		double percentage = (compliant * 100.0) / total;
		QualityGrade grade = QualityGrade.fromPercentage(percentage);

		return new ComplianceMetric(Math.round(percentage * 10.0) / 10.0, violations, total, grade.getLabel());
	}

}
