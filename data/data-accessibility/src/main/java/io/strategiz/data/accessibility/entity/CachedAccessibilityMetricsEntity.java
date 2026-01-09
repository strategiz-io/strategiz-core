package io.strategiz.data.accessibility.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.cloud.firestore.annotation.DocumentId;

/**
 * Firestore entity for cached accessibility metrics from CI/CD or on-demand scans.
 *
 * Firestore path: system/accessibility_cache/{scanId}
 * Latest by app: system/accessibility_cache/latest_{appId}
 */
public class CachedAccessibilityMetricsEntity {

	@DocumentId
	private String scanId;

	private Instant scannedAt;

	private String gitCommitHash;

	private String gitBranch;

	private String buildNumber;

	private String scanSource; // CI/CD, Manual

	// App identification (following data-testing pattern)
	private String appId; // web, auth, console

	private String appName; // Strategiz Web, Auth Portal, Admin Console

	// Target URL that was scanned
	private String targetUrl;

	// Aggregated violation counts by impact level
	private int totalViolations;

	private int criticalCount;

	private int seriousCount;

	private int moderateCount;

	private int minorCount;

	// WCAG compliance percentage (0-100)
	private double wcagCompliance;

	// Overall grade (A, B, C, D, F)
	private String overallGrade;

	// Lighthouse scores (0-100)
	private int lighthouseAccessibility;

	private int lighthousePerformance;

	private int lighthouseSeo;

	private int lighthouseBestPractices;

	// Violation counts by WCAG criterion (e.g., {"1.4.3": 5, "2.1.1": 3})
	private Map<String, Integer> violationsByWcag;

	// Detailed violations (stored as nested structure)
	private List<AxeViolationData> violations;

	public CachedAccessibilityMetricsEntity() {
	}

	// Nested class for axe-core violation data
	public static class AxeViolationData {

		private String ruleId;

		private String description;

		private String impact; // critical, serious, moderate, minor

		private String wcagCriteria;

		private List<String> tags;

		private String targetSelector;

		private String htmlSnippet;

		private String helpUrl;

		private int nodeCount;

		public AxeViolationData() {
		}

		public String getRuleId() {
			return ruleId;
		}

		public void setRuleId(String ruleId) {
			this.ruleId = ruleId;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getImpact() {
			return impact;
		}

		public void setImpact(String impact) {
			this.impact = impact;
		}

		public String getWcagCriteria() {
			return wcagCriteria;
		}

		public void setWcagCriteria(String wcagCriteria) {
			this.wcagCriteria = wcagCriteria;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

		public String getTargetSelector() {
			return targetSelector;
		}

		public void setTargetSelector(String targetSelector) {
			this.targetSelector = targetSelector;
		}

		public String getHtmlSnippet() {
			return htmlSnippet;
		}

		public void setHtmlSnippet(String htmlSnippet) {
			this.htmlSnippet = htmlSnippet;
		}

		public String getHelpUrl() {
			return helpUrl;
		}

		public void setHelpUrl(String helpUrl) {
			this.helpUrl = helpUrl;
		}

		public int getNodeCount() {
			return nodeCount;
		}

		public void setNodeCount(int nodeCount) {
			this.nodeCount = nodeCount;
		}

	}

	// Getters and setters
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

	public List<AxeViolationData> getViolations() {
		return violations;
	}

	public void setViolations(List<AxeViolationData> violations) {
		this.violations = violations;
	}

}
