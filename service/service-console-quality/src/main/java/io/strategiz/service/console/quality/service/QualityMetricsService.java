package io.strategiz.service.console.quality.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.strategiz.client.sonarqube.SonarQubeClient;
import io.strategiz.client.sonarqube.model.SonarQubeMetrics;
import io.strategiz.service.console.quality.model.ComplianceBreakdown;
import io.strategiz.service.console.quality.model.ComplianceViolation;
import io.strategiz.service.console.quality.model.QualityGrade;
import io.strategiz.service.console.quality.model.QualityOverview;
import io.strategiz.service.console.quality.model.ViolationList;

/**
 * Service for aggregating quality metrics from multiple sources. Combines
 * framework compliance metrics (ComplianceScanner) with static analysis metrics
 * (SonarQube) to provide overall quality dashboard.
 */
@Service
public class QualityMetricsService {

	private static final Logger log = LoggerFactory.getLogger(QualityMetricsService.class);

	private final ComplianceScanner complianceScanner;

	private final SonarQubeClient sonarQubeClient;

	@Autowired
	public QualityMetricsService(ComplianceScanner complianceScanner,
			@Autowired(required = false) SonarQubeClient sonarQubeClient) {
		this.complianceScanner = complianceScanner;
		this.sonarQubeClient = sonarQubeClient;
	}

	/**
	 * Get overall quality overview for dashboard.
	 * @return quality overview with compliance and SonarQube metrics
	 */
	public QualityOverview getQualityOverview() {
		log.info("Generating quality overview...");

		// Get compliance metrics
		ComplianceBreakdown compliance = complianceScanner.scanCompliance();
		double complianceScore = compliance.getOverallCompliance();
		QualityGrade grade = QualityGrade.fromPercentage(complianceScore);

		// Get SonarQube metrics
		SonarQubeMetrics sonarMetrics = (sonarQubeClient != null) ? sonarQubeClient.getProjectMetrics()
				: new SonarQubeMetrics(0, 0, 0, 0.0, 0.0, "0h", "N/A");

		String sonarQubeRating = sonarMetrics.getRating();
		int bugs = sonarMetrics.getBugs();
		int vulnerabilities = sonarMetrics.getVulnerabilities();
		int codeSmells = sonarMetrics.getCodeSmells();
		String technicalDebt = sonarMetrics.getTechnicalDebt();

		return QualityOverview.builder()
			.overallGrade(grade.getLabel())
			.complianceScore(Math.round(complianceScore * 10.0) / 10.0)
			.sonarQubeRating(sonarQubeRating)
			.bugs(bugs)
			.vulnerabilities(vulnerabilities)
			.codeSmells(codeSmells)
			.technicalDebt(technicalDebt)
			.lastUpdated(Instant.now())
			.build();
	}

	/**
	 * Get compliance breakdown by framework pattern.
	 * @return compliance metrics breakdown
	 */
	public ComplianceBreakdown getComplianceBreakdown() {
		log.info("Getting compliance breakdown...");
		return complianceScanner.scanCompliance();
	}

	/**
	 * Get top compliance violations.
	 * @param limit maximum number of violations to return
	 * @return violation list with file paths and line numbers
	 */
	public ViolationList getTopViolations(int limit) {
		log.info("Getting top {} violations...", limit);

		// Scan all violation types
		List<ComplianceViolation> exceptionViolations = complianceScanner.scanExceptionHandling();
		List<ComplianceViolation> serviceViolations = complianceScanner.scanServicePattern();
		List<ComplianceViolation> controllerViolations = complianceScanner.scanControllerPattern();

		// Combine all violations
		List<ComplianceViolation> allViolations = Stream
			.of(exceptionViolations, serviceViolations, controllerViolations)
			.flatMap(List::stream)
			.collect(Collectors.toList());

		// Sort by severity (HIGH -> MEDIUM -> LOW)
		allViolations.sort((v1, v2) -> v2.getSeverity().compareTo(v1.getSeverity()));

		// Return top N violations
		List<ComplianceViolation> topViolations = allViolations.stream().limit(limit).collect(Collectors.toList());

		ViolationList result = new ViolationList();
		result.setViolations(topViolations);
		result.setTotal(allViolations.size());
		return result;
	}

	/**
	 * Get SonarQube metrics.
	 * @return SonarQube metrics from self-hosted instance
	 */
	public SonarQubeMetrics getSonarQubeMetrics() {
		log.info("Getting SonarQube metrics...");

		if (sonarQubeClient == null) {
			log.warn("SonarQubeClient not available - returning empty metrics");
			return new SonarQubeMetrics(0, 0, 0, 0.0, 0.0, "0h", "N/A");
		}

		return sonarQubeClient.getProjectMetrics();
	}

}
