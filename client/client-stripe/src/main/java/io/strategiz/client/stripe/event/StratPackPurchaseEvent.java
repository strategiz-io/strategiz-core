package io.strategiz.client.stripe.event;

import org.springframework.context.ApplicationEvent;

/**
 * Spring Event published when a STRAT pack purchase checkout is completed.
 *
 * <p>This event is published by the webhook controller and can be listened to
 * by any module that needs to process STRAT pack purchases (e.g., service-crypto-token).</p>
 */
public class StratPackPurchaseEvent extends ApplicationEvent {

	private final String sessionId;

	private final String userId;

	private final String packId;

	private final long stratAmount;

	private final String customerId;

	private final Long amountTotal;

	public StratPackPurchaseEvent(Object source, String sessionId, String userId, String packId, long stratAmount,
			String customerId, Long amountTotal) {
		super(source);
		this.sessionId = sessionId;
		this.userId = userId;
		this.packId = packId;
		this.stratAmount = stratAmount;
		this.customerId = customerId;
		this.amountTotal = amountTotal;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getUserId() {
		return userId;
	}

	public String getPackId() {
		return packId;
	}

	public long getStratAmount() {
		return stratAmount;
	}

	public String getCustomerId() {
		return customerId;
	}

	public Long getAmountTotal() {
		return amountTotal;
	}

}
