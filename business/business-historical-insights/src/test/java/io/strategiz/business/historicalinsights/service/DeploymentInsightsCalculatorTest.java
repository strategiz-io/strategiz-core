package io.strategiz.business.historicalinsights.service;

import io.strategiz.business.historicalinsights.model.DeploymentInsights;
import io.strategiz.business.historicalinsights.model.DeploymentInsights.DeploymentMode;
import io.strategiz.business.historicalinsights.model.DeploymentInsights.DrawdownRiskLevel;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
import io.strategiz.business.historicalinsights.model.StrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DeploymentInsightsCalculator.
 * Tests Kelly criterion calculations, risk analysis, and deployment mode recommendations.
 */
@DisplayName("Deployment Insights Calculator Tests")
class DeploymentInsightsCalculatorTest {

	private DeploymentInsightsCalculator calculator;

	@BeforeEach
	void setUp() {
		calculator = new DeploymentInsightsCalculator();
	}

	@Nested
	@DisplayName("Kelly Criterion Calculations")
	class KellyCriterionTests {

		@Test
		@DisplayName("Should calculate Kelly percentage for high win rate strategy")
		void shouldCalculateKellyForHighWinRate() {
			StrategyTestResult result = createTestResult(0.65, 2.0, 12.0, 1.8, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getKellyPercentage() > 0, "Kelly should be positive for profitable strategy");
			assertTrue(insights.getConservativeKelly() > 0, "Conservative Kelly should be positive");
			assertEquals(insights.getKellyPercentage() / 2, insights.getConservativeKelly(), 0.01,
					"Conservative Kelly should be half of full Kelly");

			System.out.printf("High Win Rate Strategy: Kelly=%.2f%%, Conservative=%.2f%%%n",
					insights.getKellyPercentage(), insights.getConservativeKelly());
		}

		@Test
		@DisplayName("Should calculate Kelly percentage for low win rate with high reward")
		void shouldCalculateKellyForLowWinRateHighReward() {
			// Trend following strategy: low win rate but high profit factor
			StrategyTestResult result = createTestResult(0.35, 3.5, 25.0, 2.5, 50);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getKellyPercentage() > 0, "Kelly should be positive even with low win rate");
			System.out.printf("Low Win Rate Strategy: WinRate=35%%, PF=3.5, Kelly=%.2f%%%n",
					insights.getKellyPercentage());
		}

		@Test
		@DisplayName("Should return zero Kelly for losing strategy")
		void shouldReturnZeroKellyForLosingStrategy() {
			// Strategy that loses money
			StrategyTestResult result = createTestResult(0.30, 0.8, 35.0, 0.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(0, insights.getKellyPercentage(), 0.01, "Kelly should be 0 for losing strategy");
		}

		@ParameterizedTest
		@CsvSource({
				"0.50, 2.0, 25.0",  // 50% win rate, 2:1 reward
				"0.60, 1.5, 18.0",  // 60% win rate, 1.5:1 reward
				"0.70, 1.2, 12.0",  // 70% win rate, 1.2:1 reward
				"0.40, 3.0, 30.0",  // 40% win rate, 3:1 reward
		})
		@DisplayName("Should calculate Kelly for various win rate / reward ratios")
		void shouldCalculateKellyForVariousRatios(double winRate, double profitFactor, double expectedKellyMin) {
			StrategyTestResult result = createTestResult(winRate, profitFactor, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getKellyPercentage() > 0,
					String.format("Kelly should be positive for WinRate=%.0f%%, PF=%.1f", winRate * 100, profitFactor));
		}
	}

	@Nested
	@DisplayName("Position Sizing Recommendations")
	class PositionSizingTests {

