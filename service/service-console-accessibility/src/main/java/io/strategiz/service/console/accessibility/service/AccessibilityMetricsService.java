package io.strategiz.service.console.accessibility.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.strategiz.data.accessibility.entity.CachedAccessibilityMetricsEntity;
import io.strategiz.data.accessibility.repository.CachedAccessibilityMetricsRepository;
import io.strategiz.service.console.accessibility.model.AccessibilityOverview;
import io.strategiz.service.console.accessibility.model.AxeViolation;
import io.strategiz.service.console.accessibility.model.AxeViolationList;
import io.strategiz.service.console.accessibility.model.CachedAccessibilityMetrics;
import io.strategiz.service.console.accessibility.model.LighthouseResult;
import io.strategiz.service.console.accessibility.model.OnDemandScanRequest;
import io.strategiz.service.console.accessibility.model.OnDemandScanResponse;
import io.strategiz.service.console.accessibility.model.ScanTarget;

/**
 * Service for managing accessibility metrics. Retrieves cached metrics from Firestore and
 * provides data for the admin console dashboard.
 */
@Service
public class AccessibilityMetricsService {

	private static final Logger log = LoggerFactory.getLogger(AccessibilityMetricsService.class);

	private static final String GITHUB_API_URL = "https://api.github.com";

	private static final String REPO_OWNER = "strategiz-io";

	private static final String REPO_NAME = "strategiz-ui";

	private static final String WORKFLOW_ID = "accessibility-scan.yml";

	private final CachedAccessibilityMetricsRepository cacheRepository;

	private final RestTemplate restTemplate;

	private final GitHubAppAuthService githubAuthService;

	// In-memory cache for scan status (in production, use Redis or Firestore)
	private final Map<String, OnDemandScanResponse> scanStatusCache = new ConcurrentHashMap<>();

	// Legacy support for simple token auth (fallback)
	@Value("${github.token:}")
	private String githubToken;

	@Autowired
	public AccessibilityMetricsService(CachedAccessibilityMetricsRepository cacheRepository,
			GitHubAppAuthService githubAuthService) {
		this.cacheRepository = cacheRepository;
		this.restTemplate = new RestTemplate();
		this.githubAuthService = githubAuthService;
	}

	/**
	 * Get accessibility overview for the dashboard.
	 * @param appId optional filter by app, null for latest across all apps
	 * @return accessibility overview with aggregated metrics
	 */
	public AccessibilityOverview getAccessibilityOverview(String appId) {
		log.info("Getting accessibility overview for appId: {}", appId != null ? appId : "all");

		Optional<CachedAccessibilityMetricsEntity> cached = (appId != null) ? cacheRepository.getLatestByApp(appId)
				: cacheRepository.getLatest();

		if (cached.isEmpty()) {
			log.warn("No cached accessibility metrics found");
			return createEmptyOverview();
		}

		CachedAccessibilityMetricsEntity entity = cached.get();
		return AccessibilityOverview.builder()
			.overallGrade(entity.getOverallGrade())
			.wcagCompliance(entity.getWcagCompliance())
			.totalViolations(entity.getTotalViolations())
			.criticalViolations(entity.getCriticalCount())
			.seriousViolations(entity.getSeriousCount())
			.moderateViolations(entity.getModerateCount())
			.minorViolations(entity.getMinorCount())
			.lighthouseAccessibility(entity.getLighthouseAccessibility())
			.lighthousePerformance(entity.getLighthousePerformance())
			.lighthouseSeo(entity.getLighthouseSeo())
			.lighthouseBestPractices(entity.getLighthouseBestPractices())
			.lastScanTime(entity.getScannedAt())
			.lastScanSource(entity.getScanSource())
			.appId(entity.getAppId())
			.appName(entity.getAppName())
			.build();
	}

	/**
	 * Get paginated list of axe-core violations.
	 * @param limit maximum number of violations to return
	 * @param severity optional filter by impact level
	 * @param appId optional filter by app
	 * @return list of violations
	 */
	public AxeViolationList getViolations(int limit, String severity, String appId) {
		log.info("Getting violations: limit={}, severity={}, appId={}", limit, severity, appId);

		Optional<CachedAccessibilityMetricsEntity> cached = (appId != null) ? cacheRepository.getLatestByApp(appId)
				: cacheRepository.getLatest();

		if (cached.isEmpty() || cached.get().getViolations() == null) {
			return new AxeViolationList(List.of(), 0);
		}

		List<CachedAccessibilityMetricsEntity.AxeViolationData> violations = cached.get().getViolations();

		// Filter by severity if specified
		if (severity != null && !severity.isEmpty()) {
			violations = violations.stream()
				.filter(v -> severity.equalsIgnoreCase(v.getImpact()))
				.collect(Collectors.toList());
		}

		int total = violations.size();

		// Apply limit
		List<AxeViolation> result = violations.stream().limit(limit).map(this::convertViolation).toList();

		return new AxeViolationList(result, total);
	}

