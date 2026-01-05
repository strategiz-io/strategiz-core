# Database-Driven Job Scheduling System - Setup Complete

**Status:** ✅ READY FOR PRODUCTION
**Date:** 2026-01-04
**Phase:** Phase 6 Complete - Console Integration

---

## What Was Accomplished

### 1. Database Migration ✅
- **File:** `data/data-marketdata/src/main/resources/schema/jobs_table.sql`
- **Status:** Successfully executed against production TimescaleDB
- **Tables Created:**
  - `jobs` - Job definitions with metadata and schedules
  - `job_executions` - Now has `job_id` foreign key linking to jobs table

### 2. Job Metadata Seeded ✅
Four jobs are now in the database and ready to configure:

| Job ID | Display Name | Type | Schedule | Status |
|--------|-------------|------|----------|--------|
| MARKETDATA_INCREMENTAL | Market Data Incremental | CRON | Every 5 min (Mon-Fri) | Enabled |
| MARKETDATA_BACKFILL | Market Data Backfill | MANUAL | - | Enabled |
| FUNDAMENTALS_INCREMENTAL | Fundamentals Incremental | CRON | Daily at 2 AM | Enabled |
| FUNDAMENTALS_BACKFILL | Fundamentals Backfill | MANUAL | - | Enabled |

### 3. Backend Services Implemented ✅
- **JobDefinitionEntity** - JPA entity for jobs table
- **JobDefinitionRepository** - Spring Data repository with custom queries
- **DynamicJobSchedulerBusiness** - Dynamic scheduler reading from database
- **JobExecutionHistoryBusiness** - Tracks execution history
- **AdminJobController** - REST API for job management
- **JobManagementService** - Business logic for job operations

### 4. Configuration Updated ✅
- **AdminJobController** - `@Profile` restriction removed, now always enabled
- **Production Config** - Added `scheduler` profile to `application-prod.properties`
- **TimescaleDB** - Already enabled in production
- **Dependencies** - service-console now includes business-marketdata and data-marketdata

---

## Available Admin Console Endpoints

All endpoints require admin authentication via `AdminAuthInterceptor`.

### Job Listing
```http
GET /v1/console/jobs
```
**Response:** List of all jobs with status, last run, schedule

### Job Details
```http
GET /v1/console/jobs/{jobId}
```
**Response:** Detailed job information including execution history

### Manual Job Trigger
```http
POST /v1/console/jobs/{jobId}/trigger
```
**Example:** Trigger backfill job
```bash
curl -X POST https://api.strategiz.io/v1/console/jobs/MARKETDATA_BACKFILL/trigger \
  -H "Cookie: strategiz-access-token=YOUR_ADMIN_TOKEN"
```

### Update Job Schedule
```http
PUT /v1/console/jobs/{jobId}/schedule?cron=NEW_CRON
```
**Example:** Change incremental job to run every 10 minutes
```bash
curl -X PUT "https://api.strategiz.io/v1/console/jobs/MARKETDATA_INCREMENTAL/schedule?cron=0%20*/10%20*%20*%20*%20MON-FRI" \
  -H "Cookie: strategiz-access-token=YOUR_ADMIN_TOKEN"
```

**Validation:**
- Only works for CRON type jobs (returns 400 for MANUAL jobs)
- Updates database and immediately reschedules the running task
- Old schedule is cancelled, new schedule starts

### Enable/Disable Job
```http
PUT /v1/console/jobs/{jobId}/enabled?enabled=true|false
```
**Example:** Disable fundamentals incremental job
```bash
curl -X PUT "https://api.strategiz.io/v1/console/jobs/FUNDAMENTALS_INCREMENTAL/enabled?enabled=false" \
  -H "Cookie: strategiz-access-token=YOUR_ADMIN_TOKEN"
```

**Behavior:**
- CRON jobs: Cancels scheduler task when disabled, reschedules when re-enabled
- MANUAL jobs: Just marks as disabled, won't allow manual trigger

### Job Execution History
```http
GET /v1/console/jobs/{jobId}/history?page=0&pageSize=50&sinceDays=30
```
**Response:** Paginated execution history with statistics

**Statistics Include:**
- Success count, failure count, total count
- Success rate percentage
- Average duration in milliseconds
- Period covered (default 30 days)

### Job Statistics
```http
GET /v1/console/jobs/{jobId}/stats?sinceDays=30
```
**Response:** Execution statistics without pagination

### All Jobs History
```http
GET /v1/console/jobs/history?page=0&pageSize=50&sinceDays=30
```
**Response:** Combined execution history across all jobs

---

## How It Works

### Application Startup Flow

1. **Spring Boot starts** with `scheduler` profile active
2. **Job beans load** - All 4 batch job classes become available as Spring beans
3. **DynamicJobSchedulerBusiness.initializeSchedules()** runs via `@PostConstruct`
4. **Reads from database** - `SELECT * FROM jobs WHERE schedule_type='CRON' AND enabled=true`
5. **Schedules CRON jobs** - Creates `ScheduledFuture` for each enabled CRON job
6. **Jobs run automatically** - Spring TaskScheduler executes jobs per cron schedule

### Manual Job Trigger Flow

1. **Admin calls** `POST /v1/console/jobs/MARKETDATA_BACKFILL/trigger`
2. **AdminJobController** validates job exists and not already running
3. **JobManagementService** marks job as RUNNING, calls `executeJob()`
4. **Reflection execution** - Gets job bean from ApplicationContext, invokes `execute()` method
5. **History recorded** - JobExecutionHistoryBusiness saves start/end/duration/status to TimescaleDB
6. **Response returned** - Job details with updated status

### Dynamic Schedule Update Flow

