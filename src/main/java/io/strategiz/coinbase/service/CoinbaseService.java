package io.strategiz.coinbase.service;

import io.strategiz.coinbase.client.CoinbaseClient;
import io.strategiz.coinbase.client.exception.CoinbaseApiException;
import io.strategiz.coinbase.model.Account;
import io.strategiz.coinbase.model.TickerPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for interacting with the Coinbase API
 * This service uses CoinbaseClient for API communication and focuses on business logic
 */
@Slf4j
@Service
public class CoinbaseService {

    private final CoinbaseClient coinbaseClient;
    
    @Autowired
    public CoinbaseService(CoinbaseClient coinbaseClient) {
        this.coinbaseClient = coinbaseClient;
    }

    /**
     * Configure the Coinbase API credentials
     * @param apiKey API key
     * @param privateKey Private key
     * @return Configuration status
     */
    public Map<String, String> configure(String apiKey, String privateKey, String passphrase) {
        Map<String, String> response = new HashMap<>();
        if (apiKey == null || apiKey.isEmpty() || privateKey == null || privateKey.isEmpty() || passphrase == null || passphrase.isEmpty()) {
            response.put("status", "error");
            response.put("message", "API Key, Secret Key, and Passphrase are all required");
            return response;
        }
        
        // Test the connection to verify credentials
        boolean connectionSuccessful = testConnection(apiKey, privateKey, passphrase);
        if (connectionSuccessful) {
            response.put("status", "success");
            response.put("message", "Coinbase API configured successfully");
        } else {
            response.put("status", "error");
            response.put("message", "Failed to connect to Coinbase API with provided credentials");
        }
        
        return response;
    }

    /**
     * Get user accounts from Coinbase
     * @param apiKey API key
     * @param privateKey Private key
     * @return List of accounts
     */
    public List<Account> getAccounts(String apiKey, String privateKey, String passphrase) {
        return coinbaseClient.getAccounts(apiKey, privateKey, passphrase);
    }

    /**
     * Get ticker price for a specific currency pair
     * @param baseCurrency Base currency (e.g., BTC)
     * @param quoteCurrency Quote currency (e.g., USD)
     * @return Ticker price
     */
    public TickerPrice getTickerPrice(String baseCurrency, String quoteCurrency) {
        return coinbaseClient.getTickerPrice(baseCurrency, quoteCurrency);
    }

    /**
     * Get account balances with USD values
     * @param apiKey API key
     * @param privateKey Private key
     * @return List of accounts with balances and USD values
     */
    public List<Account> getAccountBalances(String apiKey, String privateKey, String passphrase) {
        try {
            // Get all accounts using the client
            List<Account> accounts = coinbaseClient.getAccounts(apiKey, privateKey, passphrase);
            
            // Filter accounts with non-zero balances
            List<Account> nonZeroAccounts = accounts.stream()
                .filter(account -> {
                    if (account.getBalance() == null) return false;
                    double amount = Double.parseDouble(account.getBalance().getAmount());
                    return amount > 0;
                })
                .collect(Collectors.toList());
            
            // Calculate USD values for each account
            for (Account account : nonZeroAccounts) {
                String currency = account.getCurrency();
                double amount = Double.parseDouble(account.getBalance().getAmount());
                account.getBalance().setAmountValue(amount);
                
                double usdValue = 0;
                
                // Handle USD directly
                if (currency.equals("USD") || currency.equals("USDT") || currency.equals("USDC") || 
                    currency.equals("BUSD") || currency.equals("DAI")) {
                    usdValue = amount;
                } else {
                    // Get price in USD using the client
                    TickerPrice ticker = coinbaseClient.getTickerPrice(currency, "USD");
                    if (ticker != null) {
                        usdValue = amount * Double.parseDouble(ticker.getAmount());
                    }
                }
                
                account.getBalance().setUsdValue(usdValue);
                account.setUsdValue(usdValue);
            }
            
            // Sort by USD value
            nonZeroAccounts.sort((a, b) -> Double.compare(b.getUsdValue(), a.getUsdValue()));
            
            return nonZeroAccounts;
        } catch (Exception e) {
            log.error("Error getting account balances: {}", e.getMessage());
            throw new RuntimeException("Error getting account balances", e);
        }
    }

    /**
     * Calculate total USD value of accounts
     * @param accounts List of accounts
     * @return Total USD value
     */
    public double calculateTotalUsdValue(List<Account> accounts) {
        return accounts.stream()
            .mapToDouble(Account::getUsdValue)
            .sum();
    }

    /**
     * Get raw account data from Coinbase API
     * This method returns the completely unmodified raw data from Coinbase API
     * 
     * @param apiKey API key
     * @param privateKey Private key
     * @return Raw account data
     */
    public Object getRawAccountData(String apiKey, String privateKey, String passphrase) {
        try {
            return coinbaseClient.getRawAccountData(apiKey, privateKey, passphrase);
        } catch (CoinbaseApiException e) {
            log.error("Detailed Coinbase API error: {}", e.getErrorDetails());
            throw e; // Re-throw to propagate to controller
        }
    }

    /**
     * Test connection to Coinbase API
     * 
     * @param apiKey API key
     * @param privateKey Private key
     * @return True if connection is successful, false otherwise
     */
    public boolean testConnection(String apiKey, String privateKey, String passphrase) {
        return coinbaseClient.testConnection(apiKey, privateKey, passphrase);
    }
}
