package io.strategiz.business.strategyanalysis.model;

/**
 * Mode for strategy analysis, determines prompt and focus.
 */
public enum AnalysisMode {

	/**
	 * Diagnostic mode: Analyze why strategy produced no signals.
	 * Focus: Identify issues preventing signal generation.
	 */
	NO_SIGNALS,

	/**
	 * Optimization mode: Suggest improvements to boost performance.
	 * Focus: Enhance existing strategy metrics (return, Sharpe, drawdown).
	 */
	OPTIMIZATION,

	/**
	 * General analysis mode: Broad code review and suggestions.
	 * Focus: Code quality, best practices, potential improvements.
	 */
	GENERAL

}
