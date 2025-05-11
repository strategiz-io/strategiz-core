package io.strategiz.coinbase.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class CoinbaseAdvancedTradeClient {
    private static final Logger log = LoggerFactory.getLogger(CoinbaseAdvancedTradeClient.class);
    private static final String BASE_URL = "https://api.coinbase.com/api/v3/brokerage/accounts";
    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> getAccounts(String apiKey, String privateKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("CB-ACCESS-KEY", apiKey);
        headers.set("CB-ACCESS-SIGN", privateKey); // NOTE: You may need to generate a real signature here if required by the API
        headers.set("CB-VERSION", "2023-01-01");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        return restTemplate.exchange(BASE_URL, HttpMethod.GET, entity, String.class);
    }
}