1. **Admin calls** `PUT /v1/console/jobs/MARKETDATA_INCREMENTAL/schedule?cron=0 */10 * * * MON-FRI`
2. **AdminJobController** validates job is CRON type
3. **Updates database** - `UPDATE jobs SET schedule_cron=... WHERE job_id=...`
4. **Cancels old schedule** - `ScheduledFuture.cancel(false)` stops current task
5. **Creates new schedule** - `TaskScheduler.schedule(...)` with new CronTrigger
6. **Immediate effect** - Next execution uses new schedule (no restart required)

---

## Frontend Console Integration

### Job Management UI (Not Yet Implemented)

The admin console at `console.strategiz.io/automation` should display:

**Jobs Tab:**
- Table of all jobs with status, last run, next run (for CRON jobs)
- "Trigger" button for manual jobs
- "View History" button linking to execution history
- "Edit Schedule" button for CRON jobs (opens modal with cron builder)
- "Disable/Enable" toggle switch

**Example Implementation (React):**
```typescript
// Fetch jobs
const response = await axios.get('/v1/console/jobs');
const jobs = response.data;

// Trigger job
await axios.post(`/v1/console/jobs/${jobId}/trigger`);

// Update schedule
await axios.put(`/v1/console/jobs/${jobId}/schedule`, null, {
  params: { cron: '0 */10 * * * MON-FRI' }
});

// Toggle enabled
await axios.put(`/v1/console/jobs/${jobId}/enabled`, null, {
  params: { enabled: false }
});

// Get history
const history = await axios.get(`/v1/console/jobs/${jobId}/history`, {
  params: { page: 0, pageSize: 50, sinceDays: 30 }
});
```

---

## Verification Steps

### 1. Verify Database
```bash
PGPASSWORD=yahww6nu2wwwpjvd /opt/homebrew/opt/libpq/bin/psql \
  "postgresql://tsdbadmin@ek4xcb982s.o7i8pdseks.tsdb.cloud.timescale.com:36378/tsdb?sslmode=require" \
  -c "SELECT job_id, display_name, enabled, schedule_cron FROM jobs;"
```

**Expected:** 4 jobs displayed

### 2. Verify Application Startup
After deploying to Cloud Run, check logs for:
```
Initializing dynamic job scheduler with 2 jobs from database
Scheduled job: MARKETDATA_INCREMENTAL with cron: 0 */5 * * * MON-FRI
Scheduled job: FUNDAMENTALS_INCREMENTAL with cron: 0 0 2 * * *
Dynamic job scheduler initialized with 2 active schedules
```

### 3. Test API Endpoints
```bash
# List jobs
curl https://api.strategiz.io/v1/console/jobs \
  -H "Cookie: strategiz-access-token=YOUR_ADMIN_TOKEN"

# Trigger backfill
curl -X POST https://api.strategiz.io/v1/console/jobs/MARKETDATA_BACKFILL/trigger \
  -H "Cookie: strategiz-access-token=YOUR_ADMIN_TOKEN"

# Check history
curl https://api.strategiz.io/v1/console/jobs/history \
  -H "Cookie: strategiz-access-token=YOUR_ADMIN_TOKEN"
```

### 4. Verify Job Execution
After triggering a job, check TimescaleDB:
```sql
SELECT execution_id, job_id, job_name, status, duration_ms, symbols_processed
FROM job_executions
ORDER BY start_time DESC
LIMIT 10;
```

---

## Next Deployment

When you deploy to Cloud Run, the system will:

1. ✅ Read jobs from TimescaleDB on startup
2. ✅ Schedule MARKETDATA_INCREMENTAL (every 5 min) and FUNDAMENTALS_INCREMENTAL (daily 2 AM)
3. ✅ Expose `/v1/console/jobs` endpoints for admin console
4. ✅ Allow dynamic schedule updates without redeployment
5. ✅ Track all execution history in TimescaleDB

**No additional setup required** - everything is configured and ready!

---

## Troubleshooting

### Jobs Not Scheduling
**Symptom:** No "Initializing dynamic job scheduler" log message

**Check:**
1. `spring.profiles.active=scheduler` in application-prod.properties
2. Job beans available: `grep @Component batch-marketdata/src/main/java/io/strategiz/batch/marketdata/*.java`
3. TimescaleDB connection working: Check logs for "Connected to TimescaleDB"

### Job Trigger Fails
**Symptom:** 404 or 500 error when calling `/v1/console/jobs/{jobId}/trigger`

**Check:**
1. AdminJobController enabled (no `@Profile` annotation)
2. Job exists in database: `SELECT * FROM jobs WHERE job_id='MARKETDATA_BACKFILL'`
3. Job bean available: Should be `@Component @Profile("scheduler")`

### Schedule Update Not Working
**Symptom:** Cron schedule update succeeds but job still runs on old schedule

**Check:**
1. Database updated: `SELECT schedule_cron FROM jobs WHERE job_id='...'`
2. Logs show reschedule: `"Job X rescheduled with new cron: ..."`
3. Application not restarted after update (restart clears dynamic schedules)

### No Execution History
**Symptom:** `/v1/console/jobs/history` returns empty array

**Check:**
1. Jobs have run: Trigger manually or wait for cron schedule
2. JobExecutionHistoryBusiness recording: Check logs for "Recording job start/completion"
3. TimescaleDB connection: `SELECT COUNT(*) FROM job_executions`

---

## Summary

✅ **Database:** Migration complete, 4 jobs seeded
✅ **Backend:** All services implemented and compiled
✅ **Configuration:** Scheduler profile enabled, AdminJobController active
✅ **Endpoints:** 8 REST endpoints ready for admin console
✅ **Documentation:** Migration guide, API docs, troubleshooting guide

**Status: READY FOR DEPLOYMENT AND TESTING** 🚀
