-- ClickHouse schema for market data
-- Database: ClickHouse Cloud
-- Purpose: High-performance time-series storage for OHLCV market data
-- Created: 2026-01-10
--
-- Run this schema in ClickHouse before starting the application

-- =============================================================================
-- MARKET DATA TABLE (ReplacingMergeTree for upserts)
-- =============================================================================
-- Stores OHLCV bars for all symbols and timeframes
-- Primary key: (symbol, timeframe, timestamp) for efficient time-series queries
-- Engine: ReplacingMergeTree for automatic deduplication

CREATE TABLE IF NOT EXISTS market_data (
    -- Primary identifiers
    symbol String,
    timeframe String,
    timestamp DateTime64(3, 'UTC'),

    -- OHLCV data
    open_price Decimal64(8),
    high_price Decimal64(8),
    low_price Decimal64(8),
    close_price Decimal64(8),
    volume Decimal64(8),

    -- Additional metrics
    vwap Nullable(Decimal64(8)),
    trades Nullable(UInt64),
    change_amount Nullable(Decimal64(8)),
    change_percent Nullable(Decimal64(8)),

    -- Metadata
    data_source String DEFAULT 'ALPACA',
    data_quality String DEFAULT 'HISTORICAL',
    asset_type String DEFAULT 'us_equity',
    exchange Nullable(String),

    -- Audit fields
    collected_at Nullable(DateTime64(3, 'UTC')),
    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree(created_at)
PARTITION BY (symbol, toYYYYMM(timestamp))
PRIMARY KEY (symbol, timeframe, timestamp)
ORDER BY (symbol, timeframe, timestamp)
SETTINGS index_granularity = 8192;

-- Create indexes for common query patterns
-- Note: ClickHouse uses sparse indexes, so these improve query performance

-- Index for timeframe filtering
CREATE INDEX IF NOT EXISTS idx_timeframe ON market_data (timeframe) TYPE minmax GRANULARITY 4;

-- Index for date range queries
CREATE INDEX IF NOT EXISTS idx_timestamp ON market_data (timestamp) TYPE minmax GRANULARITY 4;

-- =============================================================================
-- JOB EXECUTIONS TABLE
-- =============================================================================
-- Tracks batch job execution history (backfill, incremental updates)

CREATE TABLE IF NOT EXISTS job_executions (
    execution_id String,
    job_name String,
    start_time DateTime64(3, 'UTC'),
    end_time Nullable(DateTime64(3, 'UTC')),
    duration_ms Nullable(UInt64),
    status String,  -- SUCCESS, FAILED, RUNNING
    symbols_processed UInt32 DEFAULT 0,
    data_points_stored UInt64 DEFAULT 0,
    error_count UInt32 DEFAULT 0,
    error_details Nullable(String),  -- JSON array of errors
    timeframes Nullable(String),     -- JSON array of timeframes
    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(start_time)
PRIMARY KEY (job_name, start_time)
ORDER BY (job_name, start_time)
SETTINGS index_granularity = 8192;

-- =============================================================================
-- SYMBOL DATA STATUS TABLE
-- =============================================================================
-- Tracks data freshness for each symbol/timeframe combination

CREATE TABLE IF NOT EXISTS symbol_data_status (
    symbol String,
    timeframe String,
    last_update DateTime64(3, 'UTC'),
    last_bar_timestamp Nullable(DateTime64(3, 'UTC')),
    record_count UInt64 DEFAULT 0,
    consecutive_failures UInt32 DEFAULT 0,
    last_error Nullable(String),
    status String DEFAULT 'ACTIVE',  -- ACTIVE, STALE, FAILED
    updated_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(last_update)
PRIMARY KEY (symbol, timeframe, last_update)
ORDER BY (symbol, timeframe, last_update)
SETTINGS index_granularity = 8192;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================
-- Use these to verify the schema and check data

-- Check tables exist
-- SHOW TABLES;

-- Check table schemas
-- DESCRIBE TABLE market_data;
-- DESCRIBE TABLE job_executions;
-- DESCRIBE TABLE symbol_data_status;

-- Check data counts
-- SELECT COUNT(*) FROM market_data;
-- SELECT COUNT(DISTINCT symbol) AS symbols FROM market_data;
-- SELECT COUNT(DISTINCT timeframe) AS timeframes FROM market_data;

-- Sample data queries
-- SELECT * FROM market_data WHERE symbol = 'AAPL' ORDER BY timestamp DESC LIMIT 10;
-- SELECT * FROM job_executions ORDER BY start_time DESC LIMIT 10;

-- Check date range of data
-- SELECT
--     symbol,
--     timeframe,
--     MIN(timestamp) AS earliest,
--     MAX(timestamp) AS latest,
--     COUNT(*) AS bars
-- FROM market_data
-- GROUP BY symbol, timeframe
-- ORDER BY symbol, timeframe
-- LIMIT 20;
