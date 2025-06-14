package io.strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

/**
 * Response model for account balance information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountBalanceResponse {
    private List<Balance> positions;
    private double totalUsdValue;
    private Account rawAccountData; // Complete unmodified raw data from the API

    public AccountBalanceResponse() {
    }

    public AccountBalanceResponse(List<Balance> positions, double totalUsdValue, Account rawAccountData) {
        this.positions = positions;
        this.totalUsdValue = totalUsdValue;
        this.rawAccountData = rawAccountData;
    }

    public List<Balance> getPositions() {
        return positions;
    }

    public void setPositions(List<Balance> positions) {
        this.positions = positions;
    }

    public double getTotalUsdValue() {
        return totalUsdValue;
    }

    public void setTotalUsdValue(double totalUsdValue) {
        this.totalUsdValue = totalUsdValue;
    }

    public Account getRawAccountData() {
        return rawAccountData;
    }

    public void setRawAccountData(Account rawAccountData) {
        this.rawAccountData = rawAccountData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountBalanceResponse that = (AccountBalanceResponse) o;
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
        return "AccountBalanceResponse{" +
                "positions=" + positions +
                ", totalUsdValue=" + totalUsdValue +
                ", rawAccountData=" + rawAccountData +
                '}';
    }
}
