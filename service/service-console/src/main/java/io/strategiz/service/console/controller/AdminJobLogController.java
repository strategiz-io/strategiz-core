package io.strategiz.service.console.controller;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.logging.LogEvent;
import io.strategiz.service.console.service.JobLogStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * Controller for streaming batch job logs via Server-Sent Events (SSE).
 *
 * Endpoints:
 * - GET /v1/console/jobs/{jobName}/logs/stream → SSE stream of real-time logs
 * - GET /v1/console/jobs/{jobName}/logs/recent → Recent logs (last 500)
 *
 * Security: @RequireAuth ensures only authenticated admins can access
 */
@RestController
@RequestMapping("/v1/console/jobs")
@Tag(name = "Admin - Job Logs", description = "Real-time job log streaming for administrators")
public class AdminJobLogController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private static final long SSE_TIMEOUT = 300_000; // 5 minutes

	private final JobLogStreamService logStreamService;

	@Autowired
	public AdminJobLogController(JobLogStreamService logStreamService) {
		this.logStreamService = logStreamService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	@GetMapping(value = "/{jobName}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@RequireAuth(minAcr = "2") // Admin only
	@Operation(summary = "Stream job logs in real-time",
			description = "Establishes SSE connection for live log streaming. Returns recent logs for late joiners.")
	public SseEmitter streamLogs(@Parameter(description = "Job name") @PathVariable String jobName,
			HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("userId");
		logRequest("streamLogs", adminUserId, "jobName=" + jobName);

		// Create SSE emitter with timeout
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

		// Register client and get recent logs
		List<LogEvent> recentLogs = logStreamService.registerClient(jobName, emitter);

		// Send recent logs as initial data (for late joiners)
		if (!recentLogs.isEmpty()) {
			log.info("Sending {} recent logs to new client for job {}", recentLogs.size(), jobName);
			try {
				for (LogEvent logEvent : recentLogs) {
					emitter.send(SseEmitter.event().name("log").data(logEvent));
				}
			}
			catch (IOException e) {
				log.error("Failed to send recent logs: {}", e.getMessage());
				emitter.completeWithError(e);
			}
		}

		log.info("SSE connection established for job {}, total clients: {}", jobName,
				logStreamService.getClientCount(jobName));

		return emitter;
	}

	@GetMapping("/{jobName}/logs/recent")
	@RequireAuth(minAcr = "2") // Admin only
	@Operation(summary = "Get recent job logs",
			description = "Returns last 500 log entries for a job (for late viewers or failed SSE)")
	public List<LogEvent> getRecentLogs(@Parameter(description = "Job name") @PathVariable String jobName,
			HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("userId");
		logRequest("getRecentLogs", adminUserId, "jobName=" + jobName);

		// Create stream if doesn't exist (for completed jobs)
		logStreamService.createJobStream(jobName);

		// Create temporary emitter to get recent logs
		SseEmitter tempEmitter = new SseEmitter();
		List<LogEvent> logs = logStreamService.registerClient(jobName, tempEmitter);

		// Immediately unregister (we don't actually want to stream)
		logStreamService.unregisterClient(jobName, tempEmitter);

		log.info("Returning {} recent logs for job {}", logs.size(), jobName);
		return logs;
	}

}
