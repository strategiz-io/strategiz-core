package io.strategiz.service.profile.controller;

import io.strategiz.data.preferences.entity.PlatformConfig;
import io.strategiz.data.preferences.repository.PlatformConfigRepository;
import io.strategiz.service.base.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller for public platform configuration. Exposes non-sensitive platform settings
 * to the frontend.
 */
@RestController
@RequestMapping("/v1/config")
public class PlatformConfigController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(PlatformConfigController.class);

	private final PlatformConfigRepository platformConfigRepository;

	public PlatformConfigController(PlatformConfigRepository platformConfigRepository) {
		this.platformConfigRepository = platformConfigRepository;
	}

	@Override
	protected String getModuleName() {
		return "service-profile";
	}

	/**
	 * Get public platform configuration. This endpoint does NOT require authentication.
	 * @return Platform config (public fields only)
	 */
	@GetMapping("/platform")
	public ResponseEntity<Map<String, Object>> getPlatformConfig() {
		logger.debug("Getting public platform config");

		PlatformConfig config = platformConfigRepository.getCurrent();

		// Return only public-facing config values
		return ResponseEntity.ok(Map.of("platformFeePercent", config.getPlatformFeePercent(), "stratTokensPerUsd",
				config.getStratTokensPerUsd(), "minimumPurchaseCents", config.getMinimumPurchaseCents(),
				"minimumTipStrat", config.getMinimumTipStrat(), "minimumWithdrawStrat",
				config.getMinimumWithdrawStrat(), "defaultOwnerPrice", config.getDefaultOwnerPrice()));
	}

	/**
	 * Get just the platform fee (convenience endpoint).
	 * @return Platform fee percentage
	 */
	@GetMapping("/platform-fee")
	public ResponseEntity<Map<String, Object>> getPlatformFee() {
		PlatformConfig config = platformConfigRepository.getCurrent();
		BigDecimal feePercent = config.getPlatformFeePercent();
		BigDecimal ownerPercent = BigDecimal.ONE.subtract(feePercent);

		return ResponseEntity.ok(Map.of("platformFeePercent", feePercent, "ownerPercent", ownerPercent,
				"platformFeeDisplay", feePercent.multiply(new BigDecimal("100")).intValue() + "%"));
	}

	/**
	 * Get STRAT token configuration.
	 * @return Token rates and minimums
	 */
	@GetMapping("/strat-token")
	public ResponseEntity<Map<String, Object>> getStratTokenConfig() {
		PlatformConfig config = platformConfigRepository.getCurrent();

		return ResponseEntity.ok(Map.of("stratTokensPerUsd", config.getStratTokensPerUsd(), "minimumPurchaseCents",
				config.getMinimumPurchaseCents(), "minimumTipStrat", config.getMinimumTipStrat(),
				"minimumWithdrawStrat", config.getMinimumWithdrawStrat()));
	}

}
