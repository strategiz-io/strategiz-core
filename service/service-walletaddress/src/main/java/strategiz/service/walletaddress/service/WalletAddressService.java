package strategiz.service.walletaddress.service;

import io.strategiz.client.walletaddress.client.WalletAddressBlockchainClient;
import io.strategiz.client.walletaddress.model.WalletAddress;
import io.strategiz.client.walletaddress.model.WalletBalance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WalletAddressService {
    @Autowired
    private WalletAddressFirestoreService firestoreService;
    @Autowired
    private WalletAddressBlockchainClient blockchainClient;

    // Retrieve wallet addresses for user from Firestore
    public List<WalletAddress> getUserWallets(String userId) {
        return firestoreService.getWalletAddresses(userId);
    }

    // Save/update wallet addresses for user in Firestore
    public void saveUserWallets(String userId, List<WalletAddress> wallets) {
        firestoreService.saveWalletAddresses(userId, wallets);
    }

    // Aggregate balances for all wallets
    public List<WalletBalance> getWalletBalances(String userId) {
        List<WalletAddress> wallets = getUserWallets(userId);
        return blockchainClient.fetchAllBalances(wallets);
    }
}
