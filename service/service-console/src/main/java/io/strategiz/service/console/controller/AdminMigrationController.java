package io.strategiz.service.console.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import io.strategiz.business.cryptotoken.CryptoTokenBusiness;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for one-time data migrations.
 */
@RestController
@RequestMapping("/v1/console/migrations")
@Tag(name = "Admin - Migrations", description = "One-time data migration endpoints for administrators")
public class AdminMigrationController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final Firestore firestore;

	private final CryptoTokenBusiness cryptoTokenBusiness;

	public AdminMigrationController(Firestore firestore,
			@Autowired(required = false) CryptoTokenBusiness cryptoTokenBusiness) {
		this.firestore = firestore;
		this.cryptoTokenBusiness = cryptoTokenBusiness;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Migrate subscription documents from legacy "monthlyCreditsAllowed/Used" fields
	 * to "monthlyStratAllowed/Used". Also initializes STRAT wallet allocations for
	 * existing subscribers who don't have wallet balances yet.
	 *
	 * <p>This migration is idempotent: running it multiple times is safe.</p>
	 *
	 * <p>What it does per user subscription document:</p>
	 * <ol>
	 *   <li>Copies monthlyCreditsAllowed → monthlyStratAllowed (if old field exists)</li>
	 *   <li>Copies monthlyCreditsUsed → monthlyStratUsed (if old field exists)</li>
	 *   <li>Removes old field names after copying</li>
	 *   <li>Optionally credits STRAT wallet allocation for existing paid subscribers</li>
	 * </ol>
	 */
	@PostMapping("/credits-to-strat")
	@Operation(summary = "Migrate credits to STRAT",
			description = "Renames monthlyCreditsAllowed/Used to monthlyStratAllowed/Used in all subscription documents and optionally grants STRAT wallet allocations")
	public ResponseEntity<Map<String, Object>> migrateCreditsToStrat(
			@RequestParam(defaultValue = "false") boolean dryRun,
			@RequestParam(defaultValue = "true") boolean grantWalletAllocation,
			HttpServletRequest request) {

		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("migrateCreditsToStrat", adminUserId,
				Map.of("dryRun", dryRun, "grantWalletAllocation", grantWalletAllocation));

		int totalUsers = 0;
		int migratedDocs = 0;
		int skippedDocs = 0;
		int walletAllocations = 0;
		int errors = 0;

		try {
			// Get all user documents
			List<QueryDocumentSnapshot> userDocs = firestore.collection("users").get().get().getDocuments();
			totalUsers = userDocs.size();

			log.info("Migration: Found {} users to process (dryRun={})", totalUsers, dryRun);

			for (QueryDocumentSnapshot userDoc : userDocs) {
				String userId = userDoc.getId();

				try {
					DocumentReference subRef = firestore.collection("users").document(userId)
						.collection("subscription")
						.document("current");

					DocumentSnapshot subDoc = subRef.get().get();

					if (!subDoc.exists()) {
						skippedDocs++;
						continue;
					}

					Map<String, Object> data = subDoc.getData();
					if (data == null) {
						skippedDocs++;
						continue;
					}

					boolean needsFieldMigration = data.containsKey("monthlyCreditsAllowed")
							|| data.containsKey("monthlyCreditsUsed");
					boolean alreadyMigrated = data.containsKey("monthlyStratAllowed");

					if (needsFieldMigration && !alreadyMigrated) {
						if (!dryRun) {
							Map<String, Object> updates = new HashMap<>();

							// Copy old values to new field names
							Object creditsAllowed = data.get("monthlyCreditsAllowed");
							Object creditsUsed = data.get("monthlyCreditsUsed");

							if (creditsAllowed != null) {
								updates.put("monthlyStratAllowed", creditsAllowed);
							}
							if (creditsUsed != null) {
								updates.put("monthlyStratUsed", creditsUsed);
							}

							// Remove old fields by setting to FieldValue.delete()
							updates.put("monthlyCreditsAllowed",
									com.google.cloud.firestore.FieldValue.delete());
							updates.put("monthlyCreditsUsed",
									com.google.cloud.firestore.FieldValue.delete());

							subRef.update(updates).get();
						}
						migratedDocs++;
						log.debug("Migration: Migrated subscription for user {}", userId);
					}
					else {
						skippedDocs++;
					}

					// Grant wallet allocation for existing paid subscribers
					if (grantWalletAllocation && cryptoTokenBusiness != null) {
						String tier = (String) data.get("tier");
						String status = (String) data.get("status");

						if ("active".equals(status) && tier != null) {
							SubscriptionTier subscriptionTier = SubscriptionTier.fromId(tier);
							if (subscriptionTier != null && !subscriptionTier.isFree()) {
								if (!dryRun) {
									try {
										cryptoTokenBusiness.creditMonthlyAllocation(userId,
												subscriptionTier.getMonthlyStrat());
										walletAllocations++;
									}
									catch (Exception walletError) {
										log.warn("Migration: Failed to credit wallet for user {}: {}", userId,
												walletError.getMessage());
									}
								}
								else {
									walletAllocations++;
								}
							}
						}
					}
				}
				catch (Exception userError) {
					errors++;
					log.error("Migration: Error processing user {}: {}", userId, userError.getMessage());
				}
			}
		}
		catch (Exception e) {
			log.error("Migration failed: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("dryRun", dryRun);
		response.put("totalUsers", totalUsers);
		response.put("migratedDocs", migratedDocs);
		response.put("skippedDocs", skippedDocs);
		response.put("walletAllocations", walletAllocations);
		response.put("errors", errors);

		log.info("Migration complete: total={}, migrated={}, skipped={}, walletAllocations={}, errors={}", totalUsers,
				migratedDocs, skippedDocs, walletAllocations, errors);

		return ResponseEntity.ok(response);
	}

}
