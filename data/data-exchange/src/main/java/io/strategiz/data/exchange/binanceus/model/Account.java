package io.strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Model class for Binance US account data
 * This represents account information from the Binance US API
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {
    private int makerCommission;
    private int takerCommission;
    private int buyerCommission;
    private int sellerCommission;
    private boolean canTrade;
    private boolean canWithdraw;
    private boolean canDeposit;
    private long updateTime;
    private String accountType;
    private List<Balance> balances;
    private List<String> permissions;
}
