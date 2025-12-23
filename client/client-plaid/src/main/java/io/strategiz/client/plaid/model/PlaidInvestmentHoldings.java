package io.strategiz.client.plaid.model;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.Holding;
import com.plaid.client.model.Security;
import com.plaid.client.model.Item;

import java.util.List;

/**
 * Investment holdings response from Plaid.
 */
public class PlaidInvestmentHoldings {

    private final List<AccountBase> accounts;
    private final List<Holding> holdings;
    private final List<Security> securities;
    private final Item item;
    private final String requestId;

    public PlaidInvestmentHoldings(
            List<AccountBase> accounts,
            List<Holding> holdings,
            List<Security> securities,
            Item item,
            String requestId) {
        this.accounts = accounts;
        this.holdings = holdings;
        this.securities = securities;
        this.item = item;
        this.requestId = requestId;
    }

    public List<AccountBase> getAccounts() {
        return accounts;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public List<Security> getSecurities() {
        return securities;
    }

    public Item getItem() {
        return item;
    }

    public String getRequestId() {
        return requestId;
    }

    /**
     * Calculate total portfolio value.
     */
    public double getTotalValue() {
        return holdings.stream()
            .mapToDouble(h -> h.getInstitutionValue() != null ? h.getInstitutionValue() : 0.0)
            .sum();
    }

    /**
     * Find security by ID.
     */
    public Security findSecurity(String securityId) {
        return securities.stream()
            .filter(s -> s.getSecurityId().equals(securityId))
            .findFirst()
            .orElse(null);
    }
}
