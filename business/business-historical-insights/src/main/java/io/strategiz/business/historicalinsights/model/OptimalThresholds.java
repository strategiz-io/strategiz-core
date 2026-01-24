package io.strategiz.business.historicalinsights.model;

/**
 * Optimal entry/exit thresholds derived from historical turning point analysis.
 * These thresholds are calculated by analyzing what indicator values were present
 * at actual market peaks and troughs, allowing us to recommend thresholds that
 * would have captured most historical opportunities.
 */
public class OptimalThresholds {

	// RSI thresholds (from analyzing RSI values at turning points)
	private double rsiOversold; // RSI value at 80th percentile of troughs

	private double rsiOverbought; // RSI value at 80th percentile of peaks

	private double rsiOversoldConfidence; // % of troughs captured at this threshold

	private double rsiOverboughtConfidence; // % of peaks captured at this threshold

	// Price drop/rise thresholds (from analyzing price moves)
	private double pctDropForEntry; // % drop from high that captures 80% of troughs

	private double pctGainForExit; // % gain from low that captures 80% of peaks

	private double pctDropConfidence;

	private double pctGainConfidence;

	// Bollinger Band thresholds
	private double bbLowerPctForEntry; // % below lower BB for optimal entries

	private double bbUpperPctForExit; // % above upper BB for optimal exits

	// Volume thresholds
	private double volumeRatioForConfirmation; // Volume ratio that confirms moves

	// Stochastic thresholds
	private double stochasticOversold;

	private double stochasticOverbought;

	// Sample sizes used for calculation
	private int troughsAnalyzed;

	private int peaksAnalyzed;

	public OptimalThresholds() {
		// Set sensible defaults
		this.rsiOversold = 30.0;
		this.rsiOverbought = 70.0;
		this.pctDropForEntry = 8.0;
		this.pctGainForExit = 12.0;
		this.stochasticOversold = 20.0;
		this.stochasticOverbought = 80.0;
		this.volumeRatioForConfirmation = 1.5;
	}

	// Getters and Setters

	public double getRsiOversold() {
		return rsiOversold;
	}

	public void setRsiOversold(double rsiOversold) {
		this.rsiOversold = rsiOversold;
	}

	public double getRsiOverbought() {
		return rsiOverbought;
	}

	public void setRsiOverbought(double rsiOverbought) {
		this.rsiOverbought = rsiOverbought;
	}

	public double getRsiOversoldConfidence() {
		return rsiOversoldConfidence;
	}

	public void setRsiOversoldConfidence(double rsiOversoldConfidence) {
		this.rsiOversoldConfidence = rsiOversoldConfidence;
	}

	public double getRsiOverboughtConfidence() {
		return rsiOverboughtConfidence;
	}

	public void setRsiOverboughtConfidence(double rsiOverboughtConfidence) {
		this.rsiOverboughtConfidence = rsiOverboughtConfidence;
	}

	public double getPctDropForEntry() {
		return pctDropForEntry;
	}

	public void setPctDropForEntry(double pctDropForEntry) {
		this.pctDropForEntry = pctDropForEntry;
	}

	public double getPctGainForExit() {
		return pctGainForExit;
	}

	public void setPctGainForExit(double pctGainForExit) {
		this.pctGainForExit = pctGainForExit;
	}

	public double getPctDropConfidence() {
		return pctDropConfidence;
	}

	public void setPctDropConfidence(double pctDropConfidence) {
		this.pctDropConfidence = pctDropConfidence;
	}

	public double getPctGainConfidence() {
		return pctGainConfidence;
	}

	public void setPctGainConfidence(double pctGainConfidence) {
		this.pctGainConfidence = pctGainConfidence;
	}

	public double getBbLowerPctForEntry() {
		return bbLowerPctForEntry;
	}

	public void setBbLowerPctForEntry(double bbLowerPctForEntry) {
		this.bbLowerPctForEntry = bbLowerPctForEntry;
	}

	public double getBbUpperPctForExit() {
		return bbUpperPctForExit;
	}

	public void setBbUpperPctForExit(double bbUpperPctForExit) {
		this.bbUpperPctForExit = bbUpperPctForExit;
	}

	public double getVolumeRatioForConfirmation() {
		return volumeRatioForConfirmation;
	}

	public void setVolumeRatioForConfirmation(double volumeRatioForConfirmation) {
		this.volumeRatioForConfirmation = volumeRatioForConfirmation;
	}

	public double getStochasticOversold() {
		return stochasticOversold;
	}

	public void setStochasticOversold(double stochasticOversold) {
		this.stochasticOversold = stochasticOversold;
	}

	public double getStochasticOverbought() {
		return stochasticOverbought;
	}

	public void setStochasticOverbought(double stochasticOverbought) {
		this.stochasticOverbought = stochasticOverbought;
	}

	public int getTroughsAnalyzed() {
		return troughsAnalyzed;
	}

	public void setTroughsAnalyzed(int troughsAnalyzed) {
		this.troughsAnalyzed = troughsAnalyzed;
	}

	public int getPeaksAnalyzed() {
		return peaksAnalyzed;
	}

	public void setPeaksAnalyzed(int peaksAnalyzed) {
		this.peaksAnalyzed = peaksAnalyzed;
	}

	/**
	 * Generate a prompt-friendly summary of the optimal thresholds.
	 */
	public String toPromptSummary() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("OPTIMAL ENTRY THRESHOLDS (from %d historical troughs):\n", troughsAnalyzed));
		sb.append(String.format("- RSI below %.1f captures %.0f%% of optimal entries\n", rsiOversold,
				rsiOversoldConfidence * 100));
		sb.append(String.format("- Price drop of %.1f%% from high captures %.0f%% of entries\n", pctDropForEntry,
				pctDropConfidence * 100));
		sb.append(String.format("\nOPTIMAL EXIT THRESHOLDS (from %d historical peaks):\n", peaksAnalyzed));
		sb.append(String.format("- RSI above %.1f captures %.0f%% of optimal exits\n", rsiOverbought,
				rsiOverboughtConfidence * 100));
		sb.append(String.format("- Price gain of %.1f%% captures %.0f%% of exits\n", pctGainForExit,
				pctGainConfidence * 100));
		return sb.toString();
	}

	@Override
	public String toString() {
		return String.format("OptimalThresholds[RSI: %.0f/%.0f, PctDrop/Gain: %.1f%%/%.1f%%, analyzed: %d/%d]",
				rsiOversold, rsiOverbought, pctDropForEntry, pctGainForExit, troughsAnalyzed, peaksAnalyzed);
	}

}
