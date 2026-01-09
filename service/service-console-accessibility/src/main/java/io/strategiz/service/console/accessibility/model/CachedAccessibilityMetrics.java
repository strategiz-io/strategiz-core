package io.strategiz.service.console.accessibility.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for cached accessibility metrics from CI/CD or on-demand scans.
 * Used as request body for POST /cache endpoint.
 */
public class CachedAccessibilityMetrics {

	private String scanId;

	private Instant scannedAt;

	private String gitCommitHash;

	private String gitBranch;

	private String buildNumber;

	private String scanSource;

	private String appId;

	private String appName;

	private String targetUrl;

	private int totalViolations;

	private int criticalCount;

	private int seriousCount;

	private int moderateCount;

	private int minorCount;

	private double wcagCompliance;

	private String overallGrade;

	private int lighthouseAccessibility;

	private int lighthousePerformance;

	private int lighthouseSeo;

	private int lighthouseBestPractices;

	private Map<String, Integer> violationsByWcag;

	private List<AxeViolation> violations;

	public CachedAccessibilityMetrics() {
	}

	public String getScanId() {
		return scanId;
	}

	public void setScanId(String scanId) {
		this.scanId = scanId;
	}

	public Instant getScannedAt() {
		return scannedAt;
	}

	public void setScannedAt(Instant scannedAt) {
		this.scannedAt = scannedAt;
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

	public String getBuildNumber() {
		return buildNumber;
	}

	public void setBuildNumber(String buildNumber) {
		this.buildNumber = buildNumber;
	}

	public String getScanSource() {
		return scanSource;
	}

	public void setScanSource(String scanSource) {
		this.scanSource = scanSource;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public int getTotalViolations() {
		return totalViolations;
	}

	public void setTotalViolations(int totalViolations) {
		this.totalViolations = totalViolations;
	}

	public int getCriticalCount() {
		return criticalCount;
	}

	public void setCriticalCount(int criticalCount) {
		this.criticalCount = criticalCount;
	}

	public int getSeriousCount() {
		return seriousCount;
	}

	public void setSeriousCount(int seriousCount) {
		this.seriousCount = seriousCount;
	}

	public int getModerateCount() {
		return moderateCount;
	}

	public void setModerateCount(int moderateCount) {
		this.moderateCount = moderateCount;
	}

	public int getMinorCount() {
		return minorCount;
	}

	public void setMinorCount(int minorCount) {
		this.minorCount = minorCount;
	}

	public double getWcagCompliance() {
		return wcagCompliance;
	}

	public void setWcagCompliance(double wcagCompliance) {
		this.wcagCompliance = wcagCompliance;
	}

	public String getOverallGrade() {
		return overallGrade;
	}

	public void setOverallGrade(String overallGrade) {
		this.overallGrade = overallGrade;
	}

	public int getLighthouseAccessibility() {
		return lighthouseAccessibility;
	}

	public void setLighthouseAccessibility(int lighthouseAccessibility) {
		this.lighthouseAccessibility = lighthouseAccessibility;
	}

	public int getLighthousePerformance() {
		return lighthousePerformance;
	}

	public void setLighthousePerformance(int lighthousePerformance) {
		this.lighthousePerformance = lighthousePerformance;
	}

	public int getLighthouseSeo() {
		return lighthouseSeo;
	}

	public void setLighthouseSeo(int lighthouseSeo) {
		this.lighthouseSeo = lighthouseSeo;
	}

	public int getLighthouseBestPractices() {
		return lighthouseBestPractices;
	}

	public void setLighthouseBestPractices(int lighthouseBestPractices) {
		this.lighthouseBestPractices = lighthouseBestPractices;
	}

	public Map<String, Integer> getViolationsByWcag() {
		return violationsByWcag;
	}

	public void setViolationsByWcag(Map<String, Integer> violationsByWcag) {
		this.violationsByWcag = violationsByWcag;
	}

	public List<AxeViolation> getViolations() {
		return violations;
	}

	public void setViolations(List<AxeViolation> violations) {
		this.violations = violations;
	}

}
