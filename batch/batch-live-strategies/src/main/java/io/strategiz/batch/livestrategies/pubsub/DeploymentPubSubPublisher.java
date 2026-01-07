package io.strategiz.batch.livestrategies.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publishes deployment batch messages to Google Cloud Pub/Sub.
 *
 * Topic: publisher-deployment-processing
 * Subscription: subscriber-deployment-processing (push to strategiz-console)
 */
@Component
@ConditionalOnProperty(name = "live-strategies.pubsub.enabled", havingValue = "true")
public class DeploymentPubSubPublisher {

	private static final Logger log = LoggerFactory.getLogger(DeploymentPubSubPublisher.class);

	private static final String TOPIC_NAME = "publisher-deployment-processing";

	private final ObjectMapper objectMapper;

	private Publisher publisher;

	private final AtomicInteger pendingMessages = new AtomicInteger(0);

	@Value("${gcp.project-id:strategiz-io}")
	private String projectId;

	@Value("${live-strategies.pubsub.enabled:false}")
	private boolean pubsubEnabled;

	public DeploymentPubSubPublisher() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	@PostConstruct
	public void init() {
		if (!pubsubEnabled) {
			log.info("Pub/Sub publishing disabled");
			return;
		}

		try {
			TopicName topicName = TopicName.of(projectId, TOPIC_NAME);
			publisher = Publisher.newBuilder(topicName).build();
			log.info("DeploymentPubSubPublisher initialized for topic: {}", topicName);
		}
		catch (IOException e) {
			log.error("Failed to create Pub/Sub publisher: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to initialize Pub/Sub publisher", e);
		}
	}

	@PreDestroy
	public void shutdown() {
		if (publisher != null) {
			try {
				// Wait for pending messages
				int pending = pendingMessages.get();
				if (pending > 0) {
					log.info("Waiting for {} pending messages before shutdown", pending);
				}

				publisher.shutdown();
				publisher.awaitTermination(30, TimeUnit.SECONDS);
				log.info("Pub/Sub publisher shutdown complete");
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Publisher shutdown interrupted");
			}
		}
	}

	/**
	 * Publish a deployment batch message to Pub/Sub.
	 * @param message The batch message containing symbol sets with alert/bot IDs
	 * @return The message ID from Pub/Sub
	 */
	public String publish(DeploymentBatchMessage message) {
		if (!pubsubEnabled || publisher == null) {
			log.debug("Pub/Sub disabled, skipping publish for message: {}", message.getMessageId());
			return message.getMessageId();
		}

		try {
			// Serialize to JSON
			String json = objectMapper.writeValueAsString(message);
			ByteString data = ByteString.copyFromUtf8(json);

			// Build Pub/Sub message with attributes for filtering
			PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
				.setData(data)
				.putAttributes("tier", message.getTier())
				.putAttributes("messageId", message.getMessageId())
				.putAttributes("symbolSetCount", String.valueOf(message.getSymbolSets().size()))
				.putAttributes("totalAlerts", String.valueOf(message.getTotalAlerts()))
				.putAttributes("totalBots", String.valueOf(message.getTotalBots()))
				.build();

			// Publish asynchronously
			pendingMessages.incrementAndGet();
			ApiFuture<String> future = publisher.publish(pubsubMessage);

			// Add callback for logging
			ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
				@Override
				public void onSuccess(String publishedMessageId) {
					pendingMessages.decrementAndGet();
					log.debug("Published message {} (Pub/Sub ID: {})", message.getMessageId(), publishedMessageId);
				}

				@Override
				public void onFailure(Throwable t) {
					pendingMessages.decrementAndGet();
					log.error("Failed to publish message {}: {}", message.getMessageId(), t.getMessage(), t);
				}
			}, MoreExecutors.directExecutor());

			// Return immediately (async), but get the future result for the message ID
			return future.get(10, TimeUnit.SECONDS);

		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize message: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to serialize deployment batch message", e);
		}
		catch (Exception e) {
			log.error("Failed to publish message: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to publish to Pub/Sub", e);
		}
	}

	/**
	 * Check if publisher is available and enabled.
	 */
	public boolean isAvailable() {
		return pubsubEnabled && publisher != null;
	}

	/**
	 * Get count of pending messages.
	 */
	public int getPendingMessageCount() {
		return pendingMessages.get();
	}

}
