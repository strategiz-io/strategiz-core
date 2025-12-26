package io.strategiz.service.console.quality.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.strategiz.client.sonarqube.SonarQubeClient;
import io.strategiz.client.sonarqube.model.SonarQubeMetrics;
import io.strategiz.data.quality.entity.CachedQualityMetricsEntity;
import io.strategiz.data.quality.repository.CachedQualityMetricsRepository;
import io.strategiz.service.console.quality.model.CachedQualityMetrics;
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

	private static final int CACHE_FRESHNESS_HOURS = 24;

	private final ComplianceScanner complianceScanner;

	private final SonarQubeClient sonarQubeClient;

	private final CachedQualityMetricsRepository cacheRepository;

	@Autowired
	public QualityMetricsService(ComplianceScanner complianceScanner,
			@Autowired(required = false) SonarQubeClient sonarQubeClient,
			CachedQualityMetricsRepository cacheRepository) {
		this.complianceScanner = complianceScanner;
		this.sonarQubeClient = sonarQubeClient;
		this.cacheRepository = cacheRepository;
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
	 * Get SonarQube metrics from cache (preferred) or live API.
	 * @return SonarQube metrics including bugs, vulnerabilities, code smells
	 */
	public SonarQubeMetrics getSonarQubeMetrics() {
		log.info("Getting SonarQube metrics...");

		// Try cache first
		Optional<CachedQualityMetricsEntity> cached = cacheRepository.getLatest();
		if (cached.isPresent() && isCacheFresh(cached.get())) {
			log.info("Using cached quality metrics from {}", cached.get().getAnalyzedAt());
			return convertToSonarQubeMetrics(cached.get());
		}

		// Fallback to live SonarQube API
		if (sonarQubeClient != null) {
			log.info("Cache miss or stale - fetching from SonarQube API");
			return sonarQubeClient.getProjectMetrics();
		}

		// No cache and no SonarQube - return empty
		log.warn("No cached metrics and SonarQubeClient not available - returning empty metrics");
		return new SonarQubeMetrics(0, 0, 0, 0.0, 0.0, "0h", "N/A");
	}

	/**
	 * Cache analysis results from build pipeline.
	 * @param metrics the analysis results to cache
	 */
	public void cacheAnalysisResults(CachedQualityMetrics metrics) {
		log.info("Caching analysis results: source={}, commit={}", metrics.getAnalysisSource(),
				metrics.getGitCommitHash());

		CachedQualityMetricsEntity entity = convertToEntity(metrics);
		cacheRepository.save(entity);

		log.info("Analysis results cached successfully");
	}

	/**
	 * Get the latest cached quality metrics.
	 * @return latest cached metrics if available
	 */
	public Optional<CachedQualityMetrics> getLatestCachedMetrics() {
		return cacheRepository.getLatest().map(this::convertToModel);
	}

	/**
	 * Check if cached metrics are fresh (less than 24 hours old).
	 */
	private boolean isCacheFresh(CachedQualityMetricsEntity entity) {
		if (entity.getAnalyzedAt() == null) {
			return false;
		}

		Instant cutoff = Instant.now().minus(CACHE_FRESHNESS_HOURS, ChronoUnit.HOURS);
		return entity.getAnalyzedAt().isAfter(cutoff);
	}

	/**
	 * Convert cached entity to SonarQube metrics format.
	 */
	private SonarQubeMetrics convertToSonarQubeMetrics(CachedQualityMetricsEntity entity) {
		return new SonarQubeMetrics(entity.getBugs(), entity.getVulnerabilities(), entity.getCodeSmells(),
				entity.getCoverage(), entity.getDuplications(), entity.getTechnicalDebt(),
				entity.getReliabilityRating());
	}

	/**
	 * Convert model to entity for storage.
	 */
	private CachedQualityMetricsEntity convertToEntity(CachedQualityMetrics model) {
		CachedQualityMetricsEntity entity = new CachedQualityMetricsEntity();
		entity.setAnalysisId(model.getAnalysisId());
		entity.setAnalyzedAt(model.getAnalyzedAt());
		entity.setGitCommitHash(model.getGitCommitHash());
		entity.setGitBranch(model.getGitBranch());
		entity.setBugs(model.getBugs());
		entity.setVulnerabilities(model.getVulnerabilities());
		entity.setCodeSmells(model.getCodeSmells());
		entity.setCoverage(model.getCoverage());
		entity.setDuplications(model.getDuplications());
		entity.setTechnicalDebt(model.getTechnicalDebt());
		entity.setReliabilityRating(model.getReliabilityRating());
		entity.setSecurityRating(model.getSecurityRating());
		entity.setMaintainabilityRating(model.getMaintainabilityRating());
		entity.setQualityGateStatus(model.getQualityGateStatus());
		entity.setTotalIssues(model.getTotalIssues());
		entity.setNewIssues(model.getNewIssues());
		entity.setAnalysisSource(model.getAnalysisSource());
		entity.setBuildNumber(model.getBuildNumber());
		return entity;
	}

	/**
	 * Convert entity to model for API response.
	 */
	private CachedQualityMetrics convertToModel(CachedQualityMetricsEntity entity) {
		return CachedQualityMetrics.builder()
			.analysisId(entity.getAnalysisId())
			.analyzedAt(entity.getAnalyzedAt())
			.gitCommitHash(entity.getGitCommitHash())
			.gitBranch(entity.getGitBranch())
			.bugs(entity.getBugs())
			.vulnerabilities(entity.getVulnerabilities())
			.codeSmells(entity.getCodeSmells())
			.coverage(entity.getCoverage())
			.duplications(entity.getDuplications())
			.technicalDebt(entity.getTechnicalDebt())
			.reliabilityRating(entity.getReliabilityRating())
			.securityRating(entity.getSecurityRating())
			.maintainabilityRating(entity.getMaintainabilityRating())
			.qualityGateStatus(entity.getQualityGateStatus())
			.totalIssues(entity.getTotalIssues())
			.newIssues(entity.getNewIssues())
			.analysisSource(entity.getAnalysisSource())
			.buildNumber(entity.getBuildNumber())
			.build();
	}

}
