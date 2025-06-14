package io.strategiz.data.exchange.binanceus.model.response;

import io.strategiz.data.exchange.binanceus.model.Account;
import java.util.Objects;

/**
 * Response model for raw account information
 */
public class RawAccountResponse {
    private Account account;
    
    // Constructors
    public RawAccountResponse() {}
    
    public RawAccountResponse(Account account) {
        this.account = account;
    }
    
    // Getters and setters
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawAccountResponse that = (RawAccountResponse) o;
        return Objects.equals(account, that.account);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(account);
    }
    
    @Override
    public String toString() {
        return "RawAccountResponse{" +
               "account=" + account +
               '}';
    }
}
