package io.strategiz.business.risk;

/**
 * Risk level classification based on aggregate risk score.
 *
 * <ul>
 * <li>LOW (0-29): Normal — proceed with device trust</li>
 * <li>MEDIUM (30-49): Caution — require at least one explicit factor</li>
 * <li>HIGH (50-74): Elevated — require full MFA regardless of device trust</li>
 * <li>CRITICAL (75-100): Block — lock account, notify user</li>
 * </ul>
 */
public enum RiskLevel {

	LOW(0, 29, "Normal risk — proceed with device trust"),
	MEDIUM(30, 49, "Elevated caution — require at least one explicit factor"),
	HIGH(50, 74, "High risk — require full MFA regardless of device trust"),
	CRITICAL(75, 100, "Critical risk — block authentication and notify user");

	private final int minScore;

	private final int maxScore;

	private final String description;

	RiskLevel(int minScore, int maxScore, String description) {
		this.minScore = minScore;
		this.maxScore = maxScore;
		this.description = description;
	}

	public int getMinScore() {
		return minScore;
	}

	public int getMaxScore() {
		return maxScore;
	}

	public String getDescription() {
		return description;
	}

	public static RiskLevel fromScore(int score) {
		if (score >= CRITICAL.minScore) {
			return CRITICAL;
		}
		if (score >= HIGH.minScore) {
			return HIGH;
		}
		if (score >= MEDIUM.minScore) {
			return MEDIUM;
		}
		return LOW;
	}

}
