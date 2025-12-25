package io.strategiz.client.execution.model;

import java.util.List;

public class ValidationResponse {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> suggestions;

    public ValidationResponse() {}

    public ValidationResponse(boolean valid, List<String> errors, List<String> warnings, List<String> suggestions) {
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
        this.suggestions = suggestions;
    }

    public static ValidationResponseBuilder builder() {
        return new ValidationResponseBuilder();
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public static class ValidationResponseBuilder {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;

        public ValidationResponseBuilder valid(boolean valid) { this.valid = valid; return this; }
        public ValidationResponseBuilder errors(List<String> errors) { this.errors = errors; return this; }
        public ValidationResponseBuilder warnings(List<String> warnings) { this.warnings = warnings; return this; }
        public ValidationResponseBuilder suggestions(List<String> suggestions) { this.suggestions = suggestions; return this; }
        public ValidationResponse build() {
            return new ValidationResponse(valid, errors, warnings, suggestions);
        }
    }
}
