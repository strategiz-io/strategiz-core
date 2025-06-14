package io.strategiz.data.exchange.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Model class representing a Coinbase account balance
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Balance {
    private String amount;
    private String currency;

    // Additional fields for calculated values (not part of raw API response)
    private double amountValue;
    private double usdValue;

    public Balance() {
    }

    public Balance(String amount, String currency, double amountValue, double usdValue) {
        this.amount = amount;
        this.currency = currency;
        this.amountValue = amountValue;
        this.usdValue = usdValue;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getAmountValue() {
        return amountValue;
    }

    public void setAmountValue(double amountValue) {
        this.amountValue = amountValue;
    }

    public double getUsdValue() {
        return usdValue;
    }

    public void setUsdValue(double usdValue) {
        this.usdValue = usdValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance = (Balance) o;
        return Double.compare(balance.amountValue, amountValue) == 0 &&
                Double.compare(balance.usdValue, usdValue) == 0 &&
                Objects.equals(amount, balance.amount) &&
                Objects.equals(currency, balance.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency, amountValue, usdValue);
    }

    @Override
    public String toString() {
        return "Balance{" +
                "amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", amountValue=" + amountValue +
                ", usdValue=" + usdValue +
                '}';
    }
}
