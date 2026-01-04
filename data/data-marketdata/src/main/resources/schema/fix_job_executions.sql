-- Fix job_executions table to work with TimescaleDB hypertable
-- The primary key must include the partitioning column (start_time)

-- Drop the existing table
DROP TABLE IF EXISTS job_executions CASCADE;

-- Recreate with composite primary key including start_time
CREATE TABLE job_executions (
    execution_id TEXT NOT NULL,
    job_name TEXT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    duration_ms BIGINT,
    status TEXT NOT NULL,  -- SUCCESS, FAILED, RUNNING
    symbols_processed INTEGER DEFAULT 0,
    data_points_stored BIGINT DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    error_details TEXT,  -- JSON array of error messages
    timeframes TEXT,     -- JSON array of timeframes processed
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (execution_id, start_time)  -- Composite key includes partitioning column
);

-- Convert to hypertable (partitioned by start_time)
SELECT create_hypertable('job_executions', 'start_time',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '7 days'
);

-- Create indexes for common queries
CREATE INDEX idx_job_executions_job_time
    ON job_executions (job_name, start_time DESC);

CREATE INDEX idx_job_executions_status
    ON job_executions (status, start_time DESC);

CREATE INDEX idx_job_executions_id
    ON job_executions (execution_id);

-- Add retention policy: Keep 90 days of history
SELECT add_retention_policy('job_executions', INTERVAL '90 days', if_not_exists => TRUE);
