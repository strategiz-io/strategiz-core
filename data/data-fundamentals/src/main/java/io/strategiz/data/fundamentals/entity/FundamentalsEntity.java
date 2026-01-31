package io.strategiz.data.fundamentals.entity;

import io.strategiz.data.fundamentals.constants.PeriodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity for storing company fundamentals data. Maps to the 'company_fundamentals' table.
 *
 * Stores comprehensive financial metrics including: - Income statement data (revenue,
 * EBITDA, EPS, net income) - Balance sheet data (assets, liabilities, equity, debt) -
 * Financial ratios (P/E, P/B, P/S, margins, ROE, ROA) - Dividend information - Growth
 * metrics
 *
 * Data Source: Yahoo Finance (free, unofficial API) Update Frequency: Daily via scheduled
 * batch job
 *
 * @see io.strategiz.data.fundamentals.repository.FundamentalsRepository
 */
@Entity
@Table(name = "company_fundamentals",
		indexes = { @Index(name = "idx_fundamentals_symbol", columnList = "symbol, fiscal_period DESC"),
				@Index(name = "idx_fundamentals_period_type", columnList = "period_type, fiscal_period DESC"),
				@Index(name = "idx_fundamentals_pe", columnList = "price_to_earnings"),
				@Index(name = "idx_fundamentals_dividend_yield", columnList = "dividend_yield") })
public class FundamentalsEntity {

	// =========================================================================
	// Primary Key & Identifiers
	// =========================================================================

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id; // Format: {symbol}_{fiscal_period}_{period_type}

	@NotBlank
	@Column(name = "symbol", length = 20, nullable = false)
	private String symbol;

	@NotNull
	@Column(name = "fiscal_period", nullable = false)
	private LocalDate fiscalPeriod;

	@NotBlank
	@Column(name = "period_type", length = 10, nullable = false)
	private String periodType; // QUARTERLY, ANNUAL, TTM

	// =========================================================================
	// Income Statement Metrics
	// =========================================================================

	@Column(name = "revenue", precision = 20, scale = 2)
	private BigDecimal revenue;

	@Column(name = "cost_of_revenue", precision = 20, scale = 2)
	private BigDecimal costOfRevenue;

	@Column(name = "gross_profit", precision = 20, scale = 2)
	private BigDecimal grossProfit;

	@Column(name = "operating_income", precision = 20, scale = 2)
	private BigDecimal operatingIncome;

	@Column(name = "ebitda", precision = 20, scale = 2)
	private BigDecimal ebitda;

	@Column(name = "net_income", precision = 20, scale = 2)
	private BigDecimal netIncome;

	@Column(name = "eps_basic", precision = 12, scale = 4)
	private BigDecimal epsBasic;

	@Column(name = "eps_diluted", precision = 12, scale = 4)
	private BigDecimal epsDiluted;

	// =========================================================================
	// Margins & Profitability Ratios
	// =========================================================================

	@Column(name = "gross_margin", precision = 8, scale = 4)
	private BigDecimal grossMargin;

	@Column(name = "operating_margin", precision = 8, scale = 4)
	private BigDecimal operatingMargin;

	@Column(name = "profit_margin", precision = 8, scale = 4)
	private BigDecimal profitMargin;

	@Column(name = "return_on_equity", precision = 8, scale = 4)
	private BigDecimal returnOnEquity;

	@Column(name = "return_on_assets", precision = 8, scale = 4)
	private BigDecimal returnOnAssets;

	// =========================================================================
	// Valuation Ratios
	// =========================================================================

	@Column(name = "price_to_earnings", precision = 12, scale = 4)
	private BigDecimal priceToEarnings;

	@Column(name = "price_to_book", precision = 12, scale = 4)
	private BigDecimal priceToBook;

	@Column(name = "price_to_sales", precision = 12, scale = 4)
	private BigDecimal priceToSales;

	@Column(name = "peg_ratio", precision = 12, scale = 4)
	private BigDecimal pegRatio;

	@Column(name = "enterprise_value", precision = 20, scale = 2)
	private BigDecimal enterpriseValue;

	@Column(name = "ev_to_ebitda", precision = 12, scale = 4)
	private BigDecimal evToEbitda;

	// =========================================================================
	// Balance Sheet Metrics
	// =========================================================================

	@Column(name = "total_assets", precision = 20, scale = 2)
	private BigDecimal totalAssets;

