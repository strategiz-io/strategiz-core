package io.strategiz.service.console.accessibility.model;

import java.time.Instant;

/**
 * Overall accessibility metrics summary for dashboard.
 */
public class AccessibilityOverview {

	private String overallGrade;

	private double wcagCompliance;

	private int totalViolations;

	private int criticalViolations;

	private int seriousViolations;

	private int moderateViolations;

	private int minorViolations;

	// Lighthouse scores (0-100)
	private int lighthouseAccessibility;

	private int lighthousePerformance;

	private int lighthouseSeo;

	private int lighthouseBestPractices;

	private Instant lastScanTime;

	private String lastScanSource;

	// App info
	private String appId;

	private String appName;

	public AccessibilityOverview() {
	}

	// Getters and setters
	public String getOverallGrade() {
		return overallGrade;
	}

	public void setOverallGrade(String overallGrade) {
		this.overallGrade = overallGrade;
	}

	public double getWcagCompliance() {
		return wcagCompliance;
	}

	public void setWcagCompliance(double wcagCompliance) {
		this.wcagCompliance = wcagCompliance;
	}

	public int getTotalViolations() {
		return totalViolations;
	}

	public void setTotalViolations(int totalViolations) {
		this.totalViolations = totalViolations;
	}

	public int getCriticalViolations() {
		return criticalViolations;
	}

	public void setCriticalViolations(int criticalViolations) {
		this.criticalViolations = criticalViolations;
	}

	public int getSeriousViolations() {
		return seriousViolations;
	}

	public void setSeriousViolations(int seriousViolations) {
		this.seriousViolations = seriousViolations;
	}

	public int getModerateViolations() {
		return moderateViolations;
	}

	public void setModerateViolations(int moderateViolations) {
		this.moderateViolations = moderateViolations;
	}

	public int getMinorViolations() {
		return minorViolations;
	}

	public void setMinorViolations(int minorViolations) {
		this.minorViolations = minorViolations;
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

	public Instant getLastScanTime() {
		return lastScanTime;
	}

	public void setLastScanTime(Instant lastScanTime) {
		this.lastScanTime = lastScanTime;
	}

	public String getLastScanSource() {
		return lastScanSource;
	}

	public void setLastScanSource(String lastScanSource) {
		this.lastScanSource = lastScanSource;
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

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final AccessibilityOverview instance = new AccessibilityOverview();

		public Builder overallGrade(String overallGrade) {
			instance.setOverallGrade(overallGrade);
			return this;
		}

		public Builder wcagCompliance(double wcagCompliance) {
			instance.setWcagCompliance(wcagCompliance);
			return this;
		}

		public Builder totalViolations(int totalViolations) {
			instance.setTotalViolations(totalViolations);
			return this;
		}

		public Builder criticalViolations(int criticalViolations) {
			instance.setCriticalViolations(criticalViolations);
			return this;
		}

		public Builder seriousViolations(int seriousViolations) {
			instance.setSeriousViolations(seriousViolations);
			return this;
		}

		public Builder moderateViolations(int moderateViolations) {
			instance.setModerateViolations(moderateViolations);
			return this;
		}

		public Builder minorViolations(int minorViolations) {
			instance.setMinorViolations(minorViolations);
			return this;
		}

		public Builder lighthouseAccessibility(int score) {
			instance.setLighthouseAccessibility(score);
			return this;
		}

		public Builder lighthousePerformance(int score) {
			instance.setLighthousePerformance(score);
			return this;
		}

		public Builder lighthouseSeo(int score) {
			instance.setLighthouseSeo(score);
			return this;
		}

		public Builder lighthouseBestPractices(int score) {
			instance.setLighthouseBestPractices(score);
			return this;
		}

		public Builder lastScanTime(Instant lastScanTime) {
			instance.setLastScanTime(lastScanTime);
			return this;
		}

		public Builder lastScanSource(String lastScanSource) {
			instance.setLastScanSource(lastScanSource);
			return this;
		}

		public Builder appId(String appId) {
			instance.setAppId(appId);
			return this;
		}

		public Builder appName(String appName) {
			instance.setAppName(appName);
			return this;
		}

		public AccessibilityOverview build() {
			return instance;
		}

	}

}
