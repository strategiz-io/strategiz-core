package io.strategiz.service.console.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.JobResponse;
import io.strategiz.service.console.service.JobManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for managing scheduled jobs.
 */
@RestController
@RequestMapping("/v1/console/jobs")
@Tag(name = "Admin - Jobs", description = "Job management endpoints for administrators")
public class AdminJobController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final JobManagementService jobManagementService;

    @Autowired
    public AdminJobController(JobManagementService jobManagementService) {
        this.jobManagementService = jobManagementService;
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
}
