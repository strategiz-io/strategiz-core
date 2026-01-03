package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestRunEntity;
import io.strategiz.data.testing.entity.TestRunStatus;
import io.strategiz.data.testing.entity.TestTrigger;
import io.strategiz.data.testing.entity.TestExecutionLevel;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Firestore implementation of TestRunRepository
 * Manages test execution runs in test-runs collection
 */
@Repository
public class TestRunRepositoryImpl extends BaseRepository<TestRunEntity> implements TestRunRepository {

    private static final Logger log = LoggerFactory.getLogger(TestRunRepositoryImpl.class);

    @Autowired
    public TestRunRepositoryImpl(Firestore firestore) {
        super(firestore, TestRunEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-testing";
    }

    // ===============================
    // Basic CRUD Operations (delegate to BaseRepository)
    // ===============================

    @Override
    public TestRunEntity save(TestRunEntity testRun, String createdBy) {
        return super.save(testRun, createdBy);
    }

    @Override
    public Optional<TestRunEntity> findById(String id) {
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
    public List<TestRunEntity> findByAppId(String appId) {
        try {
            log.debug("Finding test runs by app ID: {}", appId);
            return findByField("appId", appId);
        } catch (Exception e) {
            log.error("Failed to find test runs for app: {}", appId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "appId=" + appId);
        }
    }

    @Override
    public List<TestRunEntity> findByStatus(TestRunStatus status) {
        try {
            log.debug("Finding test runs by status: {}", status);
            return findByField("status", status);
        } catch (Exception e) {
            log.error("Failed to find test runs by status: {}", status, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "status=" + status);
        }
    }

    @Override
    public List<TestRunEntity> findByTrigger(TestTrigger trigger) {
        try {
            log.debug("Finding test runs by trigger: {}", trigger);
            return findByField("trigger", trigger);
        } catch (Exception e) {
            log.error("Failed to find test runs by trigger: {}", trigger, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "trigger=" + trigger);
        }
    }

    @Override
    public List<TestRunEntity> findByHierarchy(TestExecutionLevel level, String appId, String moduleId, String suiteId, String testId) {
        try {
            log.debug("Finding test runs by hierarchy - level: {}, app: {}, module: {}, suite: {}, test: {}",
                level, appId, moduleId, suiteId, testId);

            Query query = getCollection()
                    .whereEqualTo("level", level)
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("isActive", true);

            if (moduleId != null) {
                query = query.whereEqualTo("moduleId", moduleId);
            }
            if (suiteId != null) {
                query = query.whereEqualTo("suiteId", suiteId);
            }
            if (testId != null) {
                query = query.whereEqualTo("testId", testId);
            }

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestRunEntity entity = doc.toObject(TestRunEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test runs for hierarchy", e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "level=" + level);
        }
    }

    @Override
    public List<TestRunEntity> findRecent(int limit) {
        try {
            log.debug("Finding {} most recent test runs", limit);

            Query query = getCollection()
                    .whereEqualTo("isActive", true)
                    .orderBy("startTime", Query.Direction.DESCENDING)
                    .limit(limit);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestRunEntity entity = doc.toObject(TestRunEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find recent test runs", e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "limit=" + limit);
        }
    }

    @Override
    public List<TestRunEntity> findByTimeRange(Instant startTime, Instant endTime) {
        try {
            log.debug("Finding test runs between {} and {}", startTime, endTime);

            Query query = getCollection()
                    .whereEqualTo("isActive", true)
                    .whereGreaterThanOrEqualTo("startTime", startTime)
                    .whereLessThanOrEqualTo("startTime", endTime)
                    .orderBy("startTime", Query.Direction.DESCENDING);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestRunEntity entity = doc.toObject(TestRunEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test runs by time range", e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "startTime=" + startTime + ", endTime=" + endTime);
        }
    }

    @Override
    public TestRunEntity findLatestCiRun(TestExecutionLevel level, String appId, String moduleId, String suiteId) {
        try {
            log.debug("Finding latest CI run - level: {}, app: {}, module: {}, suite: {}", level, appId, moduleId, suiteId);

            Query query = getCollection()
                    .whereEqualTo("trigger", TestTrigger.CI_CD)
                    .whereEqualTo("level", level)
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("isActive", true);

            if (moduleId != null) {
                query = query.whereEqualTo("moduleId", moduleId);
            }
            if (suiteId != null) {
                query = query.whereEqualTo("suiteId", suiteId);
            }

            query = query.orderBy("startTime", Query.Direction.DESCENDING).limit(1);

            List<TestRunEntity> runs = query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestRunEntity entity = doc.toObject(TestRunEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());

            if (runs.isEmpty()) {
                log.debug("No CI run found for specified hierarchy");
                return null;
            }
            return runs.get(0);
        } catch (Exception e) {
            log.error("Failed to find latest CI run", e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "level=" + level);
        }
    }

    @Override
    public List<TestRunEntity> findRunning() {
        try {
            log.debug("Finding all currently running tests");
            return findByField("status", TestRunStatus.RUNNING);
        } catch (Exception e) {
            log.error("Failed to find running tests", e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "status=RUNNING");
        }
    }

    @Override
    public long countByAppIdAndStatus(String appId, TestRunStatus status) {
        try {
            log.debug("Counting test runs for app: {} with status: {}", appId, status);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("status", status)
                    .whereEqualTo("isActive", true);

            return query.get().get().size();
        } catch (Exception e) {
            log.error("Failed to count test runs for app: {} and status: {}", appId, status, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestRunEntity", "appId=" + appId + ", status=" + status);
        }
    }
}