	@Column(name = "total_liabilities", precision = 20, scale = 2)
	private BigDecimal totalLiabilities;

	@Column(name = "shareholders_equity", precision = 20, scale = 2)
	private BigDecimal shareholdersEquity;

	@Column(name = "current_assets", precision = 20, scale = 2)
	private BigDecimal currentAssets;

	@Column(name = "current_liabilities", precision = 20, scale = 2)
	private BigDecimal currentLiabilities;

	@Column(name = "total_debt", precision = 20, scale = 2)
	private BigDecimal totalDebt;

	@Column(name = "cash_and_equivalents", precision = 20, scale = 2)
	private BigDecimal cashAndEquivalents;

	// =========================================================================
	// Liquidity & Leverage Ratios
	// =========================================================================

	@Column(name = "current_ratio", precision = 8, scale = 4)
	private BigDecimal currentRatio;

	@Column(name = "quick_ratio", precision = 8, scale = 4)
	private BigDecimal quickRatio;

	@Column(name = "debt_to_equity", precision = 8, scale = 4)
	private BigDecimal debtToEquity;

	@Column(name = "debt_to_assets", precision = 8, scale = 4)
	private BigDecimal debtToAssets;

	// =========================================================================
	// Dividend Information
	// =========================================================================

	@Column(name = "dividend_per_share", precision = 12, scale = 4)
	private BigDecimal dividendPerShare;

	@Column(name = "dividend_yield", precision = 8, scale = 4)
	private BigDecimal dividendYield;

	@Column(name = "payout_ratio", precision = 8, scale = 4)
	private BigDecimal payoutRatio;

	// =========================================================================
	// Share Information
	// =========================================================================

	@Column(name = "shares_outstanding", precision = 18, scale = 0)
	private BigDecimal sharesOutstanding;

	@Column(name = "market_cap", precision = 20, scale = 2)
	private BigDecimal marketCap;

	@Column(name = "book_value_per_share", precision = 12, scale = 4)
	private BigDecimal bookValuePerShare;

	// =========================================================================
	// Growth Metrics
	// =========================================================================

	@Column(name = "revenue_growth_yoy", precision = 8, scale = 4)
	private BigDecimal revenueGrowthYoy;

	@Column(name = "eps_growth_yoy", precision = 8, scale = 4)
	private BigDecimal epsGrowthYoy;

	// =========================================================================
	// Metadata
	// =========================================================================

	@Column(name = "data_source", length = 50)
	private String dataSource = "YAHOO_FINANCE";

	@Column(name = "currency", length = 3)
	private String currency = "USD";

	// =========================================================================
	// Timestamps
	// =========================================================================

	@NotNull
	@Column(name = "collected_at", nullable = false)
	private Instant collectedAt;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	// =========================================================================
	// Constructors
	// =========================================================================

	/**
	 * Default constructor for JPA.
	 */
	public FundamentalsEntity() {
		this.currency = "USD";
		this.dataSource = "YAHOO_FINANCE";
	}

	/**
	 * Constructor with required fields.
	 */
	public FundamentalsEntity(String symbol, LocalDate fiscalPeriod, String periodType) {
		this();
		this.symbol = symbol;
		this.fiscalPeriod = fiscalPeriod;
		this.periodType = PeriodType.normalize(periodType);
		this.id = generateId(symbol, fiscalPeriod, this.periodType);
	}

	// =========================================================================
	// Lifecycle Callbacks
	// =========================================================================

