package io.strategiz.client.execution.model;

import java.util.List;

public class ExecutionResponse {
    private boolean success;
    private List<Signal> signals;
    private List<Indicator> indicators;
    private Performance performance;
    private int executionTimeMs;
    private List<String> logs;
    private String error;

    // Builder pattern
    public static ExecutionResponseBuilder builder() {
        return new ExecutionResponseBuilder();
    }

    public static class ExecutionResponseBuilder {
        private boolean success;
        private List<Signal> signals;
        private List<Indicator> indicators;
        private Performance performance;
        private int executionTimeMs;
        private List<String> logs;
        private String error;

        public ExecutionResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public ExecutionResponseBuilder signals(List<Signal> signals) {
            this.signals = signals;
            return this;
        }

        public ExecutionResponseBuilder indicators(List<Indicator> indicators) {
            this.indicators = indicators;
            return this;
        }

        public ExecutionResponseBuilder performance(Performance performance) {
            this.performance = performance;
            return this;
        }

        public ExecutionResponseBuilder executionTimeMs(int executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public ExecutionResponseBuilder logs(List<String> logs) {
            this.logs = logs;
            return this;
        }

        public ExecutionResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ExecutionResponse build() {
            ExecutionResponse response = new ExecutionResponse();
            response.success = this.success;
            response.signals = this.signals;
            response.indicators = this.indicators;
            response.performance = this.performance;
            response.executionTimeMs = this.executionTimeMs;
            response.logs = this.logs;
            response.error = this.error;
            return response;
        }
    }

    // Getters
    public boolean isSuccess() { return success; }
    public List<Signal> getSignals() { return signals; }
    public List<Indicator> getIndicators() { return indicators; }
    public Performance getPerformance() { return performance; }
    public int getExecutionTimeMs() { return executionTimeMs; }
    public List<String> getLogs() { return logs; }
    public String getError() { return error; }
}
