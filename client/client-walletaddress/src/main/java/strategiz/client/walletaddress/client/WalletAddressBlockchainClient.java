package io.strategiz.walletaddress.client;

import io.strategiz.walletaddress.model.WalletAddress;
import io.strategiz.walletaddress.model.WalletBalance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Component
public class WalletAddressBlockchainClient {
    @Value("${etherscan.api.key}")
    private String etherscanApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<WalletBalance> fetchAllBalances(List<WalletAddress> wallets) {
        List<WalletBalance> balances = new ArrayList<>();
        for (WalletAddress wallet : wallets) {
            switch (wallet.getBlockchain().toUpperCase()) {
                case "ETH":
                    balances.add(fetchEthBalance(wallet.getAddress()));
                    break;
                case "BTC":
                    balances.add(fetchBtcBalance(wallet.getAddress()));
                    break;
                case "SOL":
                    balances.add(fetchSolBalance(wallet.getAddress()));
                    break;
                // Add more blockchains as needed
            }
        }
        return balances;
    }

    private WalletBalance fetchEthBalance(String address) {
        try {
            String url = String.format(
                "https://api.etherscan.io/api?module=account&action=balance&address=%s&tag=latest&apikey=%s",
                address, etherscanApiKey
            );
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject json = new JSONObject(response.getBody());
            if (json.getString("status").equals("1")) {
                double balanceEth = Double.parseDouble(json.getString("result")) / 1e18;
                return new WalletBalance("ETH", address, balanceEth);
            }
        } catch (Exception e) {
            // Log error if needed
        }
        return new WalletBalance("ETH", address, 0.0);
    }

    private WalletBalance fetchBtcBalance(String address) {
        try {
            // Blockchair API (no key needed for basic usage)
            String url = String.format("https://api.blockchair.com/bitcoin/dashboards/address/%s", address);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject json = new JSONObject(response.getBody());
            JSONObject data = json.getJSONObject("data").getJSONObject(address).getJSONObject("address");
            double satoshis = data.getDouble("balance");
            double btc = satoshis / 1e8;
            return new WalletBalance("BTC", address, btc);
        } catch (Exception e) {
            // Log error if needed
        }
        return new WalletBalance("BTC", address, 0.0);
    }

    private WalletBalance fetchSolBalance(String address) {
        try {
            // Solscan API (no key needed for basic usage)
            String url = String.format("https://api.solscan.io/account/tokens?account=%s", address);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // This endpoint returns a list of token balances. For native SOL, use a different endpoint:
            String solUrl = String.format("https://api.solscan.io/account?address=%s", address);
            ResponseEntity<String> solResponse = restTemplate.getForEntity(solUrl, String.class);
            JSONObject solJson = new JSONObject(solResponse.getBody());
            double lamports = solJson.optDouble("lamports", 0.0);
            double sol = lamports / 1e9;
            return new WalletBalance("SOL", address, sol);
        } catch (Exception e) {
            // Log error if needed
        }
        return new WalletBalance("SOL", address, 0.0);
    }
}
