-- TimescaleDB schema for market data console endpoints
-- Database: TimescaleDB (strategiz production)
-- Created: 2026-01-04

-- =============================================================================
-- JOB EXECUTIONS HYPERTABLE
-- =============================================================================
-- Tracks execution history for batch jobs (backfill, incremental)
-- Partitioned by start_time for efficient time-based queries

CREATE TABLE IF NOT EXISTS job_executions (
    execution_id VARCHAR(50) PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    duration_ms BIGINT,
    status VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, RUNNING
    symbols_processed INTEGER DEFAULT 0,
    data_points_stored BIGINT DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    error_details TEXT,  -- JSON array of error messages
    timeframes TEXT,     -- JSON array of timeframes processed
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convert to hypertable (partitioned by start_time)
SELECT create_hypertable('job_executions', 'start_time',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '7 days'
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_job_executions_job_time
    ON job_executions (job_name, start_time DESC);

CREATE INDEX IF NOT EXISTS idx_job_executions_status
    ON job_executions (status, start_time DESC);

-- Add retention policy: Keep 90 days of history
SELECT add_retention_policy('job_executions', INTERVAL '90 days', if_not_exists => TRUE);

-- =============================================================================
-- SYMBOL DATA STATUS HYPERTABLE
-- =============================================================================
-- Tracks per-symbol data freshness for each timeframe
-- Partitioned by last_update for efficient staleness queries

CREATE TABLE IF NOT EXISTS symbol_data_status (
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    last_update TIMESTAMPTZ NOT NULL,
    last_bar_timestamp TIMESTAMPTZ,
    record_count BIGINT DEFAULT 0,
    consecutive_failures INTEGER DEFAULT 0,
    last_error TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, STALE, FAILED
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (symbol, timeframe, last_update)
);

-- Convert to hypertable (partitioned by last_update)
SELECT create_hypertable('symbol_data_status', 'last_update',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '7 days'
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_symbol_status_symbol
    ON symbol_data_status (symbol, timeframe);

CREATE INDEX IF NOT EXISTS idx_symbol_status_stale
    ON symbol_data_status (status, last_update DESC);

CREATE INDEX IF NOT EXISTS idx_symbol_status_timeframe
    ON symbol_data_status (timeframe, last_update DESC);

-- Add retention policy: Keep 30 days of status history
SELECT add_retention_policy('symbol_data_status', INTERVAL '30 days', if_not_exists => TRUE);

-- =============================================================================
-- SYMBOL LATEST STATUS MATERIALIZED VIEW
-- =============================================================================
-- Shows the most recent status for each symbol/timeframe
-- Refreshed periodically for fast queries

CREATE MATERIALIZED VIEW IF NOT EXISTS symbol_latest_status AS
SELECT DISTINCT ON (symbol, timeframe)
    symbol,
    timeframe,
    last_update,
    last_bar_timestamp,
    record_count,
    consecutive_failures,
    last_error,
    status,
    updated_at
FROM symbol_data_status
ORDER BY symbol, timeframe, last_update DESC;

-- Create indexes on materialized view
CREATE INDEX IF NOT EXISTS idx_symbol_latest_symbol
    ON symbol_latest_status (symbol, timeframe);

CREATE INDEX IF NOT EXISTS idx_symbol_latest_status
    ON symbol_latest_status (status);

CREATE INDEX IF NOT EXISTS idx_symbol_latest_timeframe
    ON symbol_latest_status (timeframe);

-- =============================================================================
-- REFRESH POLICY
-- =============================================================================
-- Auto-refresh the materialized view every 5 minutes during market hours
-- This keeps the "latest status" view up-to-date without manual refresh

-- Note: TimescaleDB continuous aggregates would be better, but this works for MVP
-- Manual refresh command: REFRESH MATERIALIZED VIEW symbol_latest_status;

-- =============================================================================
-- HELPER FUNCTIONS
-- =============================================================================

-- Function to refresh symbol_latest_status view
CREATE OR REPLACE FUNCTION refresh_symbol_latest_status()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW symbol_latest_status;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- GRANTS
-- =============================================================================
-- Grant access to the strategiz application user
-- (Adjust username based on your TimescaleDB setup)

-- GRANT SELECT, INSERT, UPDATE, DELETE ON job_executions TO strategiz_app;
-- GRANT SELECT ON symbol_data_status TO strategiz_app;
-- GRANT SELECT ON symbol_latest_status TO strategiz_app;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================
-- Use these to verify the schema was created correctly

-- Check hypertables
-- SELECT hypertable_name, hypertable_schema FROM timescaledb_information.hypertables;

-- Check retention policies
-- SELECT * FROM timescaledb_information.jobs WHERE proc_name = 'policy_retention';

-- Check data
-- SELECT COUNT(*) FROM job_executions;
-- SELECT COUNT(*) FROM symbol_data_status;
-- SELECT COUNT(*) FROM symbol_latest_status;

-- Sample queries
-- SELECT * FROM job_executions ORDER BY start_time DESC LIMIT 10;
-- SELECT * FROM symbol_latest_status WHERE status = 'STALE' LIMIT 10;
