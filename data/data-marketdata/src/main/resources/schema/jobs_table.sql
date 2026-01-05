-- Jobs table for database-driven job scheduling
-- Database: TimescaleDB (strategiz production)
-- Created: 2026-01-04
--
-- This migration adds the 'jobs' table for dynamic job scheduling, allowing
-- admins to update job schedules and enable/disable jobs from the console
-- without code changes.

-- =============================================================================
-- JOBS TABLE
-- =============================================================================
-- Stores job definitions with schedules and metadata
-- Regular table (not hypertable) - small number of jobs, infrequent updates

CREATE TABLE IF NOT EXISTS jobs (
    job_id VARCHAR(50) PRIMARY KEY,           -- e.g., 'MARKETDATA_INCREMENTAL'
    display_name VARCHAR(100) NOT NULL,       -- 'Market Data Incremental'
    description TEXT,                         -- 'Incremental collection of latest bars...'
    job_group VARCHAR(50),                    -- 'MARKETDATA' or 'FUNDAMENTALS'
    schedule_type VARCHAR(20) NOT NULL,       -- 'CRON' or 'MANUAL'
    schedule_cron VARCHAR(100),               -- '0 */5 * * * MON-FRI' (null if MANUAL)
    enabled BOOLEAN DEFAULT true,             -- Can disable from console
    job_class VARCHAR(200),                   -- Java class name (for reference)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_jobs_enabled ON jobs(enabled);
CREATE INDEX IF NOT EXISTS idx_jobs_group ON jobs(job_group);
CREATE INDEX IF NOT EXISTS idx_jobs_schedule_type ON jobs(schedule_type);

-- =============================================================================
-- UPDATE JOB_EXECUTIONS TABLE
-- =============================================================================
-- Add foreign key to link executions to job definitions

-- Add job_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'job_executions' AND column_name = 'job_id'
    ) THEN
        ALTER TABLE job_executions ADD COLUMN job_id VARCHAR(50);
    END IF;
END $$;

-- Add foreign key constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_job_executions_job'
    ) THEN
        ALTER TABLE job_executions
            ADD CONSTRAINT fk_job_executions_job
            FOREIGN KEY (job_id) REFERENCES jobs(job_id);
    END IF;
END $$;

-- Create index on job_id and start_time for efficient history queries
CREATE INDEX IF NOT EXISTS idx_job_executions_job_time
    ON job_executions(job_id, start_time DESC);

-- =============================================================================
-- SEED DATA
-- =============================================================================
-- Initial job definitions for market data and fundamentals collection

INSERT INTO jobs (job_id, display_name, description, job_group, schedule_type, schedule_cron, enabled, job_class) VALUES
    (
        'MARKETDATA_INCREMENTAL',
        'Market Data Incremental',
        'Incremental collection of latest bars across all timeframes (1Min, 5Min, 15Min, 30Min, 1Hour, 4Hour, 1Day, 1Week, 1Month). Runs every 5 minutes during market hours to keep data fresh.',
        'MARKETDATA',
        'CRON',
        '0 */5 * * * MON-FRI',  -- Every 5 minutes, Monday-Friday
        true,
        'io.strategiz.batch.marketdata.MarketDataIncrementalJob'
    ),
    (
        'MARKETDATA_BACKFILL',
        'Market Data Backfill',
        'Historical data backfill for all symbols across configured timeframes (1Day, 1Hour, 1Week, 1Month). Manual trigger only - used for initial data load or filling gaps.',
        'MARKETDATA',
        'MANUAL',
        null,  -- Manual trigger only
        true,
        'io.strategiz.batch.marketdata.MarketDataBackfillJob'
    ),
    (
        'FUNDAMENTALS_INCREMENTAL',
        'Fundamentals Incremental',
        'Daily update of company fundamentals data from Yahoo Finance. Runs at 2 AM daily to collect latest financial metrics, ratios, and company information.',
        'FUNDAMENTALS',
        'CRON',
        '0 0 2 * * *',  -- Daily at 2 AM
        true,
        'io.strategiz.batch.fundamentals.FundamentalsIncrementalJob'
    ),
    (
        'FUNDAMENTALS_BACKFILL',
        'Fundamentals Backfill',
        'Backfill company fundamentals data from Yahoo Finance for all configured symbols. Manual trigger only - used for initial data load or re-processing.',
        'FUNDAMENTALS',
        'MANUAL',
        null,  -- Manual trigger only
        true,
        'io.strategiz.batch.fundamentals.FundamentalsBackfillJob'
    )
ON CONFLICT (job_id) DO NOTHING;  -- Don't overwrite if already exists

-- =============================================================================
-- DATA MIGRATION
-- =============================================================================
-- Migrate existing job_executions to use job_id

-- Update existing executions to use new job_id format
-- Old job_name format: 'marketDataBackfill' -> New: 'MARKETDATA_BACKFILL'
UPDATE job_executions
SET job_id = 'MARKETDATA_BACKFILL'
WHERE job_name = 'marketDataBackfill' AND job_id IS NULL;

UPDATE job_executions
SET job_id = 'MARKETDATA_INCREMENTAL'
WHERE job_name = 'marketDataIncremental' AND job_id IS NULL;

UPDATE job_executions
SET job_id = 'FUNDAMENTALS_BACKFILL'
WHERE job_name = 'fundamentalsBackfill' AND job_id IS NULL;

UPDATE job_executions
SET job_id = 'FUNDAMENTALS_INCREMENTAL'
WHERE job_name = 'fundamentalsIncremental' AND job_id IS NULL;

-- For API-triggered jobs (if any exist)
UPDATE job_executions
SET job_id = 'MARKETDATA_API_BACKFILL_FULL'
WHERE job_name LIKE '%API%BACKFILL%FULL%' AND job_id IS NULL;

UPDATE job_executions
SET job_id = 'MARKETDATA_API_INCREMENTAL'
WHERE job_name LIKE '%API%INCREMENTAL%' AND job_id IS NULL;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================
-- Use these to verify the migration was successful

-- Check jobs table
-- SELECT * FROM jobs ORDER BY job_group, display_name;

-- Check foreign key constraint
-- SELECT * FROM information_schema.table_constraints WHERE constraint_name = 'fk_job_executions_job';

-- Verify job_executions have job_id populated
-- SELECT job_id, job_name, COUNT(*) FROM job_executions GROUP BY job_id, job_name;

-- Check for orphaned executions (should be empty after migration)
-- SELECT * FROM job_executions WHERE job_id IS NULL LIMIT 10;
