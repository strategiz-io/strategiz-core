package io.strategiz.client.yahoofinance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Aggregated fundamental data from Yahoo Finance.
 *
 * This class combines data from multiple Yahoo Finance API endpoints:
 * - Financial Data (ratios, margins, profitability)
 * - Key Statistics (valuation, shares, market cap)
 * - Balance Sheet (assets, liabilities, equity)
 * - Income Statement (revenue, earnings, expenses)
 * - Cash Flow (operating, investing, financing cash flows)
 *
 * Yahoo Finance provides this data through their unofficial quote summary API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooFundamentals {

    private String symbol;
    private YahooFinancialData financialData;
    private YahooKeyStatistics keyStatistics;
    private YahooBalanceSheet balanceSheet;
    private YahooIncomeStatement incomeStatement;

    public YahooFundamentals() {
    }

    public YahooFundamentals(String symbol) {
        this.symbol = symbol;
    }

    // Getters and Setters

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public YahooFinancialData getFinancialData() {
        return financialData;
    }

    public void setFinancialData(YahooFinancialData financialData) {
        this.financialData = financialData;
    }

    public YahooKeyStatistics getKeyStatistics() {
        return keyStatistics;
    }

    public void setKeyStatistics(YahooKeyStatistics keyStatistics) {
        this.keyStatistics = keyStatistics;
    }

    public YahooBalanceSheet getBalanceSheet() {
        return balanceSheet;
    }

    public void setBalanceSheet(YahooBalanceSheet balanceSheet) {
        this.balanceSheet = balanceSheet;
    }

    public YahooIncomeStatement getIncomeStatement() {
        return incomeStatement;
    }

    public void setIncomeStatement(YahooIncomeStatement incomeStatement) {
        this.incomeStatement = incomeStatement;
    }

    @Override
    public String toString() {
        return String.format("YahooFundamentals[%s]", symbol);
    }
}
