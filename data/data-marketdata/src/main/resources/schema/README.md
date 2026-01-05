# TimescaleDB Schema Migrations

This directory contains SQL migration scripts for the TimescaleDB database used by Strategiz.

## Database Connection

### Development (Local)
```bash
# Using Vault credentials
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root
vault kv get secret/strategiz/timescale

# Connect using psql
psql "jdbc-url-from-vault"
```

### Production
```bash
# Using Vault credentials
export VAULT_ADDR=https://strategiz-vault-43628135674.us-east1.run.app
export VAULT_TOKEN=hvs.q2Lg7uILKNkEs20UA8mbT9Cr
vault kv get secret/strategiz/timescale

# Connect using psql
psql "jdbc-url-from-vault"
```

## Migration Scripts

### 1. `timescaledb_schema.sql` (Initial Schema)
**Purpose**: Creates initial hypertables for job executions and symbol data status

**Tables Created:**
- `job_executions` - Hypertable tracking job execution history
- `symbol_data_status` - Hypertable tracking per-symbol data freshness
- `symbol_latest_status` - Materialized view for latest status

**Run Once**: Initial setup only

```bash
psql "your-jdbc-url" -f timescaledb_schema.sql
```

### 2. `jobs_table.sql` (Database-Driven Scheduling)
**Purpose**: Adds job definitions table for editable schedules from admin console

**Tables Created:**
- `jobs` - Stores job metadata, schedules, and enable/disable status

**Schema Changes:**
- Adds `job_id` foreign key column to `job_executions`
- Updates indexes for better query performance

**Seed Data:**
- MARKETDATA_INCREMENTAL (runs every 5 min)
- MARKETDATA_BACKFILL (manual trigger)
- FUNDAMENTALS_INCREMENTAL (daily at 2 AM)
- FUNDAMENTALS_BACKFILL (manual trigger)

**Run After**: `timescaledb_schema.sql` has been applied

```bash
psql "your-jdbc-url" -f jobs_table.sql
```

### 3. `fix_job_executions.sql` (Emergency Fix)
**Purpose**: Hotfix for specific issues

**Run When**: Needed for specific bugfixes only

## Migration Order

**For New Databases:**
```bash
# 1. Initial schema
psql "your-jdbc-url" -f timescaledb_schema.sql

# 2. Add jobs table
psql "your-jdbc-url" -f jobs_table.sql
```

**For Existing Databases:**
```bash
# Only run jobs_table.sql (timescaledb_schema.sql already applied)
psql "your-jdbc-url" -f jobs_table.sql
```

## Verification

After running migrations, verify schema:

```sql
-- Check all tables exist
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- Check jobs table populated
SELECT COUNT(*) FROM jobs;  -- Should return 4

-- Check job_executions has job_id column
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'job_executions' AND column_name = 'job_id';

-- Check foreign key exists
SELECT conname, conrelid::regclass, confrelid::regclass
FROM pg_constraint WHERE conname = 'fk_job_executions_job';

-- View all jobs
SELECT job_id, display_name, schedule_type, schedule_cron, enabled
FROM jobs ORDER BY job_group, job_id;
```

## Rollback

If needed, rollback jobs_table.sql changes:

```sql
-- Remove foreign key
ALTER TABLE job_executions DROP CONSTRAINT IF EXISTS fk_job_executions_job;

-- Remove job_id column
ALTER TABLE job_executions DROP COLUMN IF EXISTS job_id;

-- Drop jobs table
DROP TABLE IF EXISTS jobs;

-- Restore original index
CREATE INDEX IF NOT EXISTS idx_job_executions_job_time
    ON job_executions (job_name, start_time DESC);
```

## Common Issues

### Issue: psql command not found
**Solution**: Install PostgreSQL client
```bash
# Mac
brew install postgresql

# Ubuntu/Debian
sudo apt-get install postgresql-client
```

### Issue: Connection refused
**Solution**: Check TimescaleDB instance is running and credentials are correct

### Issue: Permission denied
**Solution**: Ensure database user has CREATE TABLE and ALTER TABLE privileges

### Issue: Jobs table already exists
**Solution**: Script uses `CREATE TABLE IF NOT EXISTS` and `ON CONFLICT DO UPDATE`, safe to re-run

## Notes

- All migrations are idempotent (safe to run multiple times)
- Production migrations should be tested in dev environment first
- Always backup before running migrations in production
- job_executions is a hypertable - be careful with schema changes
