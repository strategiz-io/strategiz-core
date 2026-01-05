# TimescaleDB Schema Migrations

This directory contains SQL migration files for the TimescaleDB database.

## Migration Files

1. **timescaledb_schema.sql** - Initial schema (job_executions, symbol_data_status)
2. **jobs_table.sql** - Job definitions table for dynamic scheduling (Phase 6)
3. **fix_job_executions.sql** - Bug fixes for job_executions table

## Running Migrations

### Option 1: Direct psql Execution (Recommended)

```bash
# Set your TimescaleDB connection details
export TIMESCALE_HOST="your-timescale-host"
export TIMESCALE_PORT="5432"
export TIMESCALE_DB="tsdb"
export TIMESCALE_USER="tsdbadmin"
export TIMESCALE_PASSWORD="your-password"

# Run the jobs table migration
psql "postgresql://$TIMESCALE_USER:$TIMESCALE_PASSWORD@$TIMESCALE_HOST:$TIMESCALE_PORT/$TIMESCALE_DB?sslmode=require" \
  -f data/data-marketdata/src/main/resources/schema/jobs_table.sql
```

### Option 2: Using Vault Credentials

If your TimescaleDB credentials are stored in Vault:

```bash
# Load credentials from Vault
export VAULT_ADDR="https://strategiz-vault-43628135674.us-east1.run.app"
export VAULT_TOKEN="your-vault-token"

# Get connection string from Vault
JDBC_URL=$(vault kv get -field=jdbc-url secret/strategiz/timescale)
USERNAME=$(vault kv get -field=username secret/strategiz/timescale)
PASSWORD=$(vault kv get -field=password secret/strategiz/timescale)

# Extract host and port from JDBC URL
# jdbc:postgresql://host:port/tsdb?sslmode=require
HOST=$(echo $JDBC_URL | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
PORT=$(echo $JDBC_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')

# Run migration
psql "postgresql://$USERNAME:$PASSWORD@$HOST:$PORT/tsdb?sslmode=require" \
  -f data/data-marketdata/src/main/resources/schema/jobs_table.sql
```

### Option 3: Interactive psql Session

```bash
# Connect to TimescaleDB
psql "postgresql://username:password@host:port/tsdb?sslmode=require"

# Run the migration
\i data/data-marketdata/src/main/resources/schema/jobs_table.sql

# Verify
SELECT * FROM jobs ORDER BY job_group, display_name;
```

## Verification

After running the migration, verify it was successful:

```sql
-- Check jobs table exists and has 4 rows
SELECT COUNT(*) FROM jobs;
-- Expected: 4

-- View all jobs
SELECT job_id, display_name, schedule_type, schedule_cron, enabled
FROM jobs
ORDER BY job_group, display_name;

-- Check foreign key constraint exists
SELECT constraint_name, table_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_name = 'fk_job_executions_job';

-- Verify job_executions has job_id column
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'job_executions' AND column_name = 'job_id';
```

## Expected Jobs After Migration

| job_id | display_name | schedule_type | schedule_cron | enabled |
|--------|--------------|---------------|---------------|---------|
| MARKETDATA_INCREMENTAL | Market Data Incremental | CRON | 0 */5 * * * MON-FRI | true |
| MARKETDATA_BACKFILL | Market Data Backfill | MANUAL | null | true |
| FUNDAMENTALS_INCREMENTAL | Fundamentals Incremental | CRON | 0 0 2 * * * | true |
| FUNDAMENTALS_BACKFILL | Fundamentals Backfill | MANUAL | null | true |

## Rollback (if needed)

To rollback this migration:

```sql
-- Drop foreign key constraint
ALTER TABLE job_executions DROP CONSTRAINT IF EXISTS fk_job_executions_job;

-- Drop job_id column from job_executions
ALTER TABLE job_executions DROP COLUMN IF EXISTS job_id;

-- Drop indexes
DROP INDEX IF EXISTS idx_job_executions_job_time;
DROP INDEX IF EXISTS idx_jobs_enabled;
DROP INDEX IF EXISTS idx_jobs_group;
DROP INDEX IF EXISTS idx_jobs_schedule_type;

-- Drop jobs table
DROP TABLE IF EXISTS jobs;
```

## Troubleshooting

### Error: "relation jobs already exists"

This means the migration was already run. The migration is idempotent (safe to run multiple times) due to `IF NOT EXISTS` clauses.

### Error: "permission denied for table jobs"

Ensure your database user has CREATE TABLE and INSERT permissions:

```sql
GRANT CREATE ON SCHEMA public TO your_user;
GRANT ALL ON TABLE jobs TO your_user;
```

### Verify Connection

Before running migrations, test your connection:

```bash
psql "postgresql://username:password@host:port/tsdb?sslmode=require" -c "SELECT version();"
```

## Next Steps

After migration:

1. **Restart application** - DynamicJobSchedulerBusiness will read jobs from database on startup
2. **Verify scheduler** - Check logs for "Initializing dynamic job scheduler with X jobs"
3. **Test admin console** - Access job management at `/v1/console/jobs`
4. **Update schedules** - Try changing a job's cron schedule via API
5. **Monitor execution** - Check `/v1/console/jobs/history` for job runs
