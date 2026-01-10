-- ============================================================================
-- Company Fundamentals - TimescaleDB Hypertable
-- ============================================================================
--
-- This table stores company fundamental financial data (income statement,
-- balance sheet, ratios, etc.) fetched from Yahoo Finance.
--
-- Key Features:
-- - Hypertable partitioned by fiscal_period for efficient time-series queries
-- - Stores both current and historical fundamental data
-- - Supports quarterly, annual, and TTM (trailing twelve months) periods
-- - Includes 40+ financial metrics covering all major areas
--
-- Data Source: Yahoo Finance (free, unofficial API)
-- Update Frequency: Daily (via scheduled batch job)
-- Retention: Indefinite (fundamentals don't expire like OHLCV data)
--
-- ============================================================================

CREATE TABLE company_fundamentals (
    -- Primary Key
    id VARCHAR(100) PRIMARY KEY,  -- Format: {symbol}_{fiscal_period}_{period_type}
    symbol VARCHAR(20) NOT NULL,
    fiscal_period DATE NOT NULL,   -- The reporting period end date (e.g., 2024-03-31 for Q1 2024)
    period_type VARCHAR(10) NOT NULL,  -- 'QUARTERLY', 'ANNUAL', 'TTM'

    -- Income Statement Metrics
    revenue NUMERIC(20,2),              -- Total revenue/sales (in dollars)
    cost_of_revenue NUMERIC(20,2),      -- Cost of goods sold
    gross_profit NUMERIC(20,2),         -- Revenue - COGS
    operating_income NUMERIC(20,2),     -- EBIT (Earnings Before Interest & Taxes)
    ebitda NUMERIC(20,2),               -- Earnings Before Interest, Taxes, Depreciation, Amortization
    net_income NUMERIC(20,2),           -- Bottom line profit
    eps_basic NUMERIC(12,4),            -- Earnings Per Share (basic)
    eps_diluted NUMERIC(12,4),          -- Earnings Per Share (diluted - preferred for valuation)

    -- Margins & Profitability Ratios (percentages)
    gross_margin NUMERIC(8,4),          -- (Gross Profit / Revenue) * 100
    operating_margin NUMERIC(8,4),      -- (Operating Income / Revenue) * 100
    profit_margin NUMERIC(8,4),         -- (Net Income / Revenue) * 100
    return_on_equity NUMERIC(8,4),      -- ROE % - net income / shareholders' equity
    return_on_assets NUMERIC(8,4),      -- ROA % - net income / total assets

    -- Valuation Ratios (market-based)
    price_to_earnings NUMERIC(12,4),    -- P/E ratio (current price / EPS)
    price_to_book NUMERIC(12,4),        -- P/B ratio (price / book value per share)
    price_to_sales NUMERIC(12,4),       -- P/S ratio (market cap / revenue)
    peg_ratio NUMERIC(12,4),            -- P/E to Growth ratio
    enterprise_value NUMERIC(20,2),     -- Market cap + debt - cash
    ev_to_ebitda NUMERIC(12,4),         -- EV / EBITDA multiple

    -- Balance Sheet Metrics
    total_assets NUMERIC(20,2),         -- Total company assets
    total_liabilities NUMERIC(20,2),    -- Total company liabilities
    shareholders_equity NUMERIC(20,2),  -- Total equity (assets - liabilities)
    current_assets NUMERIC(20,2),       -- Assets convertible to cash within 1 year
    current_liabilities NUMERIC(20,2),  -- Liabilities due within 1 year
    total_debt NUMERIC(20,2),           -- Short-term + long-term debt
    cash_and_equivalents NUMERIC(20,2), -- Cash and liquid assets

    -- Liquidity & Leverage Ratios
    current_ratio NUMERIC(8,4),         -- Current assets / current liabilities
    quick_ratio NUMERIC(8,4),           -- (Current assets - inventory) / current liabilities
    debt_to_equity NUMERIC(8,4),        -- Total debt / shareholders' equity
    debt_to_assets NUMERIC(8,4),        -- Total debt / total assets

    -- Dividend Information
    dividend_per_share NUMERIC(12,4),   -- Annual dividend per share
    dividend_yield NUMERIC(8,4),        -- (Dividend / price) * 100
    payout_ratio NUMERIC(8,4),          -- (Dividends / net income) * 100

    -- Share Information
    shares_outstanding NUMERIC(18,0),   -- Total shares outstanding
    market_cap NUMERIC(20,2),           -- Share price * shares outstanding
    book_value_per_share NUMERIC(12,4), -- Shareholders' equity / shares outstanding

    -- Growth Metrics (Year-over-Year percentages)
    revenue_growth_yoy NUMERIC(8,4),    -- % change in revenue vs same quarter/year prior
    eps_growth_yoy NUMERIC(8,4),        -- % change in EPS vs same quarter/year prior

    -- Metadata
    data_source VARCHAR(50) DEFAULT 'YAHOO_FINANCE',  -- Data provider
    currency VARCHAR(3) DEFAULT 'USD',                 -- Currency code (ISO 4217)

    -- Timestamps
    collected_at TIMESTAMP NOT NULL,    -- When this data was collected from Yahoo Finance
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Convert to TimescaleDB hypertable (partitioned by fiscal_period)
-- This enables efficient time-series queries and automatic data retention policies
SELECT create_hypertable('company_fundamentals', 'fiscal_period');

-- Indexes for common query patterns
CREATE INDEX idx_fundamentals_symbol ON company_fundamentals(symbol, fiscal_period DESC);
CREATE INDEX idx_fundamentals_period_type ON company_fundamentals(period_type, fiscal_period DESC);

-- Stock screener indexes (for filtering by metrics)
CREATE INDEX idx_fundamentals_pe ON company_fundamentals(price_to_earnings)
    WHERE price_to_earnings IS NOT NULL;

CREATE INDEX idx_fundamentals_dividend_yield ON company_fundamentals(dividend_yield)
    WHERE dividend_yield IS NOT NULL;

-- ============================================================================
-- Example Queries
-- ============================================================================

-- Get latest fundamentals for a symbol
-- SELECT * FROM company_fundamentals
-- WHERE symbol = 'AAPL'
-- ORDER BY fiscal_period DESC, period_type DESC
-- LIMIT 1;

-- Get quarterly history for a symbol
-- SELECT symbol, fiscal_period, period_type, eps_diluted, revenue, profit_margin
-- FROM company_fundamentals
-- WHERE symbol = 'AAPL' AND period_type = 'QUARTERLY'
-- ORDER BY fiscal_period DESC
-- LIMIT 8;

-- Stock screener: Find stocks with P/E < 20 and dividend yield > 2%
-- SELECT DISTINCT symbol, price_to_earnings, dividend_yield
-- FROM company_fundamentals
-- WHERE period_type = 'TTM'
--   AND price_to_earnings < 20
--   AND dividend_yield > 2.0
-- ORDER BY dividend_yield DESC;

-- Calculate revenue growth trend
-- SELECT symbol, fiscal_period, revenue, revenue_growth_yoy
-- FROM company_fundamentals
-- WHERE symbol = 'AAPL' AND period_type = 'ANNUAL'
-- ORDER BY fiscal_period DESC
-- LIMIT 5;
