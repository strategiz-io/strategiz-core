package io.strategiz.service.portfolio.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service model for portfolio data.
 */
public class PortfolioResponse {
    
    private String userId;
    private String provider;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private List<AssetHolding> assets;
    private Map<String, String> metadata;
    private boolean success;
    private String errorMessage;

    public PortfolioResponse() {
    }

    public PortfolioResponse(String userId, String provider, BigDecimal totalValue, BigDecimal cashBalance,
                           List<AssetHolding> assets, Map<String, String> metadata, boolean success, String errorMessage) {
        this.userId = userId;
        this.provider = provider;
        this.totalValue = totalValue;
        this.cashBalance = cashBalance;
        this.assets = assets;
        this.metadata = metadata;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public List<AssetHolding> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetHolding> assets) {
        this.assets = assets;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioResponse that = (PortfolioResponse) o;
        return success == that.success &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(provider, that.provider) &&
               Objects.equals(totalValue, that.totalValue) &&
               Objects.equals(cashBalance, that.cashBalance) &&
               Objects.equals(assets, that.assets) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, provider, totalValue, cashBalance, assets, metadata, success, errorMessage);
    }

    @Override
    public String toString() {
        return "PortfolioResponse{" +
               "userId='" + userId + '\'' +
               ", provider='" + provider + '\'' +
               ", totalValue=" + totalValue +
               ", cashBalance=" + cashBalance +
               ", assets=" + assets +
               ", metadata=" + metadata +
               ", success=" + success +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String provider;
        private BigDecimal totalValue;
        private BigDecimal cashBalance;
        private List<AssetHolding> assets;
        private Map<String, String> metadata;
        private boolean success;
        private String errorMessage;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder totalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }

        public Builder cashBalance(BigDecimal cashBalance) {
            this.cashBalance = cashBalance;
            return this;
        }

        public Builder assets(List<AssetHolding> assets) {
            this.assets = assets;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public PortfolioResponse build() {
            return new PortfolioResponse(userId, provider, totalValue, cashBalance, assets, metadata, success, errorMessage);
        }
    }
}
