package io.strategiz.service.console.controller;

import io.strategiz.business.marketdata.JobExecutionHistoryService;
import io.strategiz.data.marketdata.timescale.entity.JobExecutionEntity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.JobExecutionHistoryResponse;
import io.strategiz.service.console.model.response.JobExecutionRecord;
import io.strategiz.service.console.model.response.JobResponse;
import io.strategiz.service.console.service.JobManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@RestController
@RequestMapping("/v1/console/jobs")
@Tag(name = "Admin - Jobs", description = "Job management endpoints for administrators")
public class AdminJobController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final JobManagementService jobManagementService;
    private final JobExecutionHistoryService jobExecutionHistoryService;

    @Autowired
    public AdminJobController(
            JobManagementService jobManagementService,
            JobExecutionHistoryService jobExecutionHistoryService) {
        this.jobManagementService = jobManagementService;
        this.jobExecutionHistoryService = jobExecutionHistoryService;
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
        Page<JobExecutionEntity> executionsPage = jobExecutionHistoryService.getAllExecutionHistory(pageable);

        // Convert to DTOs
        List<JobExecutionRecord> executionRecords = executionsPage.getContent().stream()
            .map(this::convertToJobExecutionRecord)
            .collect(Collectors.toList());

        // Calculate aggregate statistics across all jobs
        Instant since = Instant.ofEpochMilli(System.currentTimeMillis() - sinceDays * 24L * 60L * 60L * 1000L);
        List<JobExecutionEntity> recentExecutions = jobExecutionHistoryService.getAllExecutionsSince(since);

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
        Page<JobExecutionEntity> executionsPage = jobExecutionHistoryService.getExecutionHistory(name, page, pageSize);

        // Convert to DTOs
        List<JobExecutionRecord> executionRecords = executionsPage.getContent().stream()
            .map(this::convertToJobExecutionRecord)
            .collect(Collectors.toList());

        // Get statistics for the period
        Map<String, Object> stats = jobExecutionHistoryService.getExecutionStats(name, sinceDays);

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

        Map<String, Object> stats = jobExecutionHistoryService.getExecutionStats(name, sinceDays);

        log.debug("Job stats for {}: {}", name, stats);
        return ResponseEntity.ok(stats);
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
        return record;
    }

    /**
     * Format Instant to ISO 8601 string.
     */
    private Instant formatInstant(Instant instant) {
        return instant;  // Return as-is, Jackson will serialize to ISO 8601
    }
}
