package io.strategiz.service.cryptotoken.listener;

import io.strategiz.client.stripe.event.StratPackPurchaseEvent;
import io.strategiz.service.cryptotoken.service.CryptoTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for STRAT pack purchase events.
 * Handles crediting STRAT tokens to user wallets after successful Stripe checkout.
 */
@Component
public class StratPackPurchaseListener {

	private static final Logger logger = LoggerFactory.getLogger(StratPackPurchaseListener.class);

	private final CryptoTokenService cryptoTokenService;

	public StratPackPurchaseListener(CryptoTokenService cryptoTokenService) {
		this.cryptoTokenService = cryptoTokenService;
	}

	/**
	 * Handle STRAT pack purchase completion.
	 * Credits the purchased STRAT tokens to the user's wallet.
	 *
	 * @param event The purchase event containing user and pack details
	 */
	@EventListener
	public void handleStratPackPurchase(StratPackPurchaseEvent event) {
		logger.info("Handling STRAT pack purchase event: userId={}, packId={}, stratAmount={}, sessionId={}",
				event.getUserId(), event.getPackId(), event.getStratAmount(), event.getSessionId());

		try {
			cryptoTokenService.creditStratPackPurchase(event.getUserId(), event.getStratAmount(), event.getPackId(),
					event.getSessionId());

			logger.info("Successfully credited {} STRAT to user {} for pack {} purchase", event.getStratAmount(),
					event.getUserId(), event.getPackId());
		}
		catch (Exception e) {
			logger.error("Failed to credit STRAT pack purchase for user {}: {}", event.getUserId(), e.getMessage(), e);
			// Don't rethrow - we don't want to break the webhook response
			// The transaction should be idempotent based on sessionId anyway
		}
	}

}
