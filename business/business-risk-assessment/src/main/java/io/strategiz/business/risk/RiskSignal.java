package io.strategiz.business.risk;

/**
 * A single risk signal produced by a {@link RiskSignalProvider}.
 *
 * @param name the signal provider name
 * @param score the risk score contributed (0 to maxScore)
 * @param maxScore the maximum possible score for this signal
 * @param reason human-readable explanation of the score
 */
public record RiskSignal(String name, int score, int maxScore, String reason) {

	public static RiskSignal clean(String name, int maxScore) {
		return new RiskSignal(name, 0, maxScore, "No risk detected");
	}

}
