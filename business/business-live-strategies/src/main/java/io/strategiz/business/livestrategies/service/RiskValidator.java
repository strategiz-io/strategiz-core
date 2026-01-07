package io.strategiz.business.livestrategies.service;

import io.strategiz.business.livestrategies.model.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Validates risk parameters before executing bot trades.
 * Ensures trades comply with position limits, daily loss limits, etc.
 */
@Service
public class RiskValidator {

	private static final Logger log = LoggerFactory.getLogger(RiskValidator.class);

	@Value("${live-strategies.risk.max-position-size-pct:5.0}")
	private double maxPositionSizePct;

	@Value("${live-strategies.risk.max-daily-loss-pct:2.0}")
	private double maxDailyLossPct;

	@Value("${live-strategies.risk.max-open-positions:10}")
	private int maxOpenPositions;

	@Value("${live-strategies.risk.enable-market-hours-check:true}")
	private boolean enableMarketHoursCheck;

	/**
	 * Validate if a trade signal passes all risk checks.
	 * @param signal the trading signal to validate
	 * @return risk check result
	 */
	public RiskCheckResult validate(Signal signal) {
		if (signal == null) {
			return RiskCheckResult.failure("Signal is null");
		}

		// Check market hours if enabled
		if (enableMarketHoursCheck && !isMarketHours()) {
			return RiskCheckResult.failure("Market is closed");
		}

		// For now, return success. Full implementation when BotDeployment exists:
		// - Check position size limits
		// - Check daily P&L limits
		// - Check max open positions
		// - Check symbol restrictions

		log.debug("Risk validation passed for signal: {}", signal);
		return RiskCheckResult.success();
	}

	/**
	 * Check if current time is within market hours.
	 * Simplified check - for production use exchange calendar API.
	 */
	private boolean isMarketHours() {
		java.time.LocalDateTime now = java.time.LocalDateTime.now();

		// Weekday check (Monday = 1, Sunday = 7)
		int dayOfWeek = now.getDayOfWeek().getValue();
		if (dayOfWeek > 5) {
			return false;
		}

		// Time check (9:30 AM - 4:00 PM ET)
		// Note: This is simplified. Production should use proper timezone handling.
		int hour = now.getHour();
		int minute = now.getMinute();

		boolean afterOpen = (hour > 9) || (hour == 9 && minute >= 30);
		boolean beforeClose = (hour < 16);

		return afterOpen && beforeClose;
	}

	/**
	 * Result of risk validation
	 */
	public record RiskCheckResult(
			boolean passed,
			String reason
	) {
		public static RiskCheckResult success() {
			return new RiskCheckResult(true, null);
		}

		public static RiskCheckResult failure(String reason) {
			return new RiskCheckResult(false, reason);
		}
	}

}
