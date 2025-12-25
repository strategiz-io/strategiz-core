package io.strategiz.service.console.quality.model;

/**
 * Quality grade scale (A-F) based on compliance percentage.
 */
public enum QualityGrade {

	A_PLUS("A+", 98.0),
	A("A", 95.0),
	A_MINUS("A-", 90.0),
	B_PLUS("B+", 85.0),
	B("B", 80.0),
	B_MINUS("B-", 75.0),
	C_PLUS("C+", 70.0),
	C("C", 65.0),
	C_MINUS("C-", 60.0),
	D("D", 50.0),
	F("F", 0.0);

	private final String label;

	private final double minPercentage;

	QualityGrade(String label, double minPercentage) {
		this.label = label;
		this.minPercentage = minPercentage;
	}

	public String getLabel() {
		return label;
	}

	public double getMinPercentage() {
		return minPercentage;
	}

	/**
	 * Calculate grade from compliance percentage.
	 * @param percentage compliance percentage (0-100)
	 * @return corresponding quality grade
	 */
	public static QualityGrade fromPercentage(double percentage) {
		for (QualityGrade grade : values()) {
			if (percentage >= grade.minPercentage) {
				return grade;
			}
		}
		return F;
	}

}
