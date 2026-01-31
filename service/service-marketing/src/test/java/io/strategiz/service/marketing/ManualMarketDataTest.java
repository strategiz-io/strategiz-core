package io.strategiz.service.marketing;

import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.List;

/**
 * Manual test to verify real API data fetching. Run this with: mvn spring-boot:run
 * -Dstart-class=io.strategiz.service.marketing.ManualMarketDataTest
 */
@SpringBootApplication
public class ManualMarketDataTest {

	public static void main(String[] args) {
		// Set properties for testing
		System.setProperty("coingecko.api.url", "https://api.coingecko.com/api/v3");
		System.setProperty("alphavantage.api.url", "https://www.alphavantage.co");
		System.setProperty("alphavantage.api.key", "demo");

		ConfigurableApplicationContext context = SpringApplication.run(ManualMarketDataTest.class, args);

		try {
			// Test CoinGecko
			CoinGeckoClient coinGeckoClient = context.getBean(CoinGeckoClient.class);
			System.out.println("\n=== Testing CoinGecko API ===");

			List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum", "solana");
			var cryptoData = coinGeckoClient.getCryptocurrencyMarketData(cryptoIds, "usd");

			cryptoData.forEach(crypto -> {
				System.out.printf("%s (%s): $%.2f, 24h change: %.2f%%\n", crypto.getName(),
						crypto.getSymbol().toUpperCase(), crypto.getCurrentPrice(),
						crypto.getPriceChangePercentage24h());
			});

			// Test Alpha Vantage
			AlphaVantageClient alphaVantageClient = context.getBean(AlphaVantageClient.class);
			System.out.println("\n=== Testing Alpha Vantage API ===");

			try {
				var stockData = alphaVantageClient.getStockQuote("AAPL");
				System.out.printf("%s: $%.2f, change: %.2f (%.2f%%)\n", stockData.getName(), stockData.getPrice(),
						stockData.getChange(), stockData.getChangePercent());
			}
			catch (Exception e) {
				System.out.println("Alpha Vantage error (expected with demo key): " + e.getMessage());
			}

		}
		catch (Exception e) {
			System.err.println("Error testing APIs: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			context.close();
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		@Qualifier("coinGeckoRestTemplate")
		public RestTemplate coinGeckoRestTemplate(RestTemplateBuilder builder) {
			return builder.rootUri("https://api.coingecko.com/api/v3").build();
		}

		@Bean
		@Qualifier("alphaVantageRestTemplate")
		public RestTemplate alphaVantageRestTemplate(RestTemplateBuilder builder) {
			return builder.rootUri("https://www.alphavantage.co").build();
		}

		@Bean
		public CoinGeckoClient coinGeckoClient(@Qualifier("coinGeckoRestTemplate") RestTemplate restTemplate) {
			return new CoinGeckoClient(restTemplate);
		}

		@Bean
		public AlphaVantageClient alphaVantageClient(@Qualifier("alphaVantageRestTemplate") RestTemplate restTemplate) {
			return new AlphaVantageClient(restTemplate);
		}

	}

}