package io.strategiz.service.console.quality.model;

import java.time.Instant;

/**
 * Cached quality metrics from build-time SonarQube analysis. Stored in Firestore
 * to avoid needing a running SonarQube server.
 *
 * Firestore path: system/quality/latest
 */
public class CachedQualityMetrics {

	private String analysisId;

	private Instant analyzedAt;

	private String gitCommitHash;

	private String gitBranch;

	// SonarQube metrics
	private int bugs;

	private int vulnerabilities;

	private int codeSmells;

	private double coverage;

	private double duplications;

	private String technicalDebt;

	private String reliabilityRating;

	private String securityRating;

	private String maintainabilityRating;

	// Quality gate
	private String qualityGateStatus; // PASSED, FAILED

	private int totalIssues;

	private int newIssues;

	// Source
	private String analysisSource; // CI/CD, Manual, etc.

	private String buildNumber;

	public CachedQualityMetrics() {
	}

	public static Builder builder() {
		return new Builder();
	}

	// Getters and setters
	public String getAnalysisId() {
		return analysisId;
	}

	public void setAnalysisId(String analysisId) {
		this.analysisId = analysisId;
	}

	public Instant getAnalyzedAt() {
		return analyzedAt;
	}

	public void setAnalyzedAt(Instant analyzedAt) {
		this.analyzedAt = analyzedAt;
	}

	public String getGitCommitHash() {
		return gitCommitHash;
	}

	public void setGitCommitHash(String gitCommitHash) {
		this.gitCommitHash = gitCommitHash;
	}

	public String getGitBranch() {
		return gitBranch;
	}

	public void setGitBranch(String gitBranch) {
		this.gitBranch = gitBranch;
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

	public String getReliabilityRating() {
		return reliabilityRating;
	}

	public void setReliabilityRating(String reliabilityRating) {
		this.reliabilityRating = reliabilityRating;
	}

	public String getSecurityRating() {
		return securityRating;
	}

	public void setSecurityRating(String securityRating) {
		this.securityRating = securityRating;
	}

	public String getMaintainabilityRating() {
		return maintainabilityRating;
	}

	public void setMaintainabilityRating(String maintainabilityRating) {
		this.maintainabilityRating = maintainabilityRating;
	}

	public String getQualityGateStatus() {
		return qualityGateStatus;
	}

	public void setQualityGateStatus(String qualityGateStatus) {
		this.qualityGateStatus = qualityGateStatus;
	}

	public int getTotalIssues() {
		return totalIssues;
	}

	public void setTotalIssues(int totalIssues) {
		this.totalIssues = totalIssues;
	}

	public int getNewIssues() {
		return newIssues;
	}

	public void setNewIssues(int newIssues) {
		this.newIssues = newIssues;
	}

	public String getAnalysisSource() {
		return analysisSource;
	}

	public void setAnalysisSource(String analysisSource) {
		this.analysisSource = analysisSource;
	}

	public String getBuildNumber() {
		return buildNumber;
	}

	public void setBuildNumber(String buildNumber) {
		this.buildNumber = buildNumber;
	}

	public static class Builder {

		private final CachedQualityMetrics metrics = new CachedQualityMetrics();

		public Builder analysisId(String analysisId) {
			metrics.analysisId = analysisId;
			return this;
		}

		public Builder analyzedAt(Instant analyzedAt) {
			metrics.analyzedAt = analyzedAt;
			return this;
		}

		public Builder gitCommitHash(String gitCommitHash) {
			metrics.gitCommitHash = gitCommitHash;
			return this;
		}

		public Builder gitBranch(String gitBranch) {
			metrics.gitBranch = gitBranch;
			return this;
		}

		public Builder bugs(int bugs) {
			metrics.bugs = bugs;
			return this;
		}

		public Builder vulnerabilities(int vulnerabilities) {
			metrics.vulnerabilities = vulnerabilities;
			return this;
		}

		public Builder codeSmells(int codeSmells) {
			metrics.codeSmells = codeSmells;
			return this;
		}

		public Builder coverage(double coverage) {
			metrics.coverage = coverage;
			return this;
		}

		public Builder duplications(double duplications) {
			metrics.duplications = duplications;
			return this;
		}

		public Builder technicalDebt(String technicalDebt) {
			metrics.technicalDebt = technicalDebt;
			return this;
		}

		public Builder reliabilityRating(String reliabilityRating) {
			metrics.reliabilityRating = reliabilityRating;
			return this;
		}

		public Builder securityRating(String securityRating) {
			metrics.securityRating = securityRating;
			return this;
		}

		public Builder maintainabilityRating(String maintainabilityRating) {
			metrics.maintainabilityRating = maintainabilityRating;
			return this;
		}

		public Builder qualityGateStatus(String qualityGateStatus) {
			metrics.qualityGateStatus = qualityGateStatus;
			return this;
		}

		public Builder totalIssues(int totalIssues) {
			metrics.totalIssues = totalIssues;
			return this;
		}

		public Builder newIssues(int newIssues) {
			metrics.newIssues = newIssues;
			return this;
		}

		public Builder analysisSource(String analysisSource) {
			metrics.analysisSource = analysisSource;
			return this;
		}

		public Builder buildNumber(String buildNumber) {
			metrics.buildNumber = buildNumber;
			return this;
		}

		public CachedQualityMetrics build() {
			return metrics;
		}

	}

}
