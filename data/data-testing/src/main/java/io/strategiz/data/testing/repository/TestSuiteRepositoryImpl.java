package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestSuiteEntity;
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
 * Firestore implementation of TestSuiteRepository
 * Manages test suites in suites collection
 */
@Repository
public class TestSuiteRepositoryImpl extends BaseRepository<TestSuiteEntity> implements TestSuiteRepository {

    private static final Logger log = LoggerFactory.getLogger(TestSuiteRepositoryImpl.class);

    @Autowired
    public TestSuiteRepositoryImpl(Firestore firestore) {
        super(firestore, TestSuiteEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-testing";
    }

    // ===============================
    // Basic CRUD Operations (delegate to BaseRepository)
    // ===============================

    @Override
    public TestSuiteEntity save(TestSuiteEntity suite, String createdBy) {
        return super.save(suite, createdBy);
    }

    @Override
    public Optional<TestSuiteEntity> findById(String id) {
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
    public List<TestSuiteEntity> findByAppIdAndModuleId(String appId, String moduleId) {
        try {
            log.debug("Finding test suites by app ID: {} and module ID: {}", appId, moduleId);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("moduleId", moduleId)
                    .whereEqualTo("isActive", true);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestSuiteEntity entity = doc.toObject(TestSuiteEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test suites for app: {} and module: {}", appId, moduleId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestSuiteEntity", "appId=" + appId + ", moduleId=" + moduleId);
        }
    }

    @Override
    public TestSuiteEntity findByAppIdAndModuleIdAndDisplayName(String appId, String moduleId, String displayName) {
        try {
            log.debug("Finding test suite by app ID: {}, module ID: {}, and display name: {}", appId, moduleId, displayName);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("moduleId", moduleId)
                    .whereEqualTo("displayName", displayName)
                    .whereEqualTo("isActive", true)
                    .limit(1);

            List<TestSuiteEntity> suites = query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestSuiteEntity entity = doc.toObject(TestSuiteEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());

            if (suites.isEmpty()) {
                log.debug("No test suite found with app ID: {}, module ID: {}, and display name: {}", appId, moduleId, displayName);
                return null;
            }
            return suites.get(0);
        } catch (Exception e) {
            log.error("Failed to find test suite for app: {}, module: {}, and display name: {}", appId, moduleId, displayName, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestSuiteEntity", "appId=" + appId + ", moduleId=" + moduleId + ", displayName=" + displayName);
        }
    }

    @Override
    public List<TestSuiteEntity> findByClassName(String className) {
        try {
            log.debug("Finding test suites by class name: {}", className);
            return findByField("className", className);
        } catch (Exception e) {
            log.error("Failed to find test suites by class name: {}", className, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestSuiteEntity", "className=" + className);
        }
    }

    @Override
    public Optional<TestSuiteEntity> findByAppIdModuleIdAndSuiteId(String appId, String moduleId, String suiteId) {
        try {
            log.debug("Finding test suite by app ID: {}, module ID: {}, and suite ID: {}", appId, moduleId, suiteId);

            // The suiteId is the document ID
            Optional<TestSuiteEntity> suite = findById(suiteId);

            // Verify it belongs to the correct app and module
            if (suite.isPresent()) {
                TestSuiteEntity entity = suite.get();
                if (!entity.getAppId().equals(appId) || !entity.getModuleId().equals(moduleId)) {
                    log.debug("Suite {} found but belongs to different app/module: {}/{}", suiteId, entity.getAppId(), entity.getModuleId());
                    return Optional.empty();
                }
            }

            return suite;
        } catch (Exception e) {
            log.error("Failed to find test suite for app: {}, module: {}, suite: {}", appId, moduleId, suiteId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestSuiteEntity", "appId=" + appId + ", moduleId=" + moduleId + ", suiteId=" + suiteId);
        }
    }

    @Override
    public List<TestSuiteEntity> findByModuleId(String moduleId) {
        try {
            log.debug("Finding all test suites by module ID: {}", moduleId);

            Query query = getCollection()
                    .whereEqualTo("moduleId", moduleId)
                    .whereEqualTo("isActive", true);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestSuiteEntity entity = doc.toObject(TestSuiteEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find all test suites for module: {}", moduleId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestSuiteEntity", "moduleId=" + moduleId);
        }
    }
}