	/**
	 * Get Lighthouse scores for a target.
	 * @param appId optional filter by app
	 * @return Lighthouse result with scores
	 */
	public LighthouseResult getLighthouseScores(String appId) {
		log.info("Getting Lighthouse scores for appId: {}", appId != null ? appId : "all");

		Optional<CachedAccessibilityMetricsEntity> cached = (appId != null) ? cacheRepository.getLatestByApp(appId)
				: cacheRepository.getLatest();

		if (cached.isEmpty()) {
			return createEmptyLighthouseResult();
		}

		CachedAccessibilityMetricsEntity entity = cached.get();
		LighthouseResult result = new LighthouseResult();
		result.setTargetUrl(entity.getTargetUrl());
		result.setScanTime(entity.getScannedAt());
		result.setAccessibilityScore(entity.getLighthouseAccessibility());
		result.setPerformanceScore(entity.getLighthousePerformance());
		result.setSeoScore(entity.getLighthouseSeo());
		result.setBestPracticesScore(entity.getLighthouseBestPractices());
		result.setAppId(entity.getAppId());
		result.setAppName(entity.getAppName());

		return result;
	}

	/**
	 * Get historical scan results.
	 * @param limit maximum number of results
	 * @param appId optional filter by app
	 * @return list of cached metrics
	 */
	public List<CachedAccessibilityMetrics> getHistory(int limit, String appId) {
		log.info("Getting scan history: limit={}, appId={}", limit, appId);

		List<CachedAccessibilityMetricsEntity> history = (appId != null) ? cacheRepository.getHistoryByApp(appId, limit)
				: cacheRepository.getHistory(limit);

		return history.stream().map(this::convertToModel).toList();
	}

	/**
	 * Cache accessibility analysis results from CI/CD.
	 * @param metrics the metrics to cache
	 */
	public void cacheAnalysisResults(CachedAccessibilityMetrics metrics) {
		log.info("Caching accessibility analysis results: scanId={}, appId={}, source={}", metrics.getScanId(),
				metrics.getAppId(), metrics.getScanSource());

		CachedAccessibilityMetricsEntity entity = convertToEntity(metrics);
		cacheRepository.save(entity);

		log.info("Accessibility analysis results cached successfully");
	}

	/**
	 * Get available scan targets.
	 * @return list of scannable targets
	 */
	public List<ScanTarget> getScanTargets() {
		List<ScanTarget> targets = new ArrayList<>();

		// Web app targets
		targets.add(new ScanTarget("web-home", "Web App - Home", "https://strategiz.io", "Main landing page", "web",
				"Strategiz Web"));
		targets.add(new ScanTarget("web-dashboard", "Web App - Dashboard", "https://strategiz.io/dashboard",
				"User dashboard", "web", "Strategiz Web"));
		targets.add(new ScanTarget("web-portfolio", "Web App - Portfolio", "https://strategiz.io/portfolio",
				"Portfolio management", "web", "Strategiz Web"));

		// Auth portal targets
		targets.add(new ScanTarget("auth-signin", "Auth - Sign In", "https://auth.strategiz.io/signin", "Sign in page",
				"auth", "Auth Portal"));
		targets.add(new ScanTarget("auth-signup", "Auth - Sign Up", "https://auth.strategiz.io/signup", "Sign up page",
				"auth", "Auth Portal"));

		// Console targets
		targets.add(new ScanTarget("console-home", "Console - Dashboard", "https://console.strategiz.io",
				"Admin console dashboard", "console", "Admin Console"));

		return targets;
	}

	private AccessibilityOverview createEmptyOverview() {
		return AccessibilityOverview.builder()
			.overallGrade("N/A")
			.wcagCompliance(0)
			.totalViolations(0)
			.criticalViolations(0)
			.seriousViolations(0)
			.moderateViolations(0)
			.minorViolations(0)
			.lighthouseAccessibility(0)
			.lighthousePerformance(0)
			.lighthouseSeo(0)
			.lighthouseBestPractices(0)
			.lastScanSource("No scans yet")
			.build();
	}

