-- Migration Script: Add jobs table and update job_executions
-- Purpose: Enable database-driven job scheduling with editable schedules from admin console
-- Date: 2025-01-04

-- ============================================================================
-- STEP 1: Create jobs table for job definitions
-- ============================================================================
-- This table stores job metadata including schedules, enabling dynamic scheduling
-- and admin console management without code changes.

CREATE TABLE IF NOT EXISTS jobs (
    job_id VARCHAR(50) PRIMARY KEY,           -- e.g., 'MARKETDATA_INCREMENTAL'
    display_name VARCHAR(100) NOT NULL,       -- 'Market Data Incremental'
    description TEXT,                         -- 'Incremental collection of latest bars...'
    job_group VARCHAR(50),                    -- 'MARKETDATA' or 'FUNDAMENTALS'
    schedule_type VARCHAR(20) NOT NULL,       -- 'CRON' or 'MANUAL'
    schedule_cron VARCHAR(100),               -- '0 */5 * * * MON-FRI' (null if MANUAL)
    enabled BOOLEAN DEFAULT true,             -- Can disable from console
    job_class VARCHAR(200),                   -- Java class name (for reference)
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Note: No hypertable needed (small table, infrequent updates)
CREATE INDEX IF NOT EXISTS idx_jobs_enabled ON jobs(enabled);
CREATE INDEX IF NOT EXISTS idx_jobs_group ON jobs(job_group);

-- ============================================================================
-- STEP 2: Add foreign key to job_executions table
-- ============================================================================
-- Links execution records to job definitions for better tracking and reporting

ALTER TABLE job_executions
    ADD COLUMN IF NOT EXISTS job_id VARCHAR(50);

-- Add foreign key constraint (if not already exists)
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

-- Update index for better query performance
DROP INDEX IF EXISTS idx_job_executions_job_time;
CREATE INDEX idx_job_executions_job_time ON job_executions(job_id, start_time DESC);

-- ============================================================================
-- STEP 3: Insert seed data for existing jobs
-- ============================================================================
-- Defines 4 core jobs with their schedules and metadata

INSERT INTO jobs (job_id, display_name, description, job_group, schedule_type, schedule_cron, enabled, job_class)
VALUES
    -- Market Data Jobs
    (
        'MARKETDATA_INCREMENTAL',
        'Market Data Incremental',
        'Incremental collection of latest bars across all timeframes (1H, 4H, 1D, 1W, 1M). Runs every 5 minutes during market hours to keep data fresh.',
        'MARKETDATA',
        'CRON',
        '0 */5 * * * MON-FRI',  -- Every 5 minutes, Monday-Friday
        true,
        'io.strategiz.batch.marketdata.MarketDataIncrementalJob'
    ),
    (
        'MARKETDATA_BACKFILL',
        'Market Data Backfill',
        'Historical data backfill for all symbols across configured timeframes (1Day, 1Hour, 1Week, 1Month). Fills gaps in historical data. Manual trigger only.',
        'MARKETDATA',
        'MANUAL',
        null,  -- No schedule - manual trigger only
        true,
        'io.strategiz.batch.marketdata.MarketDataBackfillJob'
    ),

    -- Fundamentals Jobs
    (
        'FUNDAMENTALS_INCREMENTAL',
        'Fundamentals Incremental',
        'Daily update of company fundamentals data from Yahoo Finance. Refreshes financial metrics, ratios, and company info. Runs daily at 2 AM.',
        'FUNDAMENTALS',
        'CRON',
        '0 0 2 * * *',  -- Daily at 2 AM
        true,
        'io.strategiz.batch.fundamentals.FundamentalsIncrementalJob'
    ),
    (
        'FUNDAMENTALS_BACKFILL',
        'Fundamentals Backfill',
        'Backfill company fundamentals data from Yahoo Finance for all configured symbols. Populates historical fundamental data. Manual trigger only.',
        'FUNDAMENTALS',
        'MANUAL',
        null,  -- No schedule - manual trigger only
        true,
        'io.strategiz.batch.fundamentals.FundamentalsBackfillJob'
    )
ON CONFLICT (job_id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    job_group = EXCLUDED.job_group,
    schedule_type = EXCLUDED.schedule_type,
    schedule_cron = EXCLUDED.schedule_cron,
    enabled = EXCLUDED.enabled,
    job_class = EXCLUDED.job_class,
    updated_at = NOW();

-- ============================================================================
-- Verification Queries
-- ============================================================================
-- Run these after migration to verify success:
--
-- SELECT * FROM jobs ORDER BY job_group, job_id;
-- SELECT COUNT(*) FROM jobs;  -- Should return 4
-- SELECT * FROM job_executions WHERE job_id IS NOT NULL;
