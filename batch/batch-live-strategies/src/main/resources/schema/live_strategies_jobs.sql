-- Live Strategies Jobs Migration
-- Database: TimescaleDB (strategiz production)
-- Created: 2026-01-06
--
-- This migration adds the LIVE_STRATEGIES job group for alert and bot dispatching.
-- Jobs are scheduled based on subscription tier:
--   TIER1 (PRO): Every 1 minute - Premium subscribers
--   TIER2 (STARTER): Every 5 minutes - Standard subscribers
--   TIER3 (FREE): Every 15 minutes - Free users

-- =============================================================================
-- SEED DATA - LIVE STRATEGIES JOBS
-- =============================================================================

INSERT INTO jobs (job_id, display_name, description, job_group, schedule_type, schedule_cron, enabled, job_class) VALUES
    (
        'DISPATCH_TIER1',
        'Dispatch TIER1 Alerts/Bots',
        'Dispatches TIER1 (PRO) alerts and bots for strategy evaluation every 1 minute. Fastest tier for paid subscribers. Groups deployments by symbol set and publishes to Pub/Sub for parallel processing.',
        'LIVE_STRATEGIES',
        'CRON',
        '0 * * * * *',  -- Every minute
        true,
        'io.strategiz.batch.livestrategies.DispatchJob'
    ),
    (
        'DISPATCH_TIER2',
        'Dispatch TIER2 Alerts/Bots',
        'Dispatches TIER2 (STARTER) alerts and bots for strategy evaluation every 5 minutes. Standard tier for paid subscribers. Groups deployments by symbol set and publishes to Pub/Sub for parallel processing.',
        'LIVE_STRATEGIES',
        'CRON',
        '0 */5 * * * *',  -- Every 5 minutes
        true,
        'io.strategiz.batch.livestrategies.DispatchJob'
    ),
    (
        'DISPATCH_TIER3',
        'Dispatch TIER3 Alerts/Bots',
        'Dispatches TIER3 (FREE) alerts and bots for strategy evaluation every 15 minutes. Free tier for all users. Groups deployments by symbol set and publishes to Pub/Sub for parallel processing.',
        'LIVE_STRATEGIES',
        'CRON',
        '0 */15 * * * *',  -- Every 15 minutes
        true,
        'io.strategiz.batch.livestrategies.DispatchJob'
    )
ON CONFLICT (job_id) DO NOTHING;  -- Don't overwrite if already exists

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================
-- Use these to verify the migration was successful

-- Check live strategies jobs
-- SELECT * FROM jobs WHERE job_group = 'LIVE_STRATEGIES' ORDER BY job_id;

-- Verify all job groups
-- SELECT job_group, COUNT(*) as job_count FROM jobs GROUP BY job_group ORDER BY job_group;

-- Check job schedules
-- SELECT job_id, display_name, schedule_cron, enabled FROM jobs WHERE job_group = 'LIVE_STRATEGIES';
