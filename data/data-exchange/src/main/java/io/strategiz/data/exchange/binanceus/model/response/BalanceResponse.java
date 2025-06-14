package io.strategiz.data.exchange.binanceus.model.response;

import io.strategiz.data.exchange.binanceus.model.Account;
import io.strategiz.data.exchange.binanceus.model.Balance;
import java.util.List;
import java.util.Objects;

/**
 * Response model for balance information
 */
public class BalanceResponse {
    private List<Balance> positions;
    private double totalUsdValue;
    private Account rawAccountData; // Complete unmodified raw data from the API
    
    // Constructors
    public BalanceResponse() {}
    
    public BalanceResponse(List<Balance> positions, double totalUsdValue, Account rawAccountData) {
        this.positions = positions;
        this.totalUsdValue = totalUsdValue;
        this.rawAccountData = rawAccountData;
    }
    
    // Getters and setters
    public List<Balance> getPositions() { return positions; }
    public void setPositions(List<Balance> positions) { this.positions = positions; }
    
    public double getTotalUsdValue() { return totalUsdValue; }
    public void setTotalUsdValue(double totalUsdValue) { this.totalUsdValue = totalUsdValue; }
    
    public Account getRawAccountData() { return rawAccountData; }
    public void setRawAccountData(Account rawAccountData) { this.rawAccountData = rawAccountData; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BalanceResponse that = (BalanceResponse) o;
        return Double.compare(that.totalUsdValue, totalUsdValue) == 0 &&
               Objects.equals(positions, that.positions) &&
               Objects.equals(rawAccountData, that.rawAccountData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(positions, totalUsdValue, rawAccountData);
    }
    
    @Override
    public String toString() {
        return "BalanceResponse{" +
               "positions=" + positions +
               ", totalUsdValue=" + totalUsdValue +
               ", rawAccountData=" + rawAccountData +
               '}';
    }
}
