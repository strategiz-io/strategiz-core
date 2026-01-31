package io.strategiz.service.console.accessibility.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.accessibility.model.AccessibilityOverview;
import io.strategiz.service.console.accessibility.model.AxeViolationList;
import io.strategiz.service.console.accessibility.model.CachedAccessibilityMetrics;
import io.strategiz.service.console.accessibility.model.LighthouseResult;
import io.strategiz.service.console.accessibility.model.ScanTarget;
import io.strategiz.service.console.accessibility.service.AccessibilityMetricsService;

/**
 * REST controller for accessibility metrics and WCAG compliance data. Provides endpoints
 * for admin console accessibility dashboard at console.strategiz.io/accessibility.
 *
 * All endpoints require admin authentication unless otherwise noted.
 */
@RestController
@RequestMapping("/v1/console/accessibility")
public class AdminAccessibilityController extends BaseController {

	private final AccessibilityMetricsService accessibilityMetricsService;

	@Autowired
	public AdminAccessibilityController(AccessibilityMetricsService accessibilityMetricsService) {
		this.accessibilityMetricsService = accessibilityMetricsService;
	}

	/**
	 * Get overall accessibility overview for dashboard. Returns grade, WCAG compliance %,
	 * violations by severity, and Lighthouse scores.
	 * @param appId optional filter by app (web, auth, console). If null, returns latest
	 * across all apps.
	 * @return accessibility overview with aggregated metrics
	 */
	@GetMapping("/overview")
	public ResponseEntity<AccessibilityOverview> getOverview(@RequestParam(required = false) String appId) {
		AccessibilityOverview overview = accessibilityMetricsService.getAccessibilityOverview(appId);
		return ResponseEntity.ok(overview);
	}

	/**
	 * Get axe-core violations with filtering options.
	 * @param limit maximum number of violations to return (default 50)
	 * @param severity optional filter by impact level (critical, serious, moderate,
	 * minor)
	 * @param appId optional filter by app
	 * @return list of violations with total count
	 */
	@GetMapping("/axe/violations")
	public ResponseEntity<AxeViolationList> getViolations(@RequestParam(defaultValue = "50") int limit,
			@RequestParam(required = false) String severity, @RequestParam(required = false) String appId) {
		AxeViolationList violations = accessibilityMetricsService.getViolations(limit, severity, appId);
		return ResponseEntity.ok(violations);
	}

	/**
	 * Get Lighthouse scores for accessibility, performance, SEO, and best practices.
	 * @param appId optional filter by app
	 * @return Lighthouse result with per-category scores
	 */
	@GetMapping("/lighthouse/scores")
	public ResponseEntity<LighthouseResult> getLighthouseScores(@RequestParam(required = false) String appId) {
		LighthouseResult result = accessibilityMetricsService.getLighthouseScores(appId);
		return ResponseEntity.ok(result);
	}

	/**
	 * Cache accessibility analysis results from CI/CD pipeline. This endpoint is called
	 * by GitHub Actions after running axe-core and Lighthouse scans.
	 *
	 * Authentication: Requires Bearer token from Vault (secret/strategiz/ci-cd) Validated
	 * by CiCdAccessibilityAuthFilter.
	 * @param metrics the analysis results to cache
	 * @return 200 OK if cached successfully
	 */
	@PostMapping("/cache")
	public ResponseEntity<Void> cacheAnalysisResults(@RequestBody CachedAccessibilityMetrics metrics) {
		accessibilityMetricsService.cacheAnalysisResults(metrics);
		return ResponseEntity.ok().build();
	}

	/**
	 * Get historical accessibility scan results.
	 * @param limit maximum number of results to return (default 10)
	 * @param appId optional filter by app
	 * @return list of cached metrics ordered by scan time (newest first)
	 */
	@GetMapping("/cache/history")
	public ResponseEntity<List<CachedAccessibilityMetrics>> getHistory(@RequestParam(defaultValue = "10") int limit,
			@RequestParam(required = false) String appId) {
		List<CachedAccessibilityMetrics> history = accessibilityMetricsService.getHistory(limit, appId);
		return ResponseEntity.ok(history);
	}

	/**
	 * Get list of available scan targets. Returns all pages/URLs that can be scanned for
	 * accessibility.
	 * @return list of scan targets with appId and URL
	 */
	@GetMapping("/targets")
	public ResponseEntity<List<ScanTarget>> getScanTargets() {
		List<ScanTarget> targets = accessibilityMetricsService.getScanTargets();
		return ResponseEntity.ok(targets);
	}

	/**
	 * Trigger on-demand accessibility scan via GitHub Actions workflow.
	 * @param request scan configuration with target apps
	 * @return scan response with scan ID for status tracking
	 */
	@PostMapping("/scan/trigger")
	public ResponseEntity<io.strategiz.service.console.accessibility.model.OnDemandScanResponse> triggerScan(
			@RequestBody io.strategiz.service.console.accessibility.model.OnDemandScanRequest request) {
		io.strategiz.service.console.accessibility.model.OnDemandScanResponse response = accessibilityMetricsService
			.triggerOnDemandScan(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get status of an on-demand scan.
	 * @param scanId the scan ID returned from trigger
	 * @return current scan status
	 */
	@GetMapping("/scan/status/{scanId}")
	public ResponseEntity<io.strategiz.service.console.accessibility.model.OnDemandScanResponse> getScanStatus(
			@org.springframework.web.bind.annotation.PathVariable String scanId) {
		io.strategiz.service.console.accessibility.model.OnDemandScanResponse response = accessibilityMetricsService
			.getScanStatus(scanId);
		return ResponseEntity.ok(response);
	}

	@Override
	protected String getModuleName() {
		return "service-console-accessibility";
	}

}
