package io.strategiz.service.console.service.tests.model;

import java.util.List;
import java.util.Map;

/**
 * Request model for test execution
 */
public class TestRunRequest {

	private String environment;

	private Map<String, String> parameters;

	private List<String> tags;

	private Integer timeout;

	public TestRunRequest() {
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

}
