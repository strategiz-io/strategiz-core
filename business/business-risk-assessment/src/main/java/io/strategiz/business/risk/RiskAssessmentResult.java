package io.strategiz.business.risk;

import java.util.List;

/**
 * Result of a risk assessment containing the total score, risk level, and individual signals.
 *
 * @param totalScore aggregate risk score (0-100)
 * @param riskLevel classified risk level
 * @param signals individual risk signals from each provider
 */
public record RiskAssessmentResult(int totalScore, RiskLevel riskLevel, List<RiskSignal> signals) {
}
