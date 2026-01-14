package io.strategiz.data.marketdata.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Standard timeframe constants for market data.
 *
 * <p>Industry-standard short format: 1Min, 5Min, 15Min, 30Min, 1H, 4H, 1D, 1W, 1M
 *
 * <p>IMPORTANT: Use these constants throughout all layers for consistency.
 * No aliases or normalization - strict short format only.
 */
public final class Timeframe {

	private Timeframe() {
		// Prevent instantiation
	}

	// === CANONICAL VALUES (short format - industry standard) ===

	/** 1 minute bars. */
	public static final String ONE_MINUTE = "1Min";

	/** 5 minute bars. */
	public static final String FIVE_MINUTES = "5Min";

	/** 15 minute bars. */
	public static final String FIFTEEN_MINUTES = "15Min";

	/** 30 minute bars. */
	public static final String THIRTY_MINUTES = "30Min";

	/** 1 hour bars. */
	public static final String ONE_HOUR = "1H";

	/** 4 hour bars. */
	public static final String FOUR_HOURS = "4H";

	/** Daily bars. */
	public static final String ONE_DAY = "1D";

	/** Weekly bars. */
	public static final String ONE_WEEK = "1W";

	/** Monthly bars. */
	public static final String ONE_MONTH = "1M";

	/** All valid timeframe values. */
	public static final Set<String> VALID_TIMEFRAMES = Collections.unmodifiableSet(new HashSet<>(
			Arrays.asList(ONE_MINUTE, FIVE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR, FOUR_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH)));

	/**
	 * Check if a timeframe string is valid.
	 *
	 * @param timeframe The timeframe to validate
	 * @return true if valid, false otherwise
	 */
	public static boolean isValid(String timeframe) {
		return timeframe != null && VALID_TIMEFRAMES.contains(timeframe);
	}

	/**
	 * Get default timeframe if null or empty provided.
	 *
	 * @param timeframe Input timeframe (may be null or empty)
	 * @return The input if valid, or ONE_DAY as default
	 */
	public static String getOrDefault(String timeframe) {
		if (timeframe == null || timeframe.isEmpty() || !isValid(timeframe)) {
			return ONE_DAY;
		}
		return timeframe;
	}

}
