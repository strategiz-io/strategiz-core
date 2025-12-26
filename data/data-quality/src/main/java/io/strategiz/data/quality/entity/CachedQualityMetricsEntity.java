package io.strategiz.data.quality.entity;

import java.time.Instant;

import com.google.cloud.firestore.annotation.DocumentId;

/**
 * Firestore entity for cached quality metrics from build-time analysis.
 *
 * Firestore path: system/quality_cache/{analysisId}
 * Latest: system/quality_cache/latest
 */
public class CachedQualityMetricsEntity {

	@DocumentId
	private String analysisId;

	private Instant analyzedAt;

	private String gitCommitHash;

	private String gitBranch;

	// SonarQube-style metrics
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
	private String qualityGateStatus; // PASSED, FAILED, UNKNOWN

	private int totalIssues;

	private int newIssues;

	// Source info
	private String analysisSource; // GitHub Actions, Cloud Build, Manual, etc.

	private String buildNumber;

	// Constructor
	public CachedQualityMetricsEntity() {
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

}
