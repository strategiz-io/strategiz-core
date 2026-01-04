package io.strategiz.service.console.service;

import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.logging.LogEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Service for managing live log streaming to SSE clients.
 *
 * Features:
 * - Thread-safe client registration/removal
 * - Circular buffer for recent logs (late joiners get last 500 logs)
 * - Async broadcasting (doesn't block logging thread)
 * - Automatic client cleanup on disconnect/error
 * - Memory-bounded (max 500 logs per job)
 *
 * Lifecycle:
 * 1. Job starts → createJobStream(jobName)
 * 2. Admin opens console → registerClient(jobName, emitter)
 * 3. Job logs → broadcastLog(jobName, logEvent)
 * 4. Job completes → cleanupJob(jobName) after 5 minute delay
 */
@Service
public class JobLogStreamService extends BaseService {

	private static final int MAX_LOG_BUFFER_SIZE = 500;

	private static final long CLIENT_TIMEOUT_MS = 300_000; // 5 minutes

	private static final long JOB_CLEANUP_DELAY_MS = 300_000; // 5 minutes after job completion

	// jobName → CircularLogBuffer
	private final Map<String, CircularLogBuffer> logBuffers = new ConcurrentHashMap<>();

	// jobName → Set<SseEmitter>
	private final Map<String, Set<SseEmitter>> activeClients = new ConcurrentHashMap<>();

	@Override
	protected String getModuleName() {
		return "service-console";
	}

	/**
	 * Create log stream for a job (called when job starts).
	 */
	public void createJobStream(String jobName) {
		log.info("Creating log stream for job: {}", jobName);
		logBuffers.putIfAbsent(jobName, new CircularLogBuffer(MAX_LOG_BUFFER_SIZE));
		activeClients.putIfAbsent(jobName, new CopyOnWriteArraySet<>());
	}

	/**
	 * Register SSE client for job logs.
	 * Returns recent logs for late joiners.
	 */
	public List<LogEvent> registerClient(String jobName, SseEmitter emitter) {
		log.info("Registering SSE client for job: {}", jobName);

		// Ensure job stream exists
		createJobStream(jobName);

		// Add client
		Set<SseEmitter> clients = activeClients.get(jobName);
		clients.add(emitter);

		// Configure emitter callbacks
		emitter.onCompletion(() -> unregisterClient(jobName, emitter));
		emitter.onTimeout(() -> unregisterClient(jobName, emitter));
		emitter.onError((e) -> {
			log.warn("SSE error for job {}: {}", jobName, e.getMessage());
			unregisterClient(jobName, emitter);
		});

		// Return recent logs for late joiners
		CircularLogBuffer buffer = logBuffers.get(jobName);
		return buffer != null ? buffer.getRecentLogs() : Collections.emptyList();
	}

	/**
	 * Unregister SSE client (called on disconnect/timeout/error).
	 */
	public void unregisterClient(String jobName, SseEmitter emitter) {
		log.debug("Unregistering SSE client for job: {}", jobName);
		Set<SseEmitter> clients = activeClients.get(jobName);
		if (clients != null) {
			clients.remove(emitter);
			emitter.complete();
		}
	}

	/**
	 * Broadcast log event to all connected clients (async, non-blocking).
	 * Called by BatchJobLogAppender from logging thread.
	 */
	@Async
	public void broadcastLog(String jobName, LogEvent logEvent) {
		// Add to circular buffer
		CircularLogBuffer buffer = logBuffers.get(jobName);
		if (buffer != null) {
			buffer.add(logEvent);
		}

		// Broadcast to all clients
		Set<SseEmitter> clients = activeClients.get(jobName);
		if (clients == null || clients.isEmpty()) {
			return;
		}

		List<SseEmitter> deadClients = new ArrayList<>();

		for (SseEmitter emitter : clients) {
			try {
				emitter.send(SseEmitter.event().name("log").data(logEvent));
			}
			catch (IOException e) {
				log.warn("Failed to send log to client, marking for removal: {}", e.getMessage());
				deadClients.add(emitter);
			}
		}

		// Remove dead clients
		deadClients.forEach(emitter -> unregisterClient(jobName, emitter));
	}

	/**
	 * Cleanup job stream after job completion (with delay for late viewers).
	 */
	public void scheduleJobCleanup(String jobName) {
		log.info("Scheduling cleanup for job: {} in {} ms", jobName, JOB_CLEANUP_DELAY_MS);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				cleanupJob(jobName);
			}
		}, JOB_CLEANUP_DELAY_MS);
	}

	/**
	 * Immediate cleanup (disconnects all clients, clears buffer).
	 */
	public void cleanupJob(String jobName) {
		log.info("Cleaning up log stream for job: {}", jobName);

		// Disconnect all clients
		Set<SseEmitter> clients = activeClients.remove(jobName);
		if (clients != null) {
			clients.forEach(SseEmitter::complete);
		}

		// Clear log buffer
		logBuffers.remove(jobName);
	}

	/**
	 * Get current client count for a job.
	 */
	public int getClientCount(String jobName) {
		Set<SseEmitter> clients = activeClients.get(jobName);
		return clients != null ? clients.size() : 0;
	}

	/**
	 * Circular buffer for recent logs (memory-bounded).
	 */
	private static class CircularLogBuffer {

		private final int maxSize;

		private final List<LogEvent> logs = new ArrayList<>();

		public CircularLogBuffer(int maxSize) {
			this.maxSize = maxSize;
		}

		public synchronized void add(LogEvent log) {
			logs.add(log);
			if (logs.size() > maxSize) {
				logs.remove(0); // Remove oldest
			}
		}

		public synchronized List<LogEvent> getRecentLogs() {
			return new ArrayList<>(logs);
		}

	}

}
