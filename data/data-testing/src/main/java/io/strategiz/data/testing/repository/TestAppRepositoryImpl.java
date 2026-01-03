package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.data.testing.entity.TestAppType;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Firestore implementation of TestAppRepository
 * Manages test applications in tests/apps collection
 */
@Repository
public class TestAppRepositoryImpl extends BaseRepository<TestAppEntity> implements TestAppRepository {

    private static final Logger log = LoggerFactory.getLogger(TestAppRepositoryImpl.class);

    @Autowired
    public TestAppRepositoryImpl(Firestore firestore) {
        super(firestore, TestAppEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-testing";
    }

    // ===============================
    // Basic CRUD Operations (delegate to BaseRepository)
    // ===============================

    @Override
    public TestAppEntity save(TestAppEntity app, String createdBy) {
        return super.save(app, createdBy);
    }

    @Override
    public Optional<TestAppEntity> findById(String id) {
        return super.findById(id);
    }

    @Override
    public boolean delete(String id, String deletedBy) {
        return super.delete(id, deletedBy);
    }

    // ===============================
    // Custom Query Operations
    // ===============================

    @Override
    public List<TestAppEntity> findByType(TestAppType type) {
        try {
            log.debug("Finding test apps by type: {}", type);
            return findByField("type", type);
        } catch (Exception e) {
            log.error("Failed to find test apps by type: {}", type, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestAppEntity", "type=" + type);
        }
    }

    @Override
    public TestAppEntity findByDisplayName(String displayName) {
        try {
            log.debug("Finding test app by display name: {}", displayName);
            List<TestAppEntity> apps = findByField("displayName", displayName);
            if (apps.isEmpty()) {
                log.debug("No test app found with display name: {}", displayName);
                return null;
            }
            return apps.get(0);
        } catch (Exception e) {
            log.error("Failed to find test app by display name: {}", displayName, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestAppEntity", displayName);
        }
    }

    @Override
    public List<TestAppEntity> findAllActive() {
        try {
            log.debug("Finding all active test apps");
            return findAll();
        } catch (Exception e) {
            log.error("Failed to find all active test apps", e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestAppEntity");
        }
    }
}
