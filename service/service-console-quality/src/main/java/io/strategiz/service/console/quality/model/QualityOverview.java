package io.strategiz.service.console.quality.model;

import java.time.Instant;

/**
 * Overall quality metrics summary for dashboard.
 */
public class QualityOverview {

	private String overallGrade;

	private double complianceScore;

	private String sonarQubeRating;

	private int bugs;

	private int vulnerabilities;

	private int codeSmells;

	private String technicalDebt;

	private Instant lastUpdated;

	public QualityOverview() {
	}

	public QualityOverview(String overallGrade, double complianceScore, String sonarQubeRating, int bugs,
			int vulnerabilities, int codeSmells, String technicalDebt, Instant lastUpdated) {
		this.overallGrade = overallGrade;
		this.complianceScore = complianceScore;
		this.sonarQubeRating = sonarQubeRating;
		this.bugs = bugs;
		this.vulnerabilities = vulnerabilities;
		this.codeSmells = codeSmells;
		this.technicalDebt = technicalDebt;
		this.lastUpdated = lastUpdated;
	}

	public String getOverallGrade() {
		return overallGrade;
	}

	public void setOverallGrade(String overallGrade) {
		this.overallGrade = overallGrade;
	}

	public double getComplianceScore() {
		return complianceScore;
	}

	public void setComplianceScore(double complianceScore) {
		this.complianceScore = complianceScore;
	}

	public String getSonarQubeRating() {
		return sonarQubeRating;
	}

	public void setSonarQubeRating(String sonarQubeRating) {
		this.sonarQubeRating = sonarQubeRating;
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

	public String getTechnicalDebt() {
		return technicalDebt;
	}

	public void setTechnicalDebt(String technicalDebt) {
		this.technicalDebt = technicalDebt;
	}

	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String overallGrade;

		private double complianceScore;

		private String sonarQubeRating;

		private int bugs;

		private int vulnerabilities;

		private int codeSmells;

		private String technicalDebt;

		private Instant lastUpdated;

		public Builder overallGrade(String overallGrade) {
			this.overallGrade = overallGrade;
			return this;
		}

		public Builder complianceScore(double complianceScore) {
			this.complianceScore = complianceScore;
			return this;
		}

		public Builder sonarQubeRating(String sonarQubeRating) {
			this.sonarQubeRating = sonarQubeRating;
			return this;
		}

		public Builder bugs(int bugs) {
			this.bugs = bugs;
			return this;
		}

		public Builder vulnerabilities(int vulnerabilities) {
			this.vulnerabilities = vulnerabilities;
			return this;
		}

		public Builder codeSmells(int codeSmells) {
			this.codeSmells = codeSmells;
			return this;
		}

		public Builder technicalDebt(String technicalDebt) {
			this.technicalDebt = technicalDebt;
			return this;
		}

		public Builder lastUpdated(Instant lastUpdated) {
			this.lastUpdated = lastUpdated;
			return this;
		}

		public QualityOverview build() {
			return new QualityOverview(overallGrade, complianceScore, sonarQubeRating, bugs, vulnerabilities,
					codeSmells, technicalDebt, lastUpdated);
		}

	}

}
