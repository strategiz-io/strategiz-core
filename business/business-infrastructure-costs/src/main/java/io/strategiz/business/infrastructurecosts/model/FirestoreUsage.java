package io.strategiz.business.infrastructurecosts.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Firestore usage metrics for a day
 */
public record FirestoreUsage(String date, List<CollectionUsage> collections, long totalReads, long totalWrites,
		BigDecimal totalEstimatedCost) {
	public record CollectionUsage(String name, long reads, long writes, BigDecimal estimatedCost) {
	}
}
