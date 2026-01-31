package io.strategiz.service.console.quality.model;

/**
 * Breakdown of compliance metrics by framework pattern.
 */
public class ComplianceBreakdown {

	private ComplianceMetric exceptionHandling;

	private ComplianceMetric servicePattern;

	private ComplianceMetric controllerPattern;

	public ComplianceBreakdown() {
	}

	public ComplianceBreakdown(ComplianceMetric exceptionHandling, ComplianceMetric servicePattern,
			ComplianceMetric controllerPattern) {
		this.exceptionHandling = exceptionHandling;
		this.servicePattern = servicePattern;
		this.controllerPattern = controllerPattern;
	}

	public ComplianceMetric getExceptionHandling() {
		return exceptionHandling;
	}

	public void setExceptionHandling(ComplianceMetric exceptionHandling) {
		this.exceptionHandling = exceptionHandling;
	}

	public ComplianceMetric getServicePattern() {
		return servicePattern;
	}

	public void setServicePattern(ComplianceMetric servicePattern) {
		this.servicePattern = servicePattern;
	}

	public ComplianceMetric getControllerPattern() {
		return controllerPattern;
	}

	public void setControllerPattern(ComplianceMetric controllerPattern) {
		this.controllerPattern = controllerPattern;
	}

	/**
	 * Calculate overall compliance score (weighted average).
	 * @return overall compliance percentage (0-100)
	 */
	public double getOverallCompliance() {
		// Weight: exception handling 50%, service 30%, controller 20%
		double exceptionWeight = 0.5;
		double serviceWeight = 0.3;
		double controllerWeight = 0.2;

		return (exceptionHandling.getCompliance() * exceptionWeight) + (servicePattern.getCompliance() * serviceWeight)
				+ (controllerPattern.getCompliance() * controllerWeight);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private ComplianceMetric exceptionHandling;

		private ComplianceMetric servicePattern;

		private ComplianceMetric controllerPattern;

		public Builder exceptionHandling(ComplianceMetric exceptionHandling) {
			this.exceptionHandling = exceptionHandling;
			return this;
		}

		public Builder servicePattern(ComplianceMetric servicePattern) {
			this.servicePattern = servicePattern;
			return this;
		}

		public Builder controllerPattern(ComplianceMetric controllerPattern) {
			this.controllerPattern = controllerPattern;
			return this;
		}

		public ComplianceBreakdown build() {
			return new ComplianceBreakdown(exceptionHandling, servicePattern, controllerPattern);
		}

	}

}
