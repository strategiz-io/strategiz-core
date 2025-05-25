package strategiz.client.coinbase.demo;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import strategiz.client.coinbase.CoinbaseClient;
import strategiz.client.coinbase.model.CoinbaseResponse;
import strategiz.client.coinbase.model.TickerPrice;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple demonstration of fetching real-time cryptocurrency prices from the Coinbase API
 */
public class CoinbasePriceDemo {

    public static void main(String[] args) {
        System.out.println("Coinbase Real-Time Price Demo");
        System.out.println("============================");
        
        try {
            // Create a RestTemplate for HTTP requests
            RestTemplate restTemplate = new RestTemplate();
            
            // Initialize the Coinbase client
            CoinbaseClient client = new CoinbaseClient(restTemplate);
            
            // Fetch Bitcoin price
            System.out.println("Fetching Bitcoin (BTC) price...");
            Map<String, String> params = new HashMap<>();
            params.put("currency", "USD");
            
            CoinbaseResponse<TickerPrice> btcResponse = client.publicRequest(
                HttpMethod.GET,
                "/prices/BTC-USD/spot",
                params,
                new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
            );
            
            System.out.println("Bitcoin price: $" + btcResponse.getData().get(0).getAmount() + " USD");
            
            // Fetch Ethereum price
            System.out.println("\nFetching Ethereum (ETH) price...");
            
            CoinbaseResponse<TickerPrice> ethResponse = client.publicRequest(
                HttpMethod.GET,
                "/prices/ETH-USD/spot",
                params,
                new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
            );
            
            System.out.println("Ethereum price: $" + ethResponse.getData().get(0).getAmount() + " USD");
            
            System.out.println("\nDemo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
