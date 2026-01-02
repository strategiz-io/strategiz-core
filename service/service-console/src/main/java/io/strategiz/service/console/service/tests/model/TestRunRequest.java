package io.strategiz.service.console.service.tests.model;

import io.strategiz.data.testing.entity.TestTrigger;

import java.util.Map;

/**
 * Request model for test execution.
 */
public class TestRunRequest {

    private TestTrigger trigger;
    private String environment;
    private Map<String, String> parameters;
    private String[] tags;
    private Integer timeoutMinutes;

    public TestRunRequest() {
    }

    public TestTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(TestTrigger trigger) {
        this.trigger = trigger;
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

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }
}
