package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestResultEntity;
import io.strategiz.data.testing.entity.TestResultStatus;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Firestore implementation of TestResultRepository
 * Manages individual test results in test-runs/{runId}/results subcollection
 */
@Repository
public class TestResultRepositoryImpl extends SubcollectionRepository<TestResultEntity> implements TestResultRepository {

    private static final Logger log = LoggerFactory.getLogger(TestResultRepositoryImpl.class);

    @Autowired
    public TestResultRepositoryImpl(Firestore firestore) {
        super(firestore, TestResultEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-testing";
    }

    @Override
    protected String getParentCollectionName() {
        return "test-runs";
    }

    @Override
    protected String getSubcollectionName() {
        return "results";
    }

    // ===============================
    // Basic Subcollection CRUD Operations (delegate to SubcollectionRepository)
    // ===============================

    @Override
    public TestResultEntity saveInSubcollection(String runId, TestResultEntity result, String createdBy) {
        return super.saveInSubcollection(runId, result, createdBy);
    }

    @Override
    public Optional<TestResultEntity> findByIdInSubcollection(String runId, String resultId) {
        return super.findByIdInSubcollection(runId, resultId);
    }

    @Override
    public List<TestResultEntity> findAllInSubcollection(String runId) {
        return super.findAllInSubcollection(runId);
    }

    @Override
    public boolean deleteInSubcollection(String runId, String resultId, String deletedBy) {
        return super.deleteInSubcollection(runId, resultId, deletedBy);
    }

    // ===============================
    // Custom Query Operations
    // ===============================

    @Override
    public List<TestResultEntity> findByRunId(String runId) {
        try {
            log.debug("Finding all test results for run: {}", runId);
            return findAllInSubcollection(runId);
        } catch (Exception e) {
            log.error("Failed to find test results for run: {}", runId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestResultEntity", "runId=" + runId);
        }
    }

    @Override
    public List<TestResultEntity> findByRunIdAndStatus(String runId, TestResultStatus status) {
        try {
            log.debug("Finding test results for run: {} with status: {}", runId, status);
            return findAllInSubcollection(runId).stream()
                    .filter(entity -> entity.getStatus() == status)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test results for run: {} and status: {}", runId, status, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestResultEntity", "runId=" + runId + ", status=" + status);
        }
    }

    @Override
    public List<TestResultEntity> findFailedByRunId(String runId) {
        try {
            log.debug("Finding failed test results for run: {}", runId);
            return findByRunIdAndStatus(runId, TestResultStatus.FAILED);
        } catch (Exception e) {
            log.error("Failed to find failed test results for run: {}", runId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestResultEntity", "runId=" + runId);
        }
    }

    @Override
    public List<TestResultEntity> findWithScreenshotsByRunId(String runId) {
        try {
            log.debug("Finding test results with screenshots for run: {}", runId);
            return findAllInSubcollection(runId).stream()
                    .filter(entity -> entity.getScreenshots() != null && !entity.getScreenshots().isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test results with screenshots for run: {}", runId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestResultEntity", "runId=" + runId);
        }
    }

    @Override
    public long countByRunIdAndStatus(String runId, TestResultStatus status) {
        try {
            log.debug("Counting test results for run: {} with status: {}", runId, status);
            return findByRunIdAndStatus(runId, status).size();
        } catch (Exception e) {
            log.error("Failed to count test results for run: {} and status: {}", runId, status, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestResultEntity", "runId=" + runId + ", status=" + status);
        }
    }

    @Override
    public void deleteByRunId(String runId) {
        try {
            log.warn("Deleting all test results for run: {}", runId);

            List<TestResultEntity> results = findAllInSubcollection(runId);
            for (TestResultEntity result : results) {
                deleteInSubcollection(runId, result.getId(), "system");
            }

            log.info("Deleted {} test results for run: {}", results.size(), runId);
        } catch (Exception e) {
            log.error("Failed to delete test results for run: {}", runId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.BULK_OPERATION_FAILED, e, "TestResultEntity", "runId=" + runId);
        }
    }
}
