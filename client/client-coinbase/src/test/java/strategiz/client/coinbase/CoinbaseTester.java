package strategiz.client.coinbase;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import strategiz.client.coinbase.model.CoinbaseResponse;
import strategiz.client.coinbase.model.TickerPrice;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple tester class for Coinbase API integration
 * This class demonstrates how to use the CoinbaseClient to make API calls
 */
public class CoinbaseTester {

    // Replace these with your actual Coinbase API credentials
    private static final String API_KEY = "your-api-key";
    private static final String PRIVATE_KEY = "your-private-key";
    private static final String PASSPHRASE = "your-passphrase";

    public static void main(String[] args) {
        System.out.println("Testing Coinbase API integration...");
        
        // Create a RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        
        // Create the CoinbaseClient
        CoinbaseClient client = new CoinbaseClient(restTemplate);
        
        try {
            // Make a public request to get Bitcoin price
            System.out.println("Getting Bitcoin price...");
            Map<String, String> params = new HashMap<>();
            params.put("currency", "USD");
            
            CoinbaseResponse<TickerPrice> response = client.publicRequest(
                HttpMethod.GET,
                "/prices/BTC-USD/spot",
                params,
                new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
            );
            
            System.out.println("Bitcoin price: " + response.getData().get(0).getAmount() + " " + response.getData().get(0).getCurrency());
            
            // If you have API credentials, you can make authenticated requests
            if (!API_KEY.equals("your-api-key")) {
                System.out.println("Making authenticated request to get accounts...");
                
                CoinbaseResponse<Map<String, Object>> accountsResponse = client.signedRequest(
                    HttpMethod.GET,
                    "/accounts",
                    null,
                    API_KEY,
                    PRIVATE_KEY,
                    new ParameterizedTypeReference<CoinbaseResponse<Map<String, Object>>>() {}
                );
                
                System.out.println("Accounts: " + accountsResponse.getData());
            } else {
                System.out.println("Skipping authenticated request (API credentials not provided)");
            }
            
            System.out.println("Coinbase API integration test completed successfully!");
        } catch (Exception e) {
            System.err.println("Error testing Coinbase API integration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
