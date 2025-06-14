package io.strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

/**
 * Model class for Binance US account data
 * This represents account information from the Binance US API
 */
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

    public Account() {
    }

    public Account(int makerCommission, int takerCommission, int buyerCommission, int sellerCommission, boolean canTrade, boolean canWithdraw, boolean canDeposit, long updateTime, String accountType, List<Balance> balances, List<String> permissions) {
        this.makerCommission = makerCommission;
        this.takerCommission = takerCommission;
        this.buyerCommission = buyerCommission;
        this.sellerCommission = sellerCommission;
        this.canTrade = canTrade;
        this.canWithdraw = canWithdraw;
        this.canDeposit = canDeposit;
        this.updateTime = updateTime;
        this.accountType = accountType;
        this.balances = balances;
        this.permissions = permissions;
    }

    public int getMakerCommission() {
        return makerCommission;
    }

    public void setMakerCommission(int makerCommission) {
        this.makerCommission = makerCommission;
    }

    public int getTakerCommission() {
        return takerCommission;
    }

    public void setTakerCommission(int takerCommission) {
        this.takerCommission = takerCommission;
    }

    public int getBuyerCommission() {
        return buyerCommission;
    }

    public void setBuyerCommission(int buyerCommission) {
        this.buyerCommission = buyerCommission;
    }

    public int getSellerCommission() {
        return sellerCommission;
    }

    public void setSellerCommission(int sellerCommission) {
        this.sellerCommission = sellerCommission;
    }

    public boolean isCanTrade() {
        return canTrade;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }

    public boolean isCanWithdraw() {
        return canWithdraw;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public boolean isCanDeposit() {
        return canDeposit;
    }

    public void setCanDeposit(boolean canDeposit) {
        this.canDeposit = canDeposit;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public List<Balance> getBalances() {
        return balances;
    }

    public void setBalances(List<Balance> balances) {
        this.balances = balances;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return makerCommission == account.makerCommission &&
                takerCommission == account.takerCommission &&
                buyerCommission == account.buyerCommission &&
                sellerCommission == account.sellerCommission &&
                canTrade == account.canTrade &&
                canWithdraw == account.canWithdraw &&
                canDeposit == account.canDeposit &&
                updateTime == account.updateTime &&
                Objects.equals(accountType, account.accountType) &&
                Objects.equals(balances, account.balances) &&
                Objects.equals(permissions, account.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(makerCommission, takerCommission, buyerCommission, sellerCommission, canTrade, canWithdraw, canDeposit, updateTime, accountType, balances, permissions);
    }

    @Override
    public String toString() {
        return "Account{" +
                "makerCommission=" + makerCommission +
                ", takerCommission=" + takerCommission +
                ", buyerCommission=" + buyerCommission +
                ", sellerCommission=" + sellerCommission +
                ", canTrade=" + canTrade +
                ", canWithdraw=" + canWithdraw +
                ", canDeposit=" + canDeposit +
                ", updateTime=" + updateTime +
                ", accountType='" + accountType + '\'' +
                ", balances=" + balances +
                ", permissions=" + permissions +
                '}';
    }
}
