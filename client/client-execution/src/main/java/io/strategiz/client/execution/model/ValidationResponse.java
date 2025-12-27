package io.strategiz.client.execution.model;

import java.util.List;

public class ValidationResponse {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> suggestions;

    public static ValidationResponseBuilder builder() { return new ValidationResponseBuilder(); }

    public static class ValidationResponseBuilder {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;

        public ValidationResponseBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public ValidationResponseBuilder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public ValidationResponseBuilder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public ValidationResponseBuilder suggestions(List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public ValidationResponse build() {
            ValidationResponse response = new ValidationResponse();
            response.valid = this.valid;
            response.errors = this.errors;
            response.warnings = this.warnings;
            response.suggestions = this.suggestions;
            return response;
        }
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getSuggestions() { return suggestions; }
}
