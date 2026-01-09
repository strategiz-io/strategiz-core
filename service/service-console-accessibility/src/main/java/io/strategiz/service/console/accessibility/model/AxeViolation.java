package io.strategiz.service.console.accessibility.model;

import java.util.List;

/**
 * Individual accessibility violation from axe-core.
 */
public class AxeViolation {

	private String ruleId;

	private String description;

	private String impact; // critical, serious, moderate, minor

	private String wcagCriteria;

	private List<String> tags;

	private String targetUrl;

	private String targetSelector;

	private String htmlSnippet;

	private String helpUrl;

	private int nodeCount;

	public AxeViolation() {
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

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
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
