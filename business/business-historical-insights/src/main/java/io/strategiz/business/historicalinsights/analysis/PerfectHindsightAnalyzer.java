package io.strategiz.business.historicalinsights.analysis;

import io.strategiz.business.historicalinsights.model.PriceTurningPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates the theoretical maximum return achievable with perfect knowledge of all
 * price peaks and troughs. This serves as an upper bound benchmark for evaluating
 * strategy efficiency.
 *
 * Key metrics: - Perfect hindsight return: Maximum possible return if you bought every
 * trough and sold every peak - Strategy efficiency: (Actual return / Perfect return) -
 * how close a strategy comes to perfection - Missed opportunities: Analysis of what
 * percentage of swings were captured
 */
@Component
public class PerfectHindsightAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(PerfectHindsightAnalyzer.class);

	/**
	 * Result of perfect hindsight analysis.
	 */
	public static class PerfectHindsightResult {

		private double perfectReturn;

		private double compoundedPerfectReturn;

		private int totalSwings;

		private double avgSwingCapture;

		private double medianSwingCapture;

		private double largestSwing;

		private double smallestSwing;

		private List<SwingOpportunity> swings;

		public double getPerfectReturn() {
			return perfectReturn;
		}

		public void setPerfectReturn(double perfectReturn) {
			this.perfectReturn = perfectReturn;
		}

		public double getCompoundedPerfectReturn() {
			return compoundedPerfectReturn;
		}

		public void setCompoundedPerfectReturn(double compoundedPerfectReturn) {
			this.compoundedPerfectReturn = compoundedPerfectReturn;
		}

		public int getTotalSwings() {
			return totalSwings;
		}

		public void setTotalSwings(int totalSwings) {
			this.totalSwings = totalSwings;
		}

		public double getAvgSwingCapture() {
			return avgSwingCapture;
		}

		public void setAvgSwingCapture(double avgSwingCapture) {
			this.avgSwingCapture = avgSwingCapture;
		}

		public double getMedianSwingCapture() {
			return medianSwingCapture;
		}

		public void setMedianSwingCapture(double medianSwingCapture) {
			this.medianSwingCapture = medianSwingCapture;
		}

		public double getLargestSwing() {
			return largestSwing;
		}

		public void setLargestSwing(double largestSwing) {
			this.largestSwing = largestSwing;
		}

		public double getSmallestSwing() {
			return smallestSwing;
		}

		public void setSmallestSwing(double smallestSwing) {
			this.smallestSwing = smallestSwing;
		}

		public List<SwingOpportunity> getSwings() {
			return swings;
		}

		public void setSwings(List<SwingOpportunity> swings) {
			this.swings = swings;
		}

		/**
		 * Calculate strategy efficiency ratio.
		 * @param actualReturn The actual return achieved by a strategy
		 * @return Efficiency ratio (0.0 to 1.0+, where 1.0 = perfect)
		 */
		public double calculateEfficiency(double actualReturn) {
			if (perfectReturn <= 0) {
				return actualReturn > 0 ? 1.0 : 0.0;
			}
			return actualReturn / perfectReturn;
		}

		/**
		 * Format as summary string.
		 */
		public String toSummary() {
			return String.format(
					"Perfect Hindsight: %.2f%% (compounded: %.2f%%), %d swings, avg: %.2f%%, largest: %.2f%%",
					perfectReturn, compoundedPerfectReturn, totalSwings, avgSwingCapture, largestSwing);
		}

	}

	/**
	 * Represents a single swing opportunity (trough to peak).
	 */
	public static class SwingOpportunity {

		private PriceTurningPoint entry;

		private PriceTurningPoint exit;

		private double returnPct;

		private int durationDays;

		public SwingOpportunity(PriceTurningPoint entry, PriceTurningPoint exit) {
			this.entry = entry;
			this.exit = exit;
			if (entry != null && exit != null && entry.getPrice() > 0) {
				this.returnPct = ((exit.getPrice() - entry.getPrice()) / entry.getPrice()) * 100;
				long diffMs = exit.getTimestamp().toEpochMilli() - entry.getTimestamp().toEpochMilli();
				this.durationDays = (int) (diffMs / (1000 * 60 * 60 * 24));
			}
		}

		public PriceTurningPoint getEntry() {
			return entry;
		}

		public PriceTurningPoint getExit() {
			return exit;
		}

		public double getReturnPct() {
			return returnPct;
		}

		public int getDurationDays() {
			return durationDays;
		}

	}

	/**
	 * Analyze turning points to calculate perfect hindsight return.
	 * @param turningPoints List of price turning points (peaks and troughs)
	 * @return PerfectHindsightResult with analysis
	 */
	public PerfectHindsightResult analyze(List<PriceTurningPoint> turningPoints) {
		PerfectHindsightResult result = new PerfectHindsightResult();
		result.setSwings(new ArrayList<>());

		if (turningPoints == null || turningPoints.size() < 2) {
			log.warn("Insufficient turning points for perfect hindsight analysis");
			return result;
		}

		// Sort turning points by timestamp
		List<PriceTurningPoint> sorted = turningPoints.stream()
			.sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
			.toList();

		// Find all trough-to-peak swings
		List<SwingOpportunity> swings = new ArrayList<>();
		double totalSimpleReturn = 0;
		double compoundedValue = 1.0;

		PriceTurningPoint currentTrough = null;

		for (PriceTurningPoint tp : sorted) {
			if (tp.getType() == PriceTurningPoint.PointType.TROUGH) {
				// Start of a potential swing
				currentTrough = tp;
			}
			else if (tp.getType() == PriceTurningPoint.PointType.PEAK && currentTrough != null) {
				// End of a swing - calculate return
				SwingOpportunity swing = new SwingOpportunity(currentTrough, tp);

				if (swing.getReturnPct() > 0) {
					swings.add(swing);
					totalSimpleReturn += swing.getReturnPct();

					// Compounded return: multiply by (1 + return/100)
					compoundedValue *= (1 + swing.getReturnPct() / 100);
				}

				currentTrough = null;
			}
		}

		// Calculate statistics
		result.setSwings(swings);
		result.setTotalSwings(swings.size());
		result.setPerfectReturn(totalSimpleReturn);
		result.setCompoundedPerfectReturn((compoundedValue - 1) * 100);

		if (!swings.isEmpty()) {
			// Average swing
			result.setAvgSwingCapture(totalSimpleReturn / swings.size());

			// Median swing
			List<Double> sortedReturns = swings.stream().map(SwingOpportunity::getReturnPct).sorted().toList();
			int mid = sortedReturns.size() / 2;
			if (sortedReturns.size() % 2 == 0) {
				result.setMedianSwingCapture((sortedReturns.get(mid - 1) + sortedReturns.get(mid)) / 2);
			}
			else {
				result.setMedianSwingCapture(sortedReturns.get(mid));
			}

			// Min and max swings
			result.setLargestSwing(sortedReturns.get(sortedReturns.size() - 1));
			result.setSmallestSwing(sortedReturns.get(0));
		}

		log.info("Perfect hindsight analysis complete: {} swings, {:.2f}% simple return, {:.2f}% compounded",
				swings.size(), totalSimpleReturn, result.getCompoundedPerfectReturn());

		return result;
	}

	/**
	 * Calculate missed opportunities analysis by comparing actual trades to perfect
	 * hindsight swings.
	 * @param perfectResult The perfect hindsight result
	 * @param actualReturn The actual return achieved
	 * @param actualTrades The number of trades made
	 * @return Analysis of missed opportunities
	 */
	public MissedOpportunitiesAnalysis analyzeMissedOpportunities(PerfectHindsightResult perfectResult,
			double actualReturn, int actualTrades) {

		MissedOpportunitiesAnalysis analysis = new MissedOpportunitiesAnalysis();

		if (perfectResult == null || perfectResult.getTotalSwings() == 0) {
			return analysis;
		}

		// Calculate capture rate
		analysis.setTotalPossibleSwings(perfectResult.getTotalSwings());
		analysis.setPerfectReturn(perfectResult.getPerfectReturn());
		analysis.setActualReturn(actualReturn);

		// Efficiency ratio
		double efficiency = perfectResult.calculateEfficiency(actualReturn);
		analysis.setEfficiencyRatio(efficiency);

		// Estimate captured swings based on efficiency
		int estimatedCaptured = (int) Math.round(perfectResult.getTotalSwings() * efficiency);
		analysis.setEstimatedSwingsCaptured(estimatedCaptured);
		analysis.setEstimatedSwingsMissed(perfectResult.getTotalSwings() - estimatedCaptured);

		// Calculate opportunity cost (return left on the table)
		double opportunityCost = perfectResult.getPerfectReturn() - actualReturn;
		analysis.setOpportunityCost(opportunityCost);

		// Improvement potential
		if (actualReturn > 0) {
			analysis.setImprovementPotential((perfectResult.getPerfectReturn() / actualReturn - 1) * 100);
		}
		else {
			analysis.setImprovementPotential(perfectResult.getPerfectReturn());
		}

		// Generate recommendations
		analysis.setRecommendations(generateRecommendations(analysis, perfectResult));

		return analysis;
	}

	/**
	 * Generate recommendations based on missed opportunities analysis.
	 */
	private List<String> generateRecommendations(MissedOpportunitiesAnalysis analysis,
			PerfectHindsightResult perfectResult) {

		List<String> recommendations = new ArrayList<>();

		double efficiency = analysis.getEfficiencyRatio();

		if (efficiency < 0.20) {
			recommendations.add("Strategy is capturing < 20% of available swings. Consider loosening entry criteria.");
			recommendations
				.add("Review if entry thresholds are too strict - lower the buy dip % or adjust RSI oversold level.");
		}
		else if (efficiency < 0.40) {
			recommendations.add("Moderate capture rate. Consider adding momentum confirmation to improve timing.");
			recommendations.add("Evaluate exit timing - may be exiting too early or too late.");
		}
		else if (efficiency < 0.60) {
			recommendations.add("Good capture rate. Focus on reducing false signals and improving entry precision.");
		}
		else {
			recommendations
				.add("Excellent capture rate. Strategy is approaching optimal. Focus on risk management improvements.");
		}

		// Check if avg swing is being captured
		if (perfectResult.getAvgSwingCapture() > 0) {
			double avgPerTrade = analysis.getActualReturn() / Math.max(1, analysis.getEstimatedSwingsCaptured());
			double captureEfficiency = avgPerTrade / perfectResult.getAvgSwingCapture();

			if (captureEfficiency < 0.5) {
				recommendations.add(String.format("Capturing only %.0f%% of average swing magnitude. "
						+ "Consider tightening stops and extending profit targets.", captureEfficiency * 100));
			}
		}

		// Check for too many or too few trades
		if (analysis.getEstimatedSwingsCaptured() > perfectResult.getTotalSwings() * 1.5) {
			recommendations
				.add("Generating more trades than available swings - likely over-trading with false signals.");
		}

		return recommendations;
	}

	/**
	 * Analysis of missed trading opportunities.
	 */
	public static class MissedOpportunitiesAnalysis {

		private int totalPossibleSwings;

		private int estimatedSwingsCaptured;

		private int estimatedSwingsMissed;

		private double perfectReturn;

		private double actualReturn;

		private double efficiencyRatio;

		private double opportunityCost;

		private double improvementPotential;

		private List<String> recommendations;

		public int getTotalPossibleSwings() {
			return totalPossibleSwings;
		}

		public void setTotalPossibleSwings(int totalPossibleSwings) {
			this.totalPossibleSwings = totalPossibleSwings;
		}

		public int getEstimatedSwingsCaptured() {
			return estimatedSwingsCaptured;
		}

		public void setEstimatedSwingsCaptured(int estimatedSwingsCaptured) {
			this.estimatedSwingsCaptured = estimatedSwingsCaptured;
		}

		public int getEstimatedSwingsMissed() {
			return estimatedSwingsMissed;
		}

		public void setEstimatedSwingsMissed(int estimatedSwingsMissed) {
			this.estimatedSwingsMissed = estimatedSwingsMissed;
		}

		public double getPerfectReturn() {
			return perfectReturn;
		}

		public void setPerfectReturn(double perfectReturn) {
			this.perfectReturn = perfectReturn;
		}

		public double getActualReturn() {
			return actualReturn;
		}

		public void setActualReturn(double actualReturn) {
			this.actualReturn = actualReturn;
		}

		public double getEfficiencyRatio() {
			return efficiencyRatio;
		}

		public void setEfficiencyRatio(double efficiencyRatio) {
			this.efficiencyRatio = efficiencyRatio;
		}

		public double getOpportunityCost() {
			return opportunityCost;
		}

		public void setOpportunityCost(double opportunityCost) {
			this.opportunityCost = opportunityCost;
		}

		public double getImprovementPotential() {
			return improvementPotential;
		}

		public void setImprovementPotential(double improvementPotential) {
			this.improvementPotential = improvementPotential;
		}

		public List<String> getRecommendations() {
			return recommendations;
		}

		public void setRecommendations(List<String> recommendations) {
			this.recommendations = recommendations;
		}

		/**
		 * Format as summary string.
		 */
		public String toSummary() {
			return String.format(
					"Efficiency: %.1f%% (captured ~%d/%d swings), Opportunity cost: %.2f%%, "
							+ "Improvement potential: %.1f%%",
					efficiencyRatio * 100, estimatedSwingsCaptured, totalPossibleSwings, opportunityCost,
					improvementPotential);
		}

	}

}
