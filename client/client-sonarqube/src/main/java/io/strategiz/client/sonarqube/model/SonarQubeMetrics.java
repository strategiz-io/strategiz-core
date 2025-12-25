package io.strategiz.client.sonarqube.model;

/**
 * SonarQube project metrics aggregated from API responses.
 */
public class SonarQubeMetrics {

	private int bugs;

	private int vulnerabilities;

	private int codeSmells;

	private double coverage;

	private double duplications;

	private String technicalDebt;

	private String rating;

	public SonarQubeMetrics() {
	}

	public SonarQubeMetrics(int bugs, int vulnerabilities, int codeSmells, double coverage, double duplications,
			String technicalDebt, String rating) {
		this.bugs = bugs;
		this.vulnerabilities = vulnerabilities;
		this.codeSmells = codeSmells;
		this.coverage = coverage;
		this.duplications = duplications;
		this.technicalDebt = technicalDebt;
		this.rating = rating;
	}

	public int getBugs() {
		return bugs;
	}

	public void setBugs(int bugs) {
		this.bugs = bugs;
	}

	public int getVulnerabilities() {
		return vulnerabilities;
	}

	public void setVulnerabilities(int vulnerabilities) {
		this.vulnerabilities = vulnerabilities;
	}

	public int getCodeSmells() {
		return codeSmells;
	}

	public void setCodeSmells(int codeSmells) {
		this.codeSmells = codeSmells;
	}

	public double getCoverage() {
		return coverage;
	}

	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}

	public double getDuplications() {
		return duplications;
	}

	public void setDuplications(double duplications) {
		this.duplications = duplications;
	}

	public String getTechnicalDebt() {
		return technicalDebt;
	}

	public void setTechnicalDebt(String technicalDebt) {
		this.technicalDebt = technicalDebt;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

}
