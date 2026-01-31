package io.strategiz.service.labs.bdd;

import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for Cucumber BDD tests.
 *
 * Provides mocked AI strategy service to enable BDD tests to run without requiring real
 * AI service credentials or external API calls.
 *
 * Excludes Firebase/Firestore and data source auto-configuration to avoid requiring GCP
 * credentials and database connections in tests.
 *
 * PasetoTokenValidator is mocked via @MockBean in CucumberSpringConfiguration.
 *
 * NOTE: These are UNIT tests with mocked AI - they verify test structure and constant
 * extraction logic. For INTEGRATION tests with real AI, use a separate test suite with
 * proper credentials.
 */
@SpringBootApplication(exclude = { com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration.class,
		com.google.cloud.spring.autoconfigure.firestore.FirestoreTransactionManagerAutoConfiguration.class,
		org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
		org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class })
public class BDDTestConfiguration {

	/**
	 * Mock AI Strategy Service that generates strategies with all required constants.
	 *
	 * This mock parses the prompt and generates Python code demonstrating the expected
	 * format.
	 */
	@Bean
	@Primary
	public AIStrategyService mockAIStrategyService() {
		return new AIStrategyService(null, null, null, null, null) {
			@Override
			protected String getModuleName() {
				return "test";
			}

			@Override
			public AIStrategyResponse generateStrategy(AIStrategyRequest request) {
				String prompt = request.getPrompt();

				// Extract constants from prompt
				String symbol = extractSymbol(prompt);
				String timeframe = extractTimeframe(prompt);
				double stopLoss = extractStopLoss(prompt);
				double takeProfit = extractTakeProfit(prompt);

				// Generate mock Python code with all required constants
				String pythonCode = String.format("""
						# Configuration Constants
						SYMBOL = '%s'
						TIMEFRAME = '%s'
						STOP_LOSS = %.1f
						TAKE_PROFIT = %.1f
						POSITION_SIZE = 5

						# Strategy Implementation
						def strategy(data):
						    # Mock strategy logic
						    if data['rsi'].iloc[-1] < 30:
						        return 'BUY'
						    elif data['rsi'].iloc[-1] > 70:
						        return 'SELL'
						    return 'HOLD'
						""", symbol, timeframe, stopLoss, takeProfit);

				AIStrategyResponse response = new AIStrategyResponse();
				response.setSuccess(true);
				response.setPythonCode(pythonCode);
				return response;
			}

			private String extractSymbol(String prompt) {
				String lower = prompt.toLowerCase();
				// Check in order of appearance in prompt (first mention wins for
				// multi-symbol prompts)
				if (lower.contains("qqq") && lower.indexOf("qqq") < lower.indexOf("spy"))
					return "QQQ";
				if (lower.contains("aapl") || lower.contains("apple"))
					return "AAPL";
				if (lower.contains("msft") || lower.contains("microsoft"))
					return "MSFT";
				if (lower.contains("googl") || lower.contains("google"))
					return "GOOGL";
				if (lower.contains("tsla") || lower.contains("tesla"))
					return "TSLA";
				if (lower.contains("nvda"))
					return "NVDA";
				if (lower.contains("amzn") || lower.contains("amazon"))
					return "AMZN";
				if (lower.contains("spy"))
					return "SPY";
				if (lower.contains("qqq"))
					return "QQQ";
				if (lower.contains("btc") || lower.contains("bitcoin"))
					return "BTC";
				if (lower.contains("eth") || lower.contains("ethereum"))
					return "ETH";
				if (lower.contains("sol") || lower.contains("solana"))
					return "SOL";
				return "SPY"; // Default
			}

			private String extractTimeframe(String prompt) {
				String lower = prompt.toLowerCase();
				// Check longer patterns first to avoid substring matches
				// Convention: lowercase for minutes/hours (1m, 30m, 1h, 4h), uppercase
				// for day+ (1D, 1W, 1M)
				if (lower.contains("30 min") || lower.contains("30 minute"))
					return "30m";
				if (lower.contains("4 hour") || lower.contains("4h"))
					return "4h";
				if (lower.contains("1 minute") || lower.contains("1min"))
					return "1m";
				if (lower.contains("1 hour") || lower.contains("hourly") || lower.contains("1h"))
					return "1h";
				if (lower.contains("hourly basis"))
					return "1h";
				if (lower.contains("daily") || lower.contains("1 day") || lower.contains("1d"))
					return "1D";
				if (lower.contains("weekly") || lower.contains("1 week") || lower.contains("1w"))
					return "1W";
				if (lower.contains("monthly"))
					return "1M";
				return "1D"; // Default
			}

			private double extractStopLoss(String prompt) {
				String lower = prompt.toLowerCase();
				// Check specific patterns first
				if (lower.contains("stop loss of 1.5%"))
					return 1.5;
				if (lower.contains("1% stop") || lower.contains("1 percent stop"))
					return 1.0;
				if (lower.contains("stop at 1%"))
					return 1.0;
				if (lower.contains("1.5% stop"))
					return 1.5;
				if (lower.contains("2% stop") || lower.contains("2 percent stop"))
					return 2.0;
				if (lower.contains("stop at 2%"))
					return 2.0;
				if (lower.contains("cut losses at 2.5%"))
					return 2.5;
				if (lower.contains("2.5% stop") || lower.contains("2.5 percent"))
					return 2.5;
				if (lower.contains("stop at 2.5 percent"))
					return 2.5;
				if (lower.contains("3% stop") || lower.contains("three percent"))
					return 3.0;
				if (lower.contains("cut losses at three percent"))
					return 3.0;
				if (lower.contains("set stop at three percent"))
					return 3.0;
				if (lower.contains("risk 4 percent"))
					return 4.0;
				if (lower.contains("4% stop"))
					return 4.0;
				if (lower.contains("4% stop loss"))
					return 4.0;
				if (lower.contains("stop at 5%"))
					return 5.0;
				if (lower.contains("5% stop") || lower.contains("risk 5%"))
					return 5.0;
				if (lower.contains("risk 5"))
					return 5.0;
				if (lower.contains("mean reversion"))
					return 2.0; // Tighter for mean reversion
				return 3.0; // Default
			}

			private double extractTakeProfit(String prompt) {
				String lower = prompt.toLowerCase();
				if (lower.contains("2% profit") || lower.contains("2% target"))
					return 2.0;
				if (lower.contains("target 2% profit"))
					return 2.0;
				if (lower.contains("6% profit"))
					return 6.0;
				if (lower.contains("8% profit") || lower.contains("8% target"))
					return 8.0;
				if (lower.contains("8% take profit"))
					return 8.0;
				if (lower.contains("take profit of nine percent"))
					return 9.0;
				if (lower.contains("9% profit") || lower.contains("nine percent"))
					return 9.0;
				if (lower.contains("book profits at nine percent"))
					return 9.0;
				if (lower.contains("target 10%"))
					return 10.0;
				if (lower.contains("10% profit") || lower.contains("10% gain"))
					return 10.0;
				if (lower.contains("target 10% gain"))
					return 10.0;
				if (lower.contains("book profits at 12%"))
					return 12.0;
				if (lower.contains("12% profit"))
					return 12.0;
				if (lower.contains("12% take profit"))
					return 12.0;
				if (lower.contains("15% profit"))
					return 15.0;
				if (lower.contains("15% profit target"))
					return 15.0;
				if (lower.contains("exit at 20% gain"))
					return 20.0;
				if (lower.contains("20% profit") || lower.contains("20% target"))
					return 20.0;
				// Default: 3x stop loss
				return extractStopLoss(prompt) * 3.0;
			}
		};
	}

}
