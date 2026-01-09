package io.strategiz.service.console.controller;

import io.strategiz.business.marketdata.DynamicJobSchedulerBusiness;
import io.strategiz.business.marketdata.JobExecutionHistoryBusiness;
import io.strategiz.data.marketdata.firestore.entity.JobDefinitionFirestoreEntity;
import io.strategiz.data.marketdata.firestore.repository.JobDefinitionFirestoreRepository;
import io.strategiz.data.marketdata.timescale.entity.JobExecutionEntity;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.JobExecutionHistoryResponse;
import io.strategiz.service.console.model.response.JobExecutionRecord;
import io.strategiz.service.console.model.response.JobResponse;
import io.strategiz.service.console.service.JobManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for managing scheduled jobs.
 * Provides REST endpoints for viewing, triggering, and configuring jobs from the admin console.
 * Supports dynamic schedule updates and enable/disable functionality.
 */
@RestController
@RequestMapping("/v1/console/jobs")
@Tag(name = "Admin - Jobs", description = "Job management endpoints for administrators")
public class AdminJobController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final JobManagementService jobManagementService;
    private final JobExecutionHistoryBusiness jobExecutionHistoryBusiness;
    private final JobDefinitionFirestoreRepository jobDefinitionRepository;
    private final DynamicJobSchedulerBusiness dynamicJobSchedulerBusiness;

    @Autowired
    public AdminJobController(
            JobManagementService jobManagementService,
            JobExecutionHistoryBusiness jobExecutionHistoryBusiness,
            JobDefinitionFirestoreRepository jobDefinitionRepository,
            DynamicJobSchedulerBusiness dynamicJobSchedulerBusiness) {
        this.jobManagementService = jobManagementService;
        this.jobExecutionHistoryBusiness = jobExecutionHistoryBusiness;
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.dynamicJobSchedulerBusiness = dynamicJobSchedulerBusiness;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @GetMapping
    @Operation(summary = "List all scheduled jobs", description = "Returns a list of all scheduled jobs with their status")
    public ResponseEntity<List<JobResponse>> listJobs(HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("listJobs", adminUserId);

        List<JobResponse> jobs = jobManagementService.listJobs();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get job details", description = "Returns details for a specific scheduled job")
    public ResponseEntity<JobResponse> getJob(
            @Parameter(description = "Job name") @PathVariable String name,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getJob", adminUserId, "jobName=" + name);

        JobResponse job = jobManagementService.getJob(name);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{name}/trigger")
    @Operation(summary = "Trigger job execution", description = "Manually triggers immediate execution of a scheduled job")
    public ResponseEntity<JobResponse> triggerJob(
            @Parameter(description = "Job name") @PathVariable String name,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("triggerJob", adminUserId, "jobName=" + name);

        JobResponse job = jobManagementService.triggerJob(name);
        log.info("Job {} triggered by admin {}", name, adminUserId);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/history")
    @Operation(summary = "Get all job execution history", description = "Returns paginated execution history across all jobs with statistics")
    public ResponseEntity<JobExecutionHistoryResponse> getAllJobHistory(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int pageSize,
            @Parameter(description = "Statistics period in days") @RequestParam(defaultValue = "30") int sinceDays,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getAllJobHistory", adminUserId, "page=" + page);

        // Get paginated execution history across all jobs
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<JobExecutionEntity> executionsPage = jobExecutionHistoryBusiness.getAllExecutionHistory(pageable);

        // Convert to DTOs
        List<JobExecutionRecord> executionRecords = executionsPage.getContent().stream()
            .map(this::convertToJobExecutionRecord)
            .collect(Collectors.toList());

        // Calculate aggregate statistics across all jobs
        Instant since = Instant.ofEpochMilli(System.currentTimeMillis() - sinceDays * 24L * 60L * 60L * 1000L);
        List<JobExecutionEntity> recentExecutions = jobExecutionHistoryBusiness.getAllExecutionsSince(since);

        long successCount = recentExecutions.stream().filter(e -> "SUCCESS".equals(e.getStatus())).count();
        long failureCount = recentExecutions.stream().filter(e -> "FAILED".equals(e.getStatus())).count();
        long totalCount = successCount + failureCount;
        double successRate = (totalCount > 0) ? (double) successCount / totalCount * 100.0 : 0.0;

        long avgDuration = (long) recentExecutions.stream()
            .filter(e -> "SUCCESS".equals(e.getStatus()) && e.getDurationMs() != null)
            .mapToLong(JobExecutionEntity::getDurationMs)
            .average()
            .orElse(0.0);

        Map<String, Object> stats = Map.of(
            "successCount", successCount,
            "failureCount", failureCount,
            "totalCount", totalCount,
            "successRate", Math.round(successRate * 100.0) / 100.0,
            "avgDurationMs", avgDuration,
            "periodDays", sinceDays
        );

        // Build response
        JobExecutionHistoryResponse response = new JobExecutionHistoryResponse(
            executionRecords,
            stats,
            page,
            pageSize,
            executionsPage.getTotalElements(),
            executionsPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}/history")
    @Operation(summary = "Get job execution history", description = "Returns paginated execution history for a job with statistics")
    public ResponseEntity<JobExecutionHistoryResponse> getJobHistory(
            @Parameter(description = "Job name") @PathVariable String name,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int pageSize,
            @Parameter(description = "Statistics period in days") @RequestParam(defaultValue = "30") int sinceDays,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getJobHistory", adminUserId, "jobName=" + name + ", page=" + page);

        // Get paginated execution history
        Page<JobExecutionEntity> executionsPage = jobExecutionHistoryBusiness.getExecutionHistory(name, page, pageSize);

        // Convert to DTOs
        List<JobExecutionRecord> executionRecords = executionsPage.getContent().stream()
            .map(this::convertToJobExecutionRecord)
            .collect(Collectors.toList());

        // Get statistics for the period
        Map<String, Object> stats = jobExecutionHistoryBusiness.getExecutionStats(name, sinceDays);

        // Build response
        JobExecutionHistoryResponse response = new JobExecutionHistoryResponse(
            executionRecords,
            stats,
            page,
            pageSize,
            executionsPage.getTotalElements(),
            executionsPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}/stats")
    @Operation(summary = "Get job statistics", description = "Returns execution statistics for a job over a specified period")
    public ResponseEntity<Map<String, Object>> getJobStats(
            @Parameter(description = "Job name") @PathVariable String name,
            @Parameter(description = "Statistics period in days") @RequestParam(defaultValue = "30") int sinceDays,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getJobStats", adminUserId, "jobName=" + name + ", sinceDays=" + sinceDays);

        Map<String, Object> stats = jobExecutionHistoryBusiness.getExecutionStats(name, sinceDays);

        log.debug("Job stats for {}: {}", name, stats);
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{jobId}/schedule")
    @Operation(summary = "Update job schedule", description = "Updates the cron schedule for a scheduled job")
    public ResponseEntity<Map<String, Object>> updateJobSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId,
            @Parameter(description = "New cron expression") @RequestParam String cron,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("updateJobSchedule", adminUserId, "jobId=" + jobId + ", cron=" + cron);

        // Verify job exists and is a scheduled job
        JobDefinitionFirestoreEntity jobDef = jobDefinitionRepository.findByJobId(jobId)
            .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, MODULE_NAME, jobId));

        if (!"CRON".equals(jobDef.getScheduleType())) {
            throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_SCHEDULED, MODULE_NAME,
                    "Job " + jobId + " is not a scheduled job (type: " + jobDef.getScheduleType() + ")");
        }

        // Update schedule in database
        String oldCron = jobDef.getScheduleCron();
        jobDef.setScheduleCron(cron);
        jobDefinitionRepository.save(jobDef);

        // Update running scheduler
        dynamicJobSchedulerBusiness.updateSchedule(jobId, cron);

        log.info("Job {} schedule updated by admin {}: {} -> {}", jobId, adminUserId, oldCron, cron);

        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "oldSchedule", oldCron,
            "newSchedule", cron,
            "message", "Schedule updated successfully"
        ));
    }

    @PutMapping("/{jobId}/enabled")
    @Operation(summary = "Enable or disable job", description = "Enables or disables a job (both scheduled and manual)")
    public ResponseEntity<Map<String, Object>> updateJobEnabled(
            @Parameter(description = "Job ID") @PathVariable String jobId,
            @Parameter(description = "Enable (true) or disable (false)") @RequestParam boolean enabled,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("updateJobEnabled", adminUserId, "jobId=" + jobId + ", enabled=" + enabled);

        // Verify job exists
        JobDefinitionFirestoreEntity jobDef = jobDefinitionRepository.findByJobId(jobId)
            .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, MODULE_NAME, jobId));

        // Update enabled status in database
        boolean wasEnabled = jobDef.getEnabled();
        jobDef.setEnabled(enabled);
        jobDefinitionRepository.save(jobDef);

        // If this is a scheduled job, update the scheduler
        if ("CRON".equals(jobDef.getScheduleType())) {
            if (enabled && !wasEnabled) {
                // Enable: Schedule the job
                dynamicJobSchedulerBusiness.scheduleJob(jobDef);
                log.info("Job {} enabled and scheduled by admin {}", jobId, adminUserId);
            } else if (!enabled && wasEnabled) {
                // Disable: Cancel the scheduled task
                dynamicJobSchedulerBusiness.cancelJob(jobId);
                log.info("Job {} disabled and unscheduled by admin {}", jobId, adminUserId);
            }
        } else {
            log.info("Manual job {} {} by admin {}", jobId, enabled ? "enabled" : "disabled", adminUserId);
        }

        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "enabled", enabled,
            "wasEnabled", wasEnabled,
            "message", enabled ? "Job enabled successfully" : "Job disabled successfully"
        ));
    }

    // Converter methods

    /**
     * Convert JobExecutionEntity to JobExecutionRecord DTO.
     */
    private JobExecutionRecord convertToJobExecutionRecord(JobExecutionEntity entity) {
        JobExecutionRecord record = new JobExecutionRecord();
        record.setExecutionId(entity.getExecutionId());
        record.setJobName(entity.getJobName());
        record.setStartTime(formatInstant(entity.getStartTime()));
        record.setEndTime(formatInstant(entity.getEndTime()));
        record.setDurationMs(entity.getDurationMs());
        record.setStatus(entity.getStatus());
        record.setSymbolsProcessed(entity.getSymbolsProcessed());
        record.setDataPointsStored(entity.getDataPointsStored());
        record.setErrorCount(entity.getErrorCount());
        record.setErrorDetails(entity.getErrorDetails());
        record.setTimeframes(entity.getTimeframes());
        record.setContext(entity.getTimeframes()); // Context stored in timeframes field
        return record;
    }

    /**
     * Format Instant to ISO 8601 string.
     */
    private Instant formatInstant(Instant instant) {
        return instant;  // Return as-is, Jackson will serialize to ISO 8601
    }
}
