package io.strategiz.service.console.accessibility.model;

import java.time.Instant;

/**
 * Lighthouse audit scores for a target URL.
 */
public class LighthouseResult {

	private String targetUrl;

	private Instant scanTime;

	private int accessibilityScore;

	private int performanceScore;

	private int seoScore;

	private int bestPracticesScore;

	private String appId;

	private String appName;

	public LighthouseResult() {
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public Instant getScanTime() {
		return scanTime;
	}

	public void setScanTime(Instant scanTime) {
		this.scanTime = scanTime;
	}

	public int getAccessibilityScore() {
		return accessibilityScore;
	}

	public void setAccessibilityScore(int accessibilityScore) {
		this.accessibilityScore = accessibilityScore;
	}

	public int getPerformanceScore() {
		return performanceScore;
	}

	public void setPerformanceScore(int performanceScore) {
		this.performanceScore = performanceScore;
	}

	public int getSeoScore() {
		return seoScore;
	}

	public void setSeoScore(int seoScore) {
		this.seoScore = seoScore;
	}

	public int getBestPracticesScore() {
		return bestPracticesScore;
	}

	public void setBestPracticesScore(int bestPracticesScore) {
		this.bestPracticesScore = bestPracticesScore;
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

}
