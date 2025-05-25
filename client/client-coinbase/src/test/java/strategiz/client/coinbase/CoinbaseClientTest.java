package strategiz.client.coinbase;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import strategiz.client.coinbase.model.CoinbaseResponse;
import strategiz.client.coinbase.model.TickerPrice;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for demonstrating the Coinbase client integration
 * This connects to the real Coinbase API to fetch actual data
 */
public class CoinbaseClientTest {

    public static void main(String[] args) {
        System.out.println("Coinbase Client Integration Test");
        System.out.println("================================");
        
        // Create a RestTemplate for HTTP requests
        RestTemplate restTemplate = new RestTemplate();
        
        // Initialize the Coinbase client
        CoinbaseClient client = new CoinbaseClient(restTemplate);
        
        try {
            // Test public API endpoints (no authentication required)
            testPublicEndpoints(client);
            
            // If you have API credentials, you can test authenticated endpoints
            // Uncomment and add your credentials to test
            // testAuthenticatedEndpoints(client, "your-api-key", "your-private-key", "your-passphrase");
            
            System.out.println("\nCoinbase client integration test completed successfully!");
        } catch (Exception e) {
            System.err.println("Error testing Coinbase client: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testPublicEndpoints(CoinbaseClient client) {
        System.out.println("\nTesting public endpoints:");
        
        // Get Bitcoin price
        System.out.println("- Fetching Bitcoin price...");
        Map<String, String> params = new HashMap<>();
        params.put("currency", "USD");
        
        CoinbaseResponse<TickerPrice> btcResponse = client.publicRequest(
            HttpMethod.GET,
            "/prices/BTC-USD/spot",
            params,
            new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
        );
        
        System.out.println("  Bitcoin price: " + btcResponse.getData().get(0).getAmount() + " " + 
                          btcResponse.getData().get(0).getCurrency());
        
        // Get Ethereum price
        System.out.println("- Fetching Ethereum price...");
        params = new HashMap<>();
        params.put("currency", "USD");
        
        CoinbaseResponse<TickerPrice> ethResponse = client.publicRequest(
            HttpMethod.GET,
            "/prices/ETH-USD/spot",
            params,
            new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
        );
        
        System.out.println("  Ethereum price: " + ethResponse.getData().get(0).getAmount() + " " + 
                          ethResponse.getData().get(0).getCurrency());
    }
    
    private static void testAuthenticatedEndpoints(CoinbaseClient client, String apiKey, String privateKey, String passphrase) {
        System.out.println("\nTesting authenticated endpoints:");
        
        // Get user accounts
        System.out.println("- Fetching user accounts...");
        
        CoinbaseResponse<Map<String, Object>> accountsResponse = client.signedRequest(
            HttpMethod.GET,
            "/accounts",
            null,
            apiKey,
            privateKey,
            new ParameterizedTypeReference<CoinbaseResponse<Map<String, Object>>>() {}
        );
        
        System.out.println("  Found " + accountsResponse.getData().size() + " accounts");
        
        // Print account details
        for (Map<String, Object> account : accountsResponse.getData()) {
            System.out.println("  - Account: " + account.get("name") + 
                              " (" + account.get("currency") + ")" +
                              " Balance: " + account.get("balance"));
        }
    }
}
