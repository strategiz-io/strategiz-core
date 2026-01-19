package io.strategiz.batch.livestrategies.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.strategiz.batch.livestrategies.DispatchJob;
import io.strategiz.batch.livestrategies.job.SymbolSetProcessorJob;
import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

/**
 * Controller for live strategy deployment processing.
 *
 * Endpoints:
 * - POST /internal/dispatch/{tier} - Trigger dispatch for a tier (called by scheduler)
 * - POST /internal/process - Process Pub/Sub push message
 */
@RestController
@RequestMapping("/internal")
@Profile("scheduler")
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class DeploymentProcessController {

	private static final Logger log = LoggerFactory.getLogger(DeploymentProcessController.class);

	private final DispatchJob dispatchJob;

	private final SymbolSetProcessorJob processorJob;

	private final ObjectMapper objectMapper;

	@Autowired
	public DeploymentProcessController(DispatchJob dispatchJob, SymbolSetProcessorJob processorJob) {
		this.dispatchJob = dispatchJob;
		this.processorJob = processorJob;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	/**
	 * Trigger dispatch for a specific tier.
	 * Called by Spring Batch scheduler or manually from admin console.
	 *
	 * @param tier The tier to dispatch: TIER1, TIER2, or TIER3
	 */
	@PostMapping("/dispatch/{tier}")
	public ResponseEntity<DispatchJob.DispatchResult> triggerDispatch(@PathVariable String tier) {
		log.info("Dispatch triggered for tier: {}", tier);

		try {
			DispatchJob.DispatchResult result = dispatchJob.execute(tier);

			if (result.success()) {
				return ResponseEntity.ok(result);
			}
			else {
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
			}
		}
		catch (Exception e) {
			log.error("Error dispatching tier {}: {}", tier, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(DispatchJob.DispatchResult.failed(tier, e.getMessage()));
		}
	}

	/**
	 * Process a Pub/Sub push message.
	 * Called by Pub/Sub subscription with push delivery.
	 *
	 * The Pub/Sub message format:
	 * {
	 * "message": {
	 * "data": "base64-encoded-json",
	 * "attributes": {...},
	 * "messageId": "..."
	 * },
	 * "subscription": "projects/.../subscriptions/..."
	 * }
	 */
	@PostMapping("/process")
	public ResponseEntity<ProcessResponse> processMessage(@RequestBody Map<String, Object> payload) {
		String pubsubMessageId = null;

		try {
			// Extract message from Pub/Sub envelope
			@SuppressWarnings("unchecked")
			Map<String, Object> message = (Map<String, Object>) payload.get("message");

			if (message == null) {
				log.warn("Invalid Pub/Sub message: missing 'message' field");
				return ResponseEntity.badRequest().body(new ProcessResponse(false, null, "Missing message field", 0));
			}

			pubsubMessageId = (String) message.get("messageId");
			String data = (String) message.get("data");

			if (data == null || data.isEmpty()) {
				log.warn("Invalid Pub/Sub message {}: missing 'data' field", pubsubMessageId);
				return ResponseEntity.badRequest().body(new ProcessResponse(false, pubsubMessageId, "Missing data", 0));
			}

			// Decode base64 data
			byte[] decodedData = Base64.getDecoder().decode(data);
			String json = new String(decodedData);

			// Parse deployment batch message
			DeploymentBatchMessage batchMessage = objectMapper.readValue(json, DeploymentBatchMessage.class);

			log.info("Received Pub/Sub message {}: batch {} with {} symbol sets", pubsubMessageId,
					batchMessage.getMessageId(), batchMessage.getSymbolSets().size());

			// Process the batch
			SymbolSetProcessorJob.ProcessingResult result = processorJob.process(batchMessage);

			if (result.isSuccess()) {
				return ResponseEntity.ok(new ProcessResponse(true, pubsubMessageId, null, result.durationMs()));
			}
			else {
				// Return 200 to acknowledge but indicate processing had errors
				return ResponseEntity.ok(
						new ProcessResponse(false, pubsubMessageId, "Processing completed with errors", result.durationMs()));
			}

		}
		catch (Exception e) {
			log.error("Error processing Pub/Sub message {}: {}", pubsubMessageId, e.getMessage(), e);

			// Return 500 to trigger retry (Pub/Sub will re-deliver)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ProcessResponse(false, pubsubMessageId, e.getMessage(), 0));
		}
	}

	/**
	 * Response for process endpoint.
	 */
	public record ProcessResponse(boolean success, String messageId, String error, long durationMs) {
	}

}
