package io.strategiz.service.marketing.controller;

import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.service.base.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoint for checking feature flags (no authentication required). Only exposes
 * specific flags needed by public pages.
 *
 * Endpoints: - GET /v1/public/flags/pre-launch - Check if pre-launch mode is enabled
 */
@RestController
@RequestMapping("/v1/public/flags")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io",
		"https://www.strategiz.io" }, allowedHeaders = "*")
public class FeatureFlagPublicController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(FeatureFlagPublicController.class);

	@Override
	protected String getModuleName() {
		return "service-marketing";
	}

	@Autowired
	private FeatureFlagService featureFlagService;

	/**
	 * Check if pre-launch mode is enabled. Used by frontend to determine whether to show
	 * pre-launch or normal landing page.
	 */
	@GetMapping("/pre-launch")
	public ResponseEntity<Map<String, Object>> getPreLaunchMode() {
		try {
			boolean enabled = featureFlagService.isPreLaunchMode();
			log.debug("Pre-launch mode check: {}", enabled);

			return ResponseEntity.ok(Map.of("preLaunchMode", enabled, "timestamp", System.currentTimeMillis()));
		}
		catch (Exception e) {
			log.error("Error checking pre-launch mode", e);
			throw handleException(e, "getPreLaunchMode");
		}
	}

}
