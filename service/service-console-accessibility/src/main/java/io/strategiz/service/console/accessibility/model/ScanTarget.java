package io.strategiz.service.console.accessibility.model;

/**
 * Scannable URL target for accessibility testing.
 */
public class ScanTarget {

	private String id;

	private String name;

	private String url;

	private String description;

	private String appId;

	private String appName;

	public ScanTarget() {
	}

	public ScanTarget(String id, String name, String url, String description, String appId, String appName) {
		this.id = id;
		this.name = name;
		this.url = url;
		this.description = description;
		this.appId = appId;
		this.appName = appName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
