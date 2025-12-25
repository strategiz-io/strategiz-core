package io.strategiz.data.user.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.user.entity.UserFollowEntity;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for UserFollow entities using Firestore.
 * Used internally by CRUD repository implementations.
 */
@Repository
public class UserFollowBaseRepository extends BaseRepository<UserFollowEntity> {

    public UserFollowBaseRepository(Firestore firestore) {
        super(firestore, UserFollowEntity.class);
    }

    /**
     * Find all users that a specific user is following.
     */
    public List<UserFollowEntity> findByFollowerId(String followerId) {
        return findByField("followerId", followerId);
    }

    /**
     * Find all followers of a specific user.
     */
    public List<UserFollowEntity> findByFollowingId(String followingId) {
        return findByField("followingId", followingId);
    }

    /**
     * Check if a follow relationship exists.
     */
    public Optional<UserFollowEntity> findByFollowerAndFollowing(String followerId, String followingId) {
        String id = UserFollowEntity.generateId(followerId, followingId);
        return findById(id);
    }

    /**
     * Check if a user is following another user.
     */
    public boolean isFollowing(String followerId, String followingId) {
        return findByFollowerAndFollowing(followerId, followingId).isPresent();
    }

    /**
     * Count how many users a specific user is following.
     */
    public int countFollowing(String followerId) {
        return findByFollowerId(followerId).size();
    }

    /**
     * Count how many followers a specific user has.
     */
    public int countFollowers(String followingId) {
        return findByFollowingId(followingId).size();
    }

    /**
     * Find followers with pagination, ordered by followedAt date (newest first).
     */
    public List<UserFollowEntity> findFollowersOrderByDate(String followingId, int limit) {
        try {
            List<QueryDocumentSnapshot> docs = getCollection()
                    .whereEqualTo("followingId", followingId)
                    .whereEqualTo("isActive", true)
                    .orderBy("followedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            return docs.stream()
                    .map(doc -> {
                        UserFollowEntity entity = doc.toObject(UserFollowEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "UserFollowEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserFollowEntity");
        }
    }

    /**
     * Find following with pagination, ordered by followedAt date (newest first).
     */
    public List<UserFollowEntity> findFollowingOrderByDate(String followerId, int limit) {
        try {
            List<QueryDocumentSnapshot> docs = getCollection()
                    .whereEqualTo("followerId", followerId)
                    .whereEqualTo("isActive", true)
                    .orderBy("followedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            return docs.stream()
                    .map(doc -> {
                        UserFollowEntity entity = doc.toObject(UserFollowEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "UserFollowEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserFollowEntity");
        }
    }
}
