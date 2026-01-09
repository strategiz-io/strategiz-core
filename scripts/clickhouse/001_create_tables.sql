-- ClickHouse Cloud Tables for Strategiz
-- Run these in order in ClickHouse Cloud console

-- =============================================================================
-- 1. MARKET DATA TABLE
-- Stores OHLCV price data for stocks, ETFs, and crypto
-- Uses ReplacingMergeTree for upsert behavior (deduplication on symbol+timeframe+timestamp)
-- =============================================================================
CREATE TABLE IF NOT EXISTS market_data (
    symbol String,
    timeframe String,
    timestamp DateTime64(3),
    open_price Decimal64(8),
    high_price Decimal64(8),
    low_price Decimal64(8),
    close_price Decimal64(8),
    volume Decimal64(8),
    vwap Nullable(Decimal64(8)),
    trades Nullable(UInt64),
    change_amount Nullable(Decimal64(8)),
    change_percent Nullable(Decimal64(8)),
    data_source Nullable(String),
    data_quality Nullable(String),
    asset_type Nullable(String),
    exchange Nullable(String),
    collected_at Nullable(DateTime64(3)),
    created_at DateTime64(3) DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree(created_at)
PARTITION BY toYYYYMM(timestamp)
ORDER BY (symbol, timeframe, timestamp)
SETTINGS index_granularity = 8192;

-- =============================================================================
-- 2. COMPANY FUNDAMENTALS TABLE
-- Stores financial metrics (P/E, EPS, revenue, etc.) for stock analysis
-- =============================================================================
CREATE TABLE IF NOT EXISTS company_fundamentals (
    id String,
    symbol String,
    fiscal_period Date,
    period_type String,

    -- Income Statement
    revenue Nullable(Decimal64(8)),
    cost_of_revenue Nullable(Decimal64(8)),
    gross_profit Nullable(Decimal64(8)),
    operating_income Nullable(Decimal64(8)),
    ebitda Nullable(Decimal64(8)),
    net_income Nullable(Decimal64(8)),
    eps_basic Nullable(Decimal64(8)),
    eps_diluted Nullable(Decimal64(8)),

    -- Margins & Profitability
    gross_margin Nullable(Decimal64(8)),
    operating_margin Nullable(Decimal64(8)),
    profit_margin Nullable(Decimal64(8)),
    return_on_equity Nullable(Decimal64(8)),
    return_on_assets Nullable(Decimal64(8)),

    -- Valuation Ratios
    price_to_earnings Nullable(Decimal64(8)),
    price_to_book Nullable(Decimal64(8)),
    price_to_sales Nullable(Decimal64(8)),
    peg_ratio Nullable(Decimal64(8)),
    enterprise_value Nullable(Decimal64(8)),
    ev_to_ebitda Nullable(Decimal64(8)),

    -- Balance Sheet
    total_assets Nullable(Decimal64(8)),
    total_liabilities Nullable(Decimal64(8)),
    shareholders_equity Nullable(Decimal64(8)),
    current_assets Nullable(Decimal64(8)),
    current_liabilities Nullable(Decimal64(8)),
    total_debt Nullable(Decimal64(8)),
    cash_and_equivalents Nullable(Decimal64(8)),

    -- Liquidity & Leverage Ratios
    current_ratio Nullable(Decimal64(8)),
    quick_ratio Nullable(Decimal64(8)),
    debt_to_equity Nullable(Decimal64(8)),
    debt_to_assets Nullable(Decimal64(8)),

    -- Dividends
    dividend_per_share Nullable(Decimal64(8)),
    dividend_yield Nullable(Decimal64(8)),
    payout_ratio Nullable(Decimal64(8)),

    -- Share Info
    shares_outstanding Nullable(Decimal64(8)),
    market_cap Nullable(Decimal64(8)),
    book_value_per_share Nullable(Decimal64(8)),

    -- Growth Metrics
    revenue_growth_yoy Nullable(Decimal64(8)),
    eps_growth_yoy Nullable(Decimal64(8)),

    -- Metadata
    data_source Nullable(String),
    currency Nullable(String),
    collected_at Nullable(DateTime64(3)),
    created_at DateTime64(3) DEFAULT now64(3),
    updated_at DateTime64(3) DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(fiscal_period)
ORDER BY (symbol, period_type, fiscal_period)
SETTINGS index_granularity = 8192;

-- =============================================================================
-- 3. SYMBOL DATA STATUS TABLE
-- Tracks data freshness and quality for each symbol/timeframe combination
-- =============================================================================
CREATE TABLE IF NOT EXISTS symbol_data_status (
    symbol String,
    timeframe String,
    status String,
    earliest_timestamp Nullable(DateTime64(3)),
    latest_timestamp Nullable(DateTime64(3)),
    bar_count Nullable(UInt64),
    last_collection_at Nullable(DateTime64(3)),
    last_error Nullable(String),
    error_count Nullable(UInt32),
    data_source Nullable(String),
    created_at DateTime64(3) DEFAULT now64(3),
    updated_at DateTime64(3) DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (symbol, timeframe)
SETTINGS index_granularity = 8192;

-- =============================================================================
-- 4. JOB EXECUTIONS TABLE
-- Tracks batch job execution history for monitoring and debugging
-- =============================================================================
CREATE TABLE IF NOT EXISTS job_executions (
    execution_id String,
    job_name String,
    job_id Nullable(String),
    status String,
    start_time DateTime64(3),
    end_time Nullable(DateTime64(3)),
    duration_ms Nullable(UInt64),
    symbols_processed Nullable(UInt32),
    data_points_stored Nullable(UInt64),
    error_count Nullable(UInt32),
    error_details Nullable(String),
    timeframes Nullable(String),
    created_at DateTime64(3) DEFAULT now64(3)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(start_time)
ORDER BY (job_name, start_time)
SETTINGS index_granularity = 8192;

-- =============================================================================
-- INDEXES FOR COMMON QUERIES
-- =============================================================================

-- Market data: Fast lookups by symbol and timestamp range
ALTER TABLE market_data ADD INDEX idx_symbol_ts (symbol, timestamp) TYPE minmax GRANULARITY 4;

-- Fundamentals: Fast lookups by symbol
ALTER TABLE company_fundamentals ADD INDEX idx_symbol (symbol) TYPE bloom_filter GRANULARITY 4;

-- Job executions: Fast status lookups
ALTER TABLE job_executions ADD INDEX idx_status (status) TYPE set(10) GRANULARITY 4;
