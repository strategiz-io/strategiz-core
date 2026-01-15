package io.strategiz.service.console.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for streaming test execution logs in real-time Manages WebSocket
 * connections and broadcasts log lines to connected clients
 */
@Component
public class TestStreamingHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(TestStreamingHandler.class);

	// Map of runId -> set of WebSocket sessions
	private final Map<String, Set<WebSocketSession>> runIdToSessions = new ConcurrentHashMap<>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		String runId = extractRunId(session.getUri());
		if (runId == null) {
			log.warn("WebSocket connection established without runId, sessionId: {}", session.getId());
			return;
		}

		runIdToSessions.computeIfAbsent(runId, k -> ConcurrentHashMap.newKeySet()).add(session);
		log.info("WebSocket connected for runId: {}, sessionId: {}, total sessions: {}", runId, session.getId(),
				runIdToSessions.get(runId).size());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String runId = extractRunId(session.getUri());
		if (runId == null) {
			return;
		}

		Set<WebSocketSession> sessions = runIdToSessions.get(runId);
		if (sessions != null) {
			sessions.remove(session);
			log.info("WebSocket closed for runId: {}, sessionId: {}, status: {}, remaining sessions: {}", runId,
					session.getId(), status, sessions.size());

			if (sessions.isEmpty()) {
				runIdToSessions.remove(runId);
				log.debug("No more sessions for runId: {}, cleaned up", runId);
			}
		}
	}

	/**
	 * Broadcast log line to all connected clients for a specific test run
	 * @param runId Run ID to broadcast to
	 * @param logLine Log line to broadcast
	 */
	public void broadcastLogLine(String runId, String logLine) {
		Set<WebSocketSession> sessions = runIdToSessions.get(runId);
		if (sessions == null || sessions.isEmpty()) {
			log.debug("No active sessions for runId: {}, skipping broadcast", runId);
			return;
		}

		TextMessage message = new TextMessage(logLine);
		sessions.forEach(session -> {
			try {
				if (session.isOpen()) {
					session.sendMessage(message);
				}
			}
			catch (IOException e) {
				log.error("Failed to send log to session: {} for runId: {}", session.getId(), runId, e);
			}
		});
	}

	/**
	 * Broadcast completion message to all connected clients
	 * @param runId Run ID
	 * @param summary Final test run summary
	 */
	public void broadcastCompletion(String runId, String summary) {
		Set<WebSocketSession> sessions = runIdToSessions.get(runId);
		if (sessions == null || sessions.isEmpty()) {
			return;
		}

		String completionMessage = "COMPLETION:" + summary;
		TextMessage message = new TextMessage(completionMessage);

		sessions.forEach(session -> {
			try {
				if (session.isOpen()) {
					session.sendMessage(message);
					session.close(CloseStatus.NORMAL);
				}
			}
			catch (IOException e) {
				log.error("Failed to send completion message to session: {}", session.getId(), e);
			}
		});

		// Clean up sessions for this runId
		runIdToSessions.remove(runId);
		log.info("Broadcasted completion for runId: {}, closed {} sessions", runId, sessions.size());
	}

	/**
	 * Extract runId from WebSocket URI
	 * @param uri WebSocket URI (e.g., ws://localhost:8080/ws/test-stream/{runId})
	 * @return Extracted runId or null if not found
	 */
	private String extractRunId(URI uri) {
		if (uri == null) {
			return null;
		}

		String path = uri.getPath();
		if (path == null) {
			return null;
		}

		// Path format: /ws/test-stream/{runId}
		String[] segments = path.split("/");
		if (segments.length >= 4 && "test-stream".equals(segments[2])) {
			return segments[3];
		}

		log.warn("Could not extract runId from WebSocket path: {}", path);
		return null;
	}

	/**
	 * Get count of active sessions for a run
	 * @param runId Run ID
	 * @return Number of active WebSocket sessions
	 */
	public int getActiveSessionCount(String runId) {
		Set<WebSocketSession> sessions = runIdToSessions.get(runId);
		return sessions != null ? sessions.size() : 0;
	}

}
