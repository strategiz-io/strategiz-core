package io.strategiz.data.provider.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of DeleteProviderDataRepository using ProviderDataBaseRepository.
 * Performs hard delete of provider data since it's cached/synced data that can be
 * re-fetched.
 */
@Repository
public class DeleteProviderDataRepositoryImpl implements DeleteProviderDataRepository {

	private static final Logger log = LoggerFactory.getLogger(DeleteProviderDataRepositoryImpl.class);

	private final ProviderDataBaseRepository baseRepository;

	@Autowired
	public DeleteProviderDataRepositoryImpl(ProviderDataBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public boolean deleteProviderData(String userId, String providerId) {
		log.info("Deleting provider data for userId={}, providerId={}", userId, providerId);

		try {
			CollectionReference collection = baseRepository.getUserScopedCollection(userId);

			// Delete the provider data document directly using providerId as document ID
			collection.document(providerId).delete().get();

			log.info("Successfully deleted provider data at users/{}/provider_data/{}", userId, providerId);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while deleting provider data for userId={}, providerId={}", userId, providerId, e);
			return false;
		}
		catch (ExecutionException e) {
			log.error("Failed to delete provider data for userId={}, providerId={}: {}", userId, providerId,
					e.getMessage(), e);
			return false;
		}
	}

	@Override
	public int deleteAllProviderData(String userId) {
		log.info("Deleting all provider data for userId={}", userId);

		try {
			CollectionReference collection = baseRepository.getUserScopedCollection(userId);
			List<QueryDocumentSnapshot> docs = collection.get().get().getDocuments();

			int deletedCount = 0;
			for (QueryDocumentSnapshot doc : docs) {
				doc.getReference().delete().get();
				deletedCount++;
			}

			log.info("Successfully deleted {} provider data documents for userId={}", deletedCount, userId);
			return deletedCount;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while deleting all provider data for userId={}", userId, e);
			return 0;
		}
		catch (ExecutionException e) {
			log.error("Failed to delete all provider data for userId={}: {}", userId, e.getMessage(), e);
			return 0;
		}
	}

	@Override
	public int deleteProviderDataByType(String userId, String accountType) {
		log.info("Deleting provider data by type for userId={}, accountType={}", userId, accountType);

		try {
			CollectionReference collection = baseRepository.getUserScopedCollection(userId);
			List<QueryDocumentSnapshot> docs = collection.whereEqualTo("provider_type", accountType)
				.get()
				.get()
				.getDocuments();

			int deletedCount = 0;
			for (QueryDocumentSnapshot doc : docs) {
				doc.getReference().delete().get();
				deletedCount++;
			}

			log.info("Successfully deleted {} provider data documents of type {} for userId={}", deletedCount,
					accountType, userId);
			return deletedCount;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while deleting provider data by type for userId={}, accountType={}", userId,
					accountType, e);
			return 0;
		}
		catch (ExecutionException e) {
			log.error("Failed to delete provider data by type for userId={}, accountType={}: {}", userId, accountType,
					e.getMessage(), e);
			return 0;
		}
	}

}
