package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestCaseEntity;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Firestore implementation of TestCaseRepository
 * Manages individual test cases in tests collection
 */
@Repository
public class TestCaseRepositoryImpl extends BaseRepository<TestCaseEntity> implements TestCaseRepository {

    private static final Logger log = LoggerFactory.getLogger(TestCaseRepositoryImpl.class);

    @Autowired
    public TestCaseRepositoryImpl(Firestore firestore) {
        super(firestore, TestCaseEntity.class);
    }

    // ===============================
    // Basic CRUD Operations (delegate to BaseRepository)
    // ===============================

    @Override
    public TestCaseEntity save(TestCaseEntity testCase, String createdBy) {
        return super.save(testCase, createdBy);
    }

    @Override
    public Optional<TestCaseEntity> findById(String id) {
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
    public List<TestCaseEntity> findByAppIdAndModuleIdAndSuiteId(String appId, String moduleId, String suiteId) {
        try {
            log.debug("Finding test cases by app ID: {}, module ID: {}, and suite ID: {}", appId, moduleId, suiteId);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("moduleId", moduleId)
                    .whereEqualTo("suiteId", suiteId)
                    .whereEqualTo("isActive", true);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestCaseEntity entity = doc.toObject(TestCaseEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test cases for app: {}, module: {}, and suite: {}", appId, moduleId, suiteId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestCaseEntity", "appId=" + appId + ", moduleId=" + moduleId + ", suiteId=" + suiteId);
        }
    }

    @Override
    public TestCaseEntity findByAppIdAndModuleIdAndSuiteIdAndMethodName(String appId, String moduleId, String suiteId, String methodName) {
        try {
            log.debug("Finding test case by app ID: {}, module ID: {}, suite ID: {}, and method name: {}", appId, moduleId, suiteId, methodName);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("moduleId", moduleId)
                    .whereEqualTo("suiteId", suiteId)
                    .whereEqualTo("methodName", methodName)
                    .whereEqualTo("isActive", true)
                    .limit(1);

            List<TestCaseEntity> testCases = query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestCaseEntity entity = doc.toObject(TestCaseEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());

            if (testCases.isEmpty()) {
                log.debug("No test case found with app ID: {}, module ID: {}, suite ID: {}, and method name: {}", appId, moduleId, suiteId, methodName);
                return null;
            }
            return testCases.get(0);
        } catch (Exception e) {
            log.error("Failed to find test case for app: {}, module: {}, suite: {}, and method name: {}", appId, moduleId, suiteId, methodName, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestCaseEntity", "appId=" + appId + ", moduleId=" + moduleId + ", suiteId=" + suiteId + ", methodName=" + methodName);
        }
    }

    @Override
    public List<TestCaseEntity> findByAppIdAndModuleId(String appId, String moduleId) {
        try {
            log.debug("Finding all test cases by app ID: {} and module ID: {}", appId, moduleId);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("moduleId", moduleId)
                    .whereEqualTo("isActive", true);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestCaseEntity entity = doc.toObject(TestCaseEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find all test cases for app: {} and module: {}", appId, moduleId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestCaseEntity", "appId=" + appId + ", moduleId=" + moduleId);
        }
    }
}
