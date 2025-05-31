package io.strategiz.client.walletaddress.model;

public class WalletBalance {
    private String blockchain;
    private String address;
    private double balance;

    public WalletBalance() {}
    public WalletBalance(String blockchain, String address, double balance) {
        this.blockchain = blockchain;
        this.address = address;
        this.balance = balance;
    }

    public String getBlockchain() { return blockchain; }
    public void setBlockchain(String blockchain) { this.blockchain = blockchain; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
