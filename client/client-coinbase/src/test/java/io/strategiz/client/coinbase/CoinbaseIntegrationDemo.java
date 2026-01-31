package io.strategiz.client.coinbase;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import io.strategiz.client.coinbase.model.CoinbaseResponse;
import io.strategiz.client.coinbase.model.TickerPrice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demonstration of the Coinbase API integration This connects to the real
 * Coinbase API to fetch actual cryptocurrency data
 */
public class CoinbaseIntegrationDemo {

	// List of cryptocurrencies to fetch prices for
	private static final List<String> CRYPTOCURRENCIES = Arrays.asList("BTC", "ETH", "SOL", "ADA", "DOT", "AVAX",
			"MATIC", "LINK", "UNI", "XRP");

	public static void main(String[] args) {
		System.out.println("Coinbase API Integration Demonstration");
		System.out.println("======================================");

		// Create a RestTemplate for HTTP requests
		RestTemplate restTemplate = new RestTemplate();

		// Initialize the Coinbase client
		CoinbaseClient client = new CoinbaseClient(restTemplate);

		try {
			// Fetch and display current cryptocurrency prices
			displayCryptocurrencyPrices(client);

			System.out.println("\nCoinbase API integration demonstration completed successfully!");
		}
		catch (Exception e) {
			System.err.println("Error during Coinbase API integration demonstration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void displayCryptocurrencyPrices(CoinbaseClient client) {
		System.out.println("\nCurrent Cryptocurrency Prices (USD):");
		System.out.println("----------------------------------");

		// Format for displaying prices
		String format = "%-6s | $%-10s";
		System.out.println(String.format(format, "Symbol", "Price"));
		System.out.println("------------------");

		// Fetch prices for each cryptocurrency
		for (String crypto : CRYPTOCURRENCIES) {
			try {
				Map<String, String> params = new HashMap<>();
				params.put("currency", "USD");

				CoinbaseResponse<TickerPrice> response = client.publicRequest(HttpMethod.GET,
						"/prices/" + crypto + "-USD/spot", params,
						new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {
						});

				// Display the price
				String price = response.getData().get(0).getAmount();
				System.out.println(String.format(format, crypto, price));

				// Small delay to avoid rate limiting
				Thread.sleep(200);
			}
			catch (Exception e) {
				System.out.println(String.format(format, crypto, "Error: " + e.getMessage()));
			}
		}
	}

}