	private LighthouseResult createEmptyLighthouseResult() {
		LighthouseResult result = new LighthouseResult();
		result.setAccessibilityScore(0);
		result.setPerformanceScore(0);
		result.setSeoScore(0);
		result.setBestPracticesScore(0);
		return result;
	}

	private AxeViolation convertViolation(CachedAccessibilityMetricsEntity.AxeViolationData data) {
		AxeViolation violation = new AxeViolation();
		violation.setRuleId(data.getRuleId());
		violation.setDescription(data.getDescription());
		violation.setImpact(data.getImpact());
		violation.setWcagCriteria(data.getWcagCriteria());
		violation.setTags(data.getTags());
		violation.setTargetSelector(data.getTargetSelector());
		violation.setHtmlSnippet(data.getHtmlSnippet());
		violation.setHelpUrl(data.getHelpUrl());
		violation.setNodeCount(data.getNodeCount());
		return violation;
	}

	private CachedAccessibilityMetrics convertToModel(CachedAccessibilityMetricsEntity entity) {
		CachedAccessibilityMetrics model = new CachedAccessibilityMetrics();
		model.setScanId(entity.getScanId());
		model.setScannedAt(entity.getScannedAt());
		model.setGitCommitHash(entity.getGitCommitHash());
		model.setGitBranch(entity.getGitBranch());
		model.setBuildNumber(entity.getBuildNumber());
		model.setScanSource(entity.getScanSource());
		model.setAppId(entity.getAppId());
		model.setAppName(entity.getAppName());
		model.setTargetUrl(entity.getTargetUrl());
		model.setTotalViolations(entity.getTotalViolations());
		model.setCriticalCount(entity.getCriticalCount());
		model.setSeriousCount(entity.getSeriousCount());
		model.setModerateCount(entity.getModerateCount());
		model.setMinorCount(entity.getMinorCount());
		model.setWcagCompliance(entity.getWcagCompliance());
		model.setOverallGrade(entity.getOverallGrade());
		model.setLighthouseAccessibility(entity.getLighthouseAccessibility());
		model.setLighthousePerformance(entity.getLighthousePerformance());
		model.setLighthouseSeo(entity.getLighthouseSeo());
		model.setLighthouseBestPractices(entity.getLighthouseBestPractices());
		model.setViolationsByWcag(entity.getViolationsByWcag());

		if (entity.getViolations() != null) {
			model.setViolations(entity.getViolations().stream().map(this::convertViolation).toList());
		}

		return model;
	}

	private CachedAccessibilityMetricsEntity convertToEntity(CachedAccessibilityMetrics model) {
		CachedAccessibilityMetricsEntity entity = new CachedAccessibilityMetricsEntity();
		entity.setScanId(model.getScanId());
		entity.setScannedAt(model.getScannedAt());
		entity.setGitCommitHash(model.getGitCommitHash());
		entity.setGitBranch(model.getGitBranch());
		entity.setBuildNumber(model.getBuildNumber());
		entity.setScanSource(model.getScanSource());
		entity.setAppId(model.getAppId());
		entity.setAppName(model.getAppName());
		entity.setTargetUrl(model.getTargetUrl());
		entity.setTotalViolations(model.getTotalViolations());
		entity.setCriticalCount(model.getCriticalCount());
		entity.setSeriousCount(model.getSeriousCount());
		entity.setModerateCount(model.getModerateCount());
		entity.setMinorCount(model.getMinorCount());
		entity.setWcagCompliance(model.getWcagCompliance());
		entity.setOverallGrade(model.getOverallGrade());
		entity.setLighthouseAccessibility(model.getLighthouseAccessibility());
		entity.setLighthousePerformance(model.getLighthousePerformance());
		entity.setLighthouseSeo(model.getLighthouseSeo());
		entity.setLighthouseBestPractices(model.getLighthouseBestPractices());
		entity.setViolationsByWcag(model.getViolationsByWcag());

		if (model.getViolations() != null) {
			entity.setViolations(model.getViolations().stream().map(this::convertToViolationData).toList());
		}

		return entity;
	}

	private CachedAccessibilityMetricsEntity.AxeViolationData convertToViolationData(AxeViolation violation) {
		CachedAccessibilityMetricsEntity.AxeViolationData data = new CachedAccessibilityMetricsEntity.AxeViolationData();
		data.setRuleId(violation.getRuleId());
		data.setDescription(violation.getDescription());
		data.setImpact(violation.getImpact());
		data.setWcagCriteria(violation.getWcagCriteria());
		data.setTags(violation.getTags());
		data.setTargetSelector(violation.getTargetSelector());
		data.setHtmlSnippet(violation.getHtmlSnippet());
		data.setHelpUrl(violation.getHelpUrl());
		data.setNodeCount(violation.getNodeCount());
		return data;
	}

	/**
	 * Trigger an on-demand accessibility scan via GitHub Actions.
	 * @param request the scan request with target apps
	 * @return response with scan ID for status tracking
	 */
	public OnDemandScanResponse triggerOnDemandScan(OnDemandScanRequest request) {
		String scanId = UUID.randomUUID().toString();
		log.info("Triggering on-demand accessibility scan: scanId={}, targets={}", scanId, request.getTargetIds());

		OnDemandScanResponse response = new OnDemandScanResponse(scanId, "PENDING", 0);
		response.setStartedAt(Instant.now());

		// Store initial status
		scanStatusCache.put(scanId, response);

		// Get authentication token (GitHub App or legacy token)
		String authToken = getAuthToken();
		if (authToken == null) {
			log.warn("GitHub authentication not configured");
			response.setStatus("FAILED");
			response.setError("GitHub integration not configured");
			return response;
		}

		try {
			// Determine app parameter
			String appParam = "all";
			if (request.getTargetIds() != null && !request.getTargetIds().isEmpty()) {
				// Extract app from target IDs (e.g., "web-home" -> "web")
				String firstTarget = request.getTargetIds().get(0);
				if (firstTarget.contains("-")) {
					appParam = firstTarget.split("-")[0];
				}
			}

			// Trigger GitHub Actions workflow
			String url = String.format("%s/repos/%s/%s/actions/workflows/%s/dispatches", GITHUB_API_URL, REPO_OWNER,
					REPO_NAME, WORKFLOW_ID);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + authToken);
			headers.set("Accept", "application/vnd.github.v3+json");
			headers.set("X-GitHub-Api-Version", "2022-11-28");

			String body = String.format("{\"ref\":\"main\",\"inputs\":{\"app\":\"%s\"}}", appParam);

			HttpEntity<String> entity = new HttpEntity<>(body, headers);
			restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

			response.setStatus("RUNNING");
			response.setProgress(10);
			log.info("GitHub Actions workflow triggered successfully for scan: {}", scanId);

		}
		catch (Exception e) {
			log.error("Failed to trigger GitHub Actions workflow: {}", e.getMessage());
			response.setStatus("FAILED");
			response.setError("Failed to trigger scan: " + e.getMessage());
		}

		scanStatusCache.put(scanId, response);
		return response;
	}

	/**
	 * Get the status of an on-demand scan.
	 * @param scanId the scan ID
	 * @return current scan status
	 */
	public OnDemandScanResponse getScanStatus(String scanId) {
		OnDemandScanResponse status = scanStatusCache.get(scanId);
		if (status == null) {
			status = new OnDemandScanResponse(scanId, "NOT_FOUND", 0);
			status.setError("Scan not found");
		}
		return status;
	}

	/**
	 * Update scan status (called when CI/CD pipeline reports progress).
	 * @param scanId the scan ID
	 * @param status new status
	 * @param progress progress percentage
	 */
	public void updateScanStatus(String scanId, String status, int progress) {
		OnDemandScanResponse response = scanStatusCache.get(scanId);
		if (response != null) {
			response.setStatus(status);
			response.setProgress(progress);
			if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
				response.setCompletedAt(Instant.now());
			}
			scanStatusCache.put(scanId, response);
		}
	}

	/**
	 * Get GitHub authentication token. Tries GitHub App first (preferred), falls back to
	 * simple bearer token.
	 * @return GitHub authentication token, or null if not configured
	 */
	private String getAuthToken() {
		// Try GitHub App authentication first (preferred method)
		if (githubAuthService.isConfigured()) {
			try {
				log.info("Using GitHub App authentication");
				return githubAuthService.getInstallationToken();
			}
			catch (Exception e) {
				log.error("GitHub App authentication failed, falling back to simple token: {}", e.getMessage());
			}
		}

		// Fall back to simple bearer token
		if (githubToken != null && !githubToken.isEmpty()) {
			log.info("Using legacy token authentication");
			return githubToken;
		}

		return null;
	}

}
