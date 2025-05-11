package io.strategiz.walletaddress.model;

public class WalletAddress {
    private String blockchain; // e.g. ETH, SOL
    private String address;

    public WalletAddress() {}
    public WalletAddress(String blockchain, String address) {
        this.blockchain = blockchain;
        this.address = address;
    }

    public String getBlockchain() { return blockchain; }
    public void setBlockchain(String blockchain) { this.blockchain = blockchain; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
