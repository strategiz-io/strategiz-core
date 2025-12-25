package io.strategiz.service.console.quality.model;

/**
 * Individual compliance violation with file location.
 */
public class ComplianceViolation {

	private ViolationType type;

	private String file;

	private Integer line;

	private String message;

	private ViolationSeverity severity;

	public ComplianceViolation() {
	}

	public ComplianceViolation(ViolationType type, String file, Integer line, String message,
			ViolationSeverity severity) {
		this.type = type;
		this.file = file;
		this.line = line;
		this.message = message;
		this.severity = severity;
	}

	public ViolationType getType() {
		return type;
	}

	public void setType(ViolationType type) {
		this.type = type;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ViolationSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(ViolationSeverity severity) {
		this.severity = severity;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private ViolationType type;

		private String file;

		private Integer line;

		private String message;

		private ViolationSeverity severity;

		public Builder type(ViolationType type) {
			this.type = type;
			return this;
		}

		public Builder file(String file) {
			this.file = file;
			return this;
		}

		public Builder line(Integer line) {
			this.line = line;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public Builder severity(ViolationSeverity severity) {
			this.severity = severity;
			return this;
		}

		public ComplianceViolation build() {
			return new ComplianceViolation(type, file, line, message, severity);
		}

	}

	public enum ViolationType {

		EXCEPTION_HANDLING, SERVICE_PATTERN, CONTROLLER_PATTERN

	}

	public enum ViolationSeverity {

		HIGH, MEDIUM, LOW

	}

}