		@Test
		@DisplayName("Should recommend conservative allocation for high drawdown strategy")
		void shouldRecommendConservativeForHighDrawdown() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 35.0, 1.2, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getRecommendedPortfolioAllocation() <= 10.0,
					"Should recommend <=10% allocation for 35% drawdown");
			assertTrue(insights.getAllocationRationale().contains("drawdown") ||
							insights.getAllocationRationale().toLowerCase().contains("risk"),
					"Rationale should mention drawdown or risk");

			System.out.printf("High Drawdown Strategy: Recommended Allocation=%.1f%%, Drawdown=%.1f%%%n",
					insights.getRecommendedPortfolioAllocation(), result.getMaxDrawdown());
		}

		@Test
		@DisplayName("Should recommend higher allocation for low drawdown strategy")
		void shouldRecommendHigherForLowDrawdown() {
			StrategyTestResult result = createTestResult(0.60, 2.5, 8.0, 2.0, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getRecommendedPortfolioAllocation() >= 5.0,
					"Should recommend >=5% allocation for 8% drawdown strategy");

			System.out.printf("Low Drawdown Strategy: Recommended Allocation=%.1f%%, Drawdown=%.1f%%%n",
					insights.getRecommendedPortfolioAllocation(), result.getMaxDrawdown());
		}

		@Test
		@DisplayName("Should cap allocation at 25%")
		void shouldCapAllocationAt25Percent() {
			// Excellent strategy that might otherwise suggest very high allocation
			StrategyTestResult result = createTestResult(0.75, 4.0, 5.0, 3.0, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getRecommendedPortfolioAllocation() <= 25.0,
					"Allocation should never exceed 25%");
		}

		@Test
		@DisplayName("Should floor allocation at 2%")
		void shouldFloorAllocationAt2Percent() {
			// Poor strategy
			StrategyTestResult result = createTestResult(0.40, 1.1, 40.0, 0.6, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getRecommendedPortfolioAllocation() >= 2.0,
					"Allocation should never be below 2%");
		}

		@Test
		@DisplayName("Should reduce allocation for low Sharpe ratio")
		void shouldReduceAllocationForLowSharpe() {
			// Good win rate but low Sharpe
			StrategyTestResult highSharpe = createTestResult(0.60, 2.0, 12.0, 1.8, 100);
			StrategyTestResult lowSharpe = createTestResult(0.60, 2.0, 12.0, 0.7, 100);

			DeploymentInsights highSharpeInsights = calculator.calculate(highSharpe, 1095);
			DeploymentInsights lowSharpeInsights = calculator.calculate(lowSharpe, 1095);

			assertTrue(lowSharpeInsights.getRecommendedPortfolioAllocation() <
							highSharpeInsights.getRecommendedPortfolioAllocation(),
					"Low Sharpe should result in lower allocation");
		}
	}

	@Nested
	@DisplayName("Drawdown Risk Analysis")
	class DrawdownRiskTests {

		@ParameterizedTest
		@CsvSource({
				"5.0, LOW",
				"8.0, LOW",
				"12.0, MEDIUM",
				"18.0, MEDIUM",
				"25.0, HIGH",
				"32.0, HIGH",
				"40.0, EXTREME",
				"55.0, EXTREME"
		})
		@DisplayName("Should classify drawdown risk levels correctly")
		void shouldClassifyDrawdownRiskLevels(double drawdown, DrawdownRiskLevel expectedLevel) {
			StrategyTestResult result = createTestResult(0.55, 1.8, drawdown, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(expectedLevel, insights.getDrawdownRiskLevel(),
					String.format("Drawdown %.1f%% should be %s", drawdown, expectedLevel));
		}

		@Test
		@DisplayName("Should calculate recovery required correctly")
		void shouldCalculateRecoveryRequired() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 20.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			// 20% drawdown requires 25% recovery: 1/(1-0.20) - 1 = 0.25
			assertEquals(25.0, insights.getRecoveryRequired(), 0.5,
					"20% drawdown should require ~25% recovery");
		}

		@Test
		@DisplayName("Should calculate extreme recovery for large drawdowns")
		void shouldCalculateExtremeRecovery() {
			StrategyTestResult result = createTestResult(0.50, 1.5, 50.0, 1.0, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			// 50% drawdown requires 100% recovery: 1/(1-0.50) - 1 = 1.0
			assertEquals(100.0, insights.getRecoveryRequired(), 1.0,
					"50% drawdown should require ~100% recovery");
		}

		@Test
		@DisplayName("Should provide meaningful drawdown explanation")
		void shouldProvideDrawdownExplanation() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 25.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getDrawdownExplanation());
			assertFalse(insights.getDrawdownExplanation().isEmpty());
			assertTrue(insights.getDrawdownExplanation().contains("25") ||
					insights.getDrawdownExplanation().contains("drawdown"));
		}
	}

	@Nested
	@DisplayName("Consecutive Loss Analysis")
	class ConsecutiveLossTests {

		@Test
		@DisplayName("Should estimate max consecutive losses from win rate")
		void shouldEstimateMaxConsecutiveLosses() {
			StrategyTestResult result = createTestResult(0.50, 1.5, 15.0, 1.2, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getMaxConsecutiveLosses() > 0,
					"Should estimate some consecutive losses");
			assertTrue(insights.getMaxConsecutiveLosses() <= 100,
					"Consecutive losses should be reasonable");

			System.out.printf("Win Rate 50%%: Estimated Max Consecutive Losses = %d%n",
					insights.getMaxConsecutiveLosses());
		}

		@Test
		@DisplayName("Should calculate probability of 5 consecutive losses")
		void shouldCalculateProbabilityOf5Losses() {
			// 50% win rate = 50% loss rate = 0.5^5 = 3.125%
			StrategyTestResult result = createTestResult(0.50, 1.5, 15.0, 1.2, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(3.125, insights.getProbabilityOf5ConsecutiveLosses(), 0.5,
					"50% win rate should have ~3.125% chance of 5 consecutive losses");
		}

		@Test
		@DisplayName("Should calculate lower probability for high win rate")
		void shouldCalculateLowerProbabilityForHighWinRate() {
			// 70% win rate = 30% loss rate = 0.3^5 = 0.243%
			StrategyTestResult result = createTestResult(0.70, 2.0, 12.0, 1.8, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getProbabilityOf5ConsecutiveLosses() < 1.0,
					"High win rate should have <1% chance of 5 consecutive losses");

			System.out.printf("Win Rate 70%%: P(5 consecutive losses) = %.2f%%%n",
					insights.getProbabilityOf5ConsecutiveLosses());
		}

		@Test
		@DisplayName("Should provide consecutive loss explanation")
		void shouldProvideConsecutiveLossExplanation() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getConsecutiveLossExplanation());
			assertFalse(insights.getConsecutiveLossExplanation().isEmpty());
		}
	}

	@Nested
	@DisplayName("Deployment Mode Recommendations")
	class DeploymentModeTests {

		@Test
		@DisplayName("Should recommend BOT for high frequency trading (500+ trades/year)")
		void shouldRecommendBotForHighFrequency() {
			// 1500 trades over 3 years = 500 trades/year
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 1500);

			DeploymentInsights insights = calculator.calculate(result, 1095); // 3 years

			assertNotNull(insights);
			assertEquals(DeploymentMode.BOT, insights.getRecommendedDeploymentMode(),
					"Should recommend BOT for 500+ trades/year");
			assertTrue(insights.getEstimatedTradesPerYear() >= 500);
			assertEquals("High Frequency", insights.getTradingFrequencyClassification());
		}

		@Test
		@DisplayName("Should recommend BOT for active trading (100-500 trades/year)")
		void shouldRecommendBotForActiveTrading() {
			// 300 trades over 3 years = 100 trades/year
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 300);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(DeploymentMode.BOT, insights.getRecommendedDeploymentMode(),
					"Should recommend BOT for 100+ trades/year");
			assertTrue(insights.getEstimatedTradesPerYear() >= 100);
			assertEquals("Active Trading", insights.getTradingFrequencyClassification());
		}

		@Test
		@DisplayName("Should recommend ALERT for swing trading (50-100 trades/year)")
		void shouldRecommendAlertForSwingTrading() {
			// 180 trades over 3 years = 60 trades/year
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 180);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(DeploymentMode.ALERT, insights.getRecommendedDeploymentMode(),
					"Should recommend ALERT for 50-100 trades/year");
			assertEquals("Swing Trading", insights.getTradingFrequencyClassification());
		}

		@Test
		@DisplayName("Should recommend ALERT for position trading (<50 trades/year)")
		void shouldRecommendAlertForPositionTrading() {
			// 90 trades over 3 years = 30 trades/year
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 90);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(DeploymentMode.ALERT, insights.getRecommendedDeploymentMode(),
					"Should recommend ALERT for <50 trades/year");
			assertEquals("Position Trading", insights.getTradingFrequencyClassification());
		}

		@Test
		@DisplayName("Should provide deployment mode rationale")
		void shouldProvideDeploymentModeRationale() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 300);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getDeploymentModeRationale());
			assertFalse(insights.getDeploymentModeRationale().isEmpty());
			assertTrue(insights.getDeploymentModeRationale().contains("trades") ||
					insights.getDeploymentModeRationale().contains("year"));
		}
	}

	@Nested
	@DisplayName("Risk Metric Interpretations")
	class RiskMetricInterpretationTests {

		@ParameterizedTest
		@CsvSource({
				"3.5, Excellent",
				"2.5, Very Good",
				"1.5, Good",
				"0.7, Moderate",
				"0.3, Poor"
		})
		@DisplayName("Should interpret Sharpe ratio correctly")
		void shouldInterpretSharpeRatio(double sharpe, String expectedKeyword) {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, sharpe, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getSharpeRatioInterpretation());
			assertTrue(insights.getSharpeRatioInterpretation().contains(expectedKeyword),
					String.format("Sharpe %.1f should be interpreted as %s", sharpe, expectedKeyword));
		}

		@ParameterizedTest
		@CsvSource({
				"3.5, Excellent",
				"2.5, Very Good",
				"1.7, Good",
				"1.1, Break-even",
				"0.8, Losing"
		})
		@DisplayName("Should interpret profit factor correctly")
		void shouldInterpretProfitFactor(double pf, String expectedKeyword) {
			StrategyTestResult result = createTestResult(0.55, pf, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getProfitFactorInterpretation());
			assertTrue(insights.getProfitFactorInterpretation().contains(expectedKeyword),
					String.format("PF %.1f should be interpreted as %s", pf, expectedKeyword));
		}

		@ParameterizedTest
		@CsvSource({
				"0.75, High",
				"0.60, Good",
				"0.48, Moderate",
				"0.38, Low",
				"0.25, Very Low"
		})
		@DisplayName("Should interpret win rate correctly")
		void shouldInterpretWinRate(double winRate, String expectedKeyword) {
			StrategyTestResult result = createTestResult(winRate, 1.8, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getWinRateInterpretation());
			assertTrue(insights.getWinRateInterpretation().contains(expectedKeyword),
					String.format("Win rate %.0f%% should be interpreted as %s", winRate * 100, expectedKeyword));
		}
	}

	@Nested
	@DisplayName("Alert vs Bot Feature Lists")
	class AlertBotFeaturesTests {

		@Test
		@DisplayName("Should populate alert advantages")
		void shouldPopulateAlertAdvantages() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getAlertAdvantages());
			assertFalse(insights.getAlertAdvantages().isEmpty());
			assertTrue(insights.getAlertAdvantages().size() >= 3);
		}

		@Test
		@DisplayName("Should populate alert limitations")
		void shouldPopulateAlertLimitations() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getAlertLimitations());
			assertFalse(insights.getAlertLimitations().isEmpty());
			assertTrue(insights.getAlertLimitations().size() >= 3);
		}

		@Test
		@DisplayName("Should populate bot advantages")
		void shouldPopulateBotAdvantages() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getBotAdvantages());
			assertFalse(insights.getBotAdvantages().isEmpty());
			assertTrue(insights.getBotAdvantages().size() >= 5);
		}

		@Test
		@DisplayName("Should populate bot limitations")
		void shouldPopulateBotLimitations() {
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights.getBotLimitations());
			assertFalse(insights.getBotLimitations().isEmpty());
			assertTrue(insights.getBotLimitations().size() >= 3);
		}
	}

	@Nested
	@DisplayName("Edge Cases and Error Handling")
	class EdgeCaseTests {

		@Test
		@DisplayName("Should handle null strategy result")
		void shouldHandleNullResult() {
			DeploymentInsights insights = calculator.calculate(null, 1095);

			assertNotNull(insights);
			assertEquals(5.0, insights.getRecommendedPortfolioAllocation(), 0.1,
					"Should return default allocation for null result");
		}

		@Test
		@DisplayName("Should handle failed strategy result")
		void shouldHandleFailedResult() {
			StrategyTestResult result = StrategyTestResult.failed(
					StrategyType.RSI_MEAN_REVERSION, Map.of(), "Execution error");

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(5.0, insights.getRecommendedPortfolioAllocation(), 0.1,
					"Should return default allocation for failed result");
		}

		@Test
		@DisplayName("Should handle zero trades")
		void shouldHandleZeroTrades() {
			StrategyTestResult result = createTestResult(0.50, 1.5, 15.0, 1.0, 0);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(0, insights.getEstimatedTradesPerYear());
		}

		@Test
		@DisplayName("Should handle zero win rate")
		void shouldHandleZeroWinRate() {
			StrategyTestResult result = createTestResult(0.0, 0.0, 50.0, 0.0, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertEquals(0, insights.getKellyPercentage(), 0.01);
		}

		@Test
		@DisplayName("Should handle 100% win rate")
		void shouldHandle100PercentWinRate() {
			StrategyTestResult result = createTestResult(1.0, 10.0, 5.0, 3.0, 100);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			assertNotNull(insights);
			assertTrue(insights.getKellyPercentage() > 0);
		}

		@Test
		@DisplayName("Should handle short analysis period")
		void shouldHandleShortPeriod() {
			// 30 days instead of 3 years
			StrategyTestResult result = createTestResult(0.55, 1.8, 15.0, 1.5, 10);

			DeploymentInsights insights = calculator.calculate(result, 30);

			assertNotNull(insights);
			// 10 trades in 30 days = ~122 trades/year
			assertTrue(insights.getEstimatedTradesPerYear() > 100);
		}
	}

	@Nested
	@DisplayName("Integration Scenarios")
	class IntegrationScenarioTests {

		@Test
		@DisplayName("Conservative swing trader scenario")
		void shouldHandleConservativeSwingTrader() {
			// Low frequency, moderate risk, consistent returns
			StrategyTestResult result = createTestResult(0.58, 1.6, 12.0, 1.3, 120);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			// Should recommend alert mode for swing trading
			assertEquals(DeploymentMode.ALERT, insights.getRecommendedDeploymentMode());
			assertEquals(DrawdownRiskLevel.MEDIUM, insights.getDrawdownRiskLevel());
			assertTrue(insights.getRecommendedPortfolioAllocation() >= 5.0);
			assertTrue(insights.getRecommendedPortfolioAllocation() <= 15.0);

			System.out.println("Conservative Swing Trader:");
			System.out.printf("  Allocation: %.1f%%%n", insights.getRecommendedPortfolioAllocation());
			System.out.printf("  Mode: %s%n", insights.getRecommendedDeploymentMode());
			System.out.printf("  Frequency: %s%n", insights.getTradingFrequencyClassification());
		}

		@Test
		@DisplayName("Aggressive day trader scenario")
		void shouldHandleAggressiveDayTrader() {
			// High frequency, higher risk, high returns
			StrategyTestResult result = createTestResult(0.52, 2.2, 22.0, 1.6, 600);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			// Should recommend bot mode for high frequency
			assertEquals(DeploymentMode.BOT, insights.getRecommendedDeploymentMode());
			assertEquals(DrawdownRiskLevel.HIGH, insights.getDrawdownRiskLevel());

			System.out.println("Aggressive Day Trader:");
			System.out.printf("  Allocation: %.1f%%%n", insights.getRecommendedPortfolioAllocation());
			System.out.printf("  Mode: %s%n", insights.getRecommendedDeploymentMode());
			System.out.printf("  Frequency: %s (~%d trades/year)%n",
					insights.getTradingFrequencyClassification(),
					insights.getEstimatedTradesPerYear());
		}

		@Test
		@DisplayName("Long-term position trader scenario")
		void shouldHandleLongTermPositionTrader() {
			// Very low frequency, low risk, steady returns
			StrategyTestResult result = createTestResult(0.65, 2.8, 8.0, 1.9, 45);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			// Should recommend alert mode for position trading
			assertEquals(DeploymentMode.ALERT, insights.getRecommendedDeploymentMode());
			assertEquals(DrawdownRiskLevel.LOW, insights.getDrawdownRiskLevel());
			assertEquals("Position Trading", insights.getTradingFrequencyClassification());

			System.out.println("Long-term Position Trader:");
			System.out.printf("  Allocation: %.1f%%%n", insights.getRecommendedPortfolioAllocation());
			System.out.printf("  Mode: %s%n", insights.getRecommendedDeploymentMode());
			System.out.printf("  Recovery Required: %.1f%%%n", insights.getRecoveryRequired());
		}

		@Test
		@DisplayName("High-risk scalper scenario")
		void shouldHandleHighRiskScalper() {
			// Extreme frequency, higher drawdown, smaller profits per trade
			StrategyTestResult result = createTestResult(0.48, 1.4, 28.0, 0.9, 2500);

			DeploymentInsights insights = calculator.calculate(result, 1095);

			// Should recommend bot mode and flag high risk
			assertEquals(DeploymentMode.BOT, insights.getRecommendedDeploymentMode());
			assertTrue(insights.getDrawdownRiskLevel() == DrawdownRiskLevel.HIGH ||
					insights.getDrawdownRiskLevel() == DrawdownRiskLevel.EXTREME);
			assertTrue(insights.getRecommendedPortfolioAllocation() <= 10.0,
					"Should recommend conservative allocation for high-risk scalper");

			System.out.println("High-risk Scalper:");
			System.out.printf("  Allocation: %.1f%%%n", insights.getRecommendedPortfolioAllocation());
			System.out.printf("  Risk Level: %s%n", insights.getDrawdownRiskLevel());
			System.out.printf("  P(5 losses): %.1f%%%n", insights.getProbabilityOf5ConsecutiveLosses());
		}
	}

	// ========== Helper Methods ==========

	private StrategyTestResult createTestResult(double winRate, double profitFactor,
												double maxDrawdown, double sharpeRatio, int totalTrades) {
		StrategyTestResult result = new StrategyTestResult(StrategyType.RSI_MEAN_REVERSION, Map.of());
		result.setWinRate(winRate);
		result.setProfitFactor(profitFactor);
		result.setMaxDrawdown(maxDrawdown);
		result.setSharpeRatio(sharpeRatio);
		result.setTotalTrades(totalTrades);
		result.setTotalReturn(50.0); // Default positive return
		result.setSuccess(true);
		return result;
	}
}