	@PrePersist
	protected void onCreate() {
		if (id == null) {
			id = generateId(symbol, fiscalPeriod, periodType);
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (updatedAt == null) {
			updatedAt = Instant.now();
		}
		if (collectedAt == null) {
			collectedAt = Instant.now();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}

	// =========================================================================
	// Helper Methods
	// =========================================================================

	/**
	 * Generate unique ID from symbol, fiscal period, and period type. Format:
	 * {symbol}_{YYYY-MM-DD}_{periodType}
	 */
	private static String generateId(String symbol, LocalDate fiscalPeriod, String periodType) {
		if (symbol == null || fiscalPeriod == null || periodType == null) {
			throw new IllegalArgumentException("Symbol, fiscalPeriod, and periodType are required for ID generation");
		}
		return String.format("%s_%s_%s", symbol, fiscalPeriod, periodType);
	}

	// =========================================================================
	// Getters and Setters
	// =========================================================================

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public LocalDate getFiscalPeriod() {
		return fiscalPeriod;
	}

	public void setFiscalPeriod(LocalDate fiscalPeriod) {
		this.fiscalPeriod = fiscalPeriod;
	}

	public String getPeriodType() {
		return periodType;
	}

	public void setPeriodType(String periodType) {
		this.periodType = periodType;
	}

	public BigDecimal getRevenue() {
		return revenue;
	}

	public void setRevenue(BigDecimal revenue) {
		this.revenue = revenue;
	}

	public BigDecimal getCostOfRevenue() {
		return costOfRevenue;
	}

	public void setCostOfRevenue(BigDecimal costOfRevenue) {
		this.costOfRevenue = costOfRevenue;
	}

	public BigDecimal getGrossProfit() {
		return grossProfit;
	}

	public void setGrossProfit(BigDecimal grossProfit) {
		this.grossProfit = grossProfit;
	}

	public BigDecimal getOperatingIncome() {
		return operatingIncome;
	}

	public void setOperatingIncome(BigDecimal operatingIncome) {
		this.operatingIncome = operatingIncome;
	}

	public BigDecimal getEbitda() {
		return ebitda;
	}

	public void setEbitda(BigDecimal ebitda) {
		this.ebitda = ebitda;
	}

	public BigDecimal getNetIncome() {
		return netIncome;
	}

	public void setNetIncome(BigDecimal netIncome) {
		this.netIncome = netIncome;
	}

	public BigDecimal getEpsBasic() {
		return epsBasic;
	}

	public void setEpsBasic(BigDecimal epsBasic) {
		this.epsBasic = epsBasic;
	}

	public BigDecimal getEpsDiluted() {
		return epsDiluted;
	}

	public void setEpsDiluted(BigDecimal epsDiluted) {
		this.epsDiluted = epsDiluted;
	}

	public BigDecimal getGrossMargin() {
		return grossMargin;
	}

	public void setGrossMargin(BigDecimal grossMargin) {
		this.grossMargin = grossMargin;
	}

	public BigDecimal getOperatingMargin() {
		return operatingMargin;
	}

	public void setOperatingMargin(BigDecimal operatingMargin) {
		this.operatingMargin = operatingMargin;
	}

	public BigDecimal getProfitMargin() {
		return profitMargin;
	}

	public void setProfitMargin(BigDecimal profitMargin) {
		this.profitMargin = profitMargin;
	}

	public BigDecimal getReturnOnEquity() {
		return returnOnEquity;
	}

	public void setReturnOnEquity(BigDecimal returnOnEquity) {
		this.returnOnEquity = returnOnEquity;
	}

	public BigDecimal getReturnOnAssets() {
		return returnOnAssets;
	}

	public void setReturnOnAssets(BigDecimal returnOnAssets) {
		this.returnOnAssets = returnOnAssets;
	}

	public BigDecimal getPriceToEarnings() {
		return priceToEarnings;
	}

	public void setPriceToEarnings(BigDecimal priceToEarnings) {
		this.priceToEarnings = priceToEarnings;
	}

	public BigDecimal getPriceToBook() {
		return priceToBook;
	}

	public void setPriceToBook(BigDecimal priceToBook) {
		this.priceToBook = priceToBook;
	}

	public BigDecimal getPriceToSales() {
		return priceToSales;
	}

	public void setPriceToSales(BigDecimal priceToSales) {
		this.priceToSales = priceToSales;
	}

	public BigDecimal getPegRatio() {
		return pegRatio;
	}

	public void setPegRatio(BigDecimal pegRatio) {
		this.pegRatio = pegRatio;
	}

	public BigDecimal getEnterpriseValue() {
		return enterpriseValue;
	}

	public void setEnterpriseValue(BigDecimal enterpriseValue) {
		this.enterpriseValue = enterpriseValue;
	}

	public BigDecimal getEvToEbitda() {
		return evToEbitda;
	}

	public void setEvToEbitda(BigDecimal evToEbitda) {
		this.evToEbitda = evToEbitda;
	}

	public BigDecimal getTotalAssets() {
		return totalAssets;
	}

	public void setTotalAssets(BigDecimal totalAssets) {
		this.totalAssets = totalAssets;
	}

	public BigDecimal getTotalLiabilities() {
		return totalLiabilities;
	}

	public void setTotalLiabilities(BigDecimal totalLiabilities) {
		this.totalLiabilities = totalLiabilities;
	}

	public BigDecimal getShareholdersEquity() {
		return shareholdersEquity;
	}

	public void setShareholdersEquity(BigDecimal shareholdersEquity) {
		this.shareholdersEquity = shareholdersEquity;
	}

	public BigDecimal getCurrentAssets() {
		return currentAssets;
	}

	public void setCurrentAssets(BigDecimal currentAssets) {
		this.currentAssets = currentAssets;
	}

	public BigDecimal getCurrentLiabilities() {
		return currentLiabilities;
	}

	public void setCurrentLiabilities(BigDecimal currentLiabilities) {
		this.currentLiabilities = currentLiabilities;
	}

	public BigDecimal getTotalDebt() {
		return totalDebt;
	}

	public void setTotalDebt(BigDecimal totalDebt) {
		this.totalDebt = totalDebt;
	}

	public BigDecimal getCashAndEquivalents() {
		return cashAndEquivalents;
	}

	public void setCashAndEquivalents(BigDecimal cashAndEquivalents) {
		this.cashAndEquivalents = cashAndEquivalents;
	}

	public BigDecimal getCurrentRatio() {
		return currentRatio;
	}

	public void setCurrentRatio(BigDecimal currentRatio) {
		this.currentRatio = currentRatio;
	}

	public BigDecimal getQuickRatio() {
		return quickRatio;
	}

	public void setQuickRatio(BigDecimal quickRatio) {
		this.quickRatio = quickRatio;
	}

	public BigDecimal getDebtToEquity() {
		return debtToEquity;
	}

	public void setDebtToEquity(BigDecimal debtToEquity) {
		this.debtToEquity = debtToEquity;
	}

	public BigDecimal getDebtToAssets() {
		return debtToAssets;
	}

	public void setDebtToAssets(BigDecimal debtToAssets) {
		this.debtToAssets = debtToAssets;
	}

	public BigDecimal getDividendPerShare() {
		return dividendPerShare;
	}

	public void setDividendPerShare(BigDecimal dividendPerShare) {
		this.dividendPerShare = dividendPerShare;
	}

	public BigDecimal getDividendYield() {
		return dividendYield;
	}

	public void setDividendYield(BigDecimal dividendYield) {
		this.dividendYield = dividendYield;
	}

	public BigDecimal getPayoutRatio() {
		return payoutRatio;
	}

	public void setPayoutRatio(BigDecimal payoutRatio) {
		this.payoutRatio = payoutRatio;
	}

	public BigDecimal getSharesOutstanding() {
		return sharesOutstanding;
	}

	public void setSharesOutstanding(BigDecimal sharesOutstanding) {
		this.sharesOutstanding = sharesOutstanding;
	}

	public BigDecimal getMarketCap() {
		return marketCap;
	}

	public void setMarketCap(BigDecimal marketCap) {
		this.marketCap = marketCap;
	}

	public BigDecimal getBookValuePerShare() {
		return bookValuePerShare;
	}

	public void setBookValuePerShare(BigDecimal bookValuePerShare) {
		this.bookValuePerShare = bookValuePerShare;
	}

	public BigDecimal getRevenueGrowthYoy() {
		return revenueGrowthYoy;
	}

	public void setRevenueGrowthYoy(BigDecimal revenueGrowthYoy) {
		this.revenueGrowthYoy = revenueGrowthYoy;
	}

	public BigDecimal getEpsGrowthYoy() {
		return epsGrowthYoy;
	}

	public void setEpsGrowthYoy(BigDecimal epsGrowthYoy) {
		this.epsGrowthYoy = epsGrowthYoy;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Instant getCollectedAt() {
		return collectedAt;
	}

	public void setCollectedAt(Instant collectedAt) {
		this.collectedAt = collectedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	// =========================================================================
	// Object Methods
	// =========================================================================

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FundamentalsEntity that = (FundamentalsEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return String.format("FundamentalsTimescaleEntity[%s %s %s: EPS=%.2f, PE=%.2f, Revenue=%.0f, MarketCap=%.0f]",
				symbol, fiscalPeriod, periodType, epsDiluted != null ? epsDiluted : BigDecimal.ZERO,
				priceToEarnings != null ? priceToEarnings : BigDecimal.ZERO,
				revenue != null ? revenue : BigDecimal.ZERO, marketCap != null ? marketCap : BigDecimal.ZERO);
	}

}
