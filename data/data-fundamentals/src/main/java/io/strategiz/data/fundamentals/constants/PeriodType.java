package io.strategiz.data.fundamentals.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Constants for fundamental data period types.
 *
 * Fundamentals can be reported on different time periods: - QUARTERLY: Quarterly
 * financial reports (Q1, Q2, Q3, Q4) - ANNUAL: Annual financial reports (fiscal year) -
 * TTM: Trailing Twelve Months (rolling 12-month period)
 */
public final class PeriodType {

	/**
	 * Quarterly financial report (3 months)
	 */
	public static final String QUARTERLY = "QUARTERLY";

	/**
	 * Annual financial report (fiscal year)
	 */
	public static final String ANNUAL = "ANNUAL";

	/**
	 * Trailing Twelve Months (rolling 12-month period)
	 */
	public static final String TTM = "TTM";

	/**
	 * Set of all valid period types
	 */
	public static final Set<String> VALID_PERIOD_TYPES = new HashSet<>(Arrays.asList(QUARTERLY, ANNUAL, TTM));

	/**
	 * Private constructor to prevent instantiation
	 */
	private PeriodType() {
		throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
	}

	/**
	 * Check if a period type is valid.
	 * @param periodType the period type to validate
	 * @return true if the period type is valid, false otherwise
	 */
	public static boolean isValid(String periodType) {
		return periodType != null && VALID_PERIOD_TYPES.contains(periodType.toUpperCase());
	}

	/**
	 * Normalize a period type to uppercase canonical form.
	 * @param periodType the period type to normalize
	 * @return the normalized period type, or null if invalid
	 */
	public static String normalize(String periodType) {
		if (periodType == null) {
			return null;
		}
		String normalized = periodType.toUpperCase();
		return isValid(normalized) ? normalized : null;
	}

}
