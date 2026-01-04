package io.strategiz.service.labs.utils;

/**
 * Utility class for strategy name operations
 * Handles name normalization for case-insensitive uniqueness checks
 */
public class StrategyNameUtils {

	/**
	 * Normalize strategy name for case-insensitive uniqueness checks
	 * Rules:
	 * - Convert to lowercase
	 * - Trim leading/trailing whitespace
	 * - Collapse multiple consecutive spaces to single space
	 * - Preserve Unicode characters (no slugification)
	 *
	 * Examples:
	 * "  My STRATEGY  " → "my strategy"
	 * "MACD  Cross" → "macd cross"
	 * "Стратегия RSI" → "стратегия rsi"
	 *
	 * @param name Strategy name to normalize
	 * @return Normalized name for comparison, or original if null/empty
	 */
	public static String normalizeName(String name) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		return name.trim().toLowerCase().replaceAll("\\s+", " ");
	}

	/**
	 * Check if two strategy names are equal (case-insensitive)
	 *
	 * @param name1 First name
	 * @param name2 Second name
	 * @return true if names are equal after normalization
	 */
	public static boolean areNamesEqual(String name1, String name2) {
		if (name1 == null && name2 == null) {
			return true;
		}
		if (name1 == null || name2 == null) {
			return false;
		}
		return normalizeName(name1).equals(normalizeName(name2));
	}

}
