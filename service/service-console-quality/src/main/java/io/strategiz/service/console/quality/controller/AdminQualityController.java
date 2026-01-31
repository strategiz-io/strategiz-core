package io.strategiz.service.console.quality.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.client.sonarqube.model.SonarQubeMetrics;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.quality.model.CachedQualityMetrics;
import io.strategiz.service.console.quality.model.ComplianceBreakdown;
import io.strategiz.service.console.quality.model.QualityOverview;
import io.strategiz.service.console.quality.model.ViolationList;
import io.strategiz.service.console.quality.service.QualityMetricsService;

/**
 * REST controller for quality metrics and compliance data. Provides endpoints for admin
 * console quality dashboard at console.strategiz.io/quality.
 *
 * All endpoints require admin authentication.
 */
@RestController
@RequestMapping("/v1/console/quality")
public class AdminQualityController extends BaseController {

	private final QualityMetricsService qualityMetricsService;

	@Autowired
	public AdminQualityController(QualityMetricsService qualityMetricsService) {
		this.qualityMetricsService = qualityMetricsService;
	}

	/**
	 * Get overall quality overview for dashboard.
	 * @return quality overview with compliance score, SonarQube metrics, etc.
	 */
	@GetMapping("/overview")
	public ResponseEntity<QualityOverview> getOverview() {
		// TODO: Add @RequireAuth annotation once authentication is integrated
		QualityOverview overview = qualityMetricsService.getQualityOverview();
		return ResponseEntity.ok(overview);
	}

	/**
	 * Get compliance breakdown by framework pattern.
	 * @return compliance metrics for exception handling, service pattern, controller
	 * pattern
	 */
	@GetMapping("/compliance")
	public ResponseEntity<ComplianceBreakdown> getCompliance() {
		// TODO: Add @RequireAuth annotation once authentication is integrated
		ComplianceBreakdown compliance = qualityMetricsService.getComplianceBreakdown();
		return ResponseEntity.ok(compliance);
	}

	/**
	 * Get top compliance violations with file paths.
	 * @param limit maximum number of violations to return (default 20)
	 * @return list of violations with file paths and line numbers
	 */
	@GetMapping("/violations")
	public ResponseEntity<ViolationList> getViolations(@RequestParam(defaultValue = "20") int limit) {
		// TODO: Add @RequireAuth annotation once authentication is integrated
		ViolationList violations = qualityMetricsService.getTopViolations(limit);
		return ResponseEntity.ok(violations);
	}

	/**
	 * Get SonarQube metrics (proxied from SonarQube API or cached).
	 * @return SonarQube metrics including bugs, vulnerabilities, code smells
	 */
	@GetMapping("/sonarqube")
	public ResponseEntity<SonarQubeMetrics> getSonarQubeMetrics() {
		// TODO: Add @RequireAuth annotation once authentication is integrated
		SonarQubeMetrics metrics = qualityMetricsService.getSonarQubeMetrics();
		return ResponseEntity.ok(metrics);
	}

	/**
	 * Cache quality analysis results from build pipeline (GitHub Actions, Cloud Build,
	 * etc.). This endpoint is called by CI/CD workflows after running analysis tools.
	 *
	 * Authentication: Requires Bearer token from Vault (secret/strategiz/ci-cd) Validated
	 * by CiCdAuthFilter.
	 * @param metrics the analysis results to cache
	 * @return 200 OK if cached successfully
	 */
	@PostMapping("/cache")
	public ResponseEntity<Void> cacheAnalysisResults(@RequestBody CachedQualityMetrics metrics) {
		qualityMetricsService.cacheAnalysisResults(metrics);
		return ResponseEntity.ok().build();
	}

	/**
	 * Get the latest cached quality analysis results.
	 * @return cached metrics if available
	 */
	@GetMapping("/cache/latest")
	public ResponseEntity<CachedQualityMetrics> getLatestCachedMetrics() {
		// TODO: Add @RequireAuth annotation once authentication is integrated
		return qualityMetricsService.getLatestCachedMetrics()
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@Override
	protected String getModuleName() {
		return "service-console-quality";
	}

}
