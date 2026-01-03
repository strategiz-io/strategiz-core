package io.strategiz.data.testing.repository;

import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestFramework;
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
 * Firestore implementation of TestModuleRepository
 * Manages test modules in modules collection
 */
@Repository
public class TestModuleRepositoryImpl extends BaseRepository<TestModuleEntity> implements TestModuleRepository {

    private static final Logger log = LoggerFactory.getLogger(TestModuleRepositoryImpl.class);

    @Autowired
    public TestModuleRepositoryImpl(Firestore firestore) {
        super(firestore, TestModuleEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-testing";
    }

    // ===============================
    // Basic CRUD Operations (delegate to BaseRepository)
    // ===============================

    @Override
    public TestModuleEntity save(TestModuleEntity module, String createdBy) {
        return super.save(module, createdBy);
    }

    @Override
    public Optional<TestModuleEntity> findById(String id) {
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
    public List<TestModuleEntity> findByAppId(String appId) {
        try {
            log.debug("Finding test modules by app ID: {}", appId);
            return findByField("appId", appId);
        } catch (Exception e) {
            log.error("Failed to find test modules for app: {}", appId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestModuleEntity", "appId=" + appId);
        }
    }

    @Override
    public List<TestModuleEntity> findByFramework(TestFramework framework) {
        try {
            log.debug("Finding test modules by framework: {}", framework);
            return findByField("framework", framework);
        } catch (Exception e) {
            log.error("Failed to find test modules by framework: {}", framework, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestModuleEntity", "framework=" + framework);
        }
    }

    @Override
    public List<TestModuleEntity> findByAppIdAndFramework(String appId, TestFramework framework) {
        try {
            log.debug("Finding test modules by app ID: {} and framework: {}", appId, framework);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("framework", framework)
                    .whereEqualTo("isActive", true);

            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestModuleEntity entity = doc.toObject(TestModuleEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find test modules for app: {} and framework: {}", appId, framework, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestModuleEntity", "appId=" + appId + ", framework=" + framework);
        }
    }

    @Override
    public TestModuleEntity findByAppIdAndDisplayName(String appId, String displayName) {
        try {
            log.debug("Finding test module by app ID: {} and display name: {}", appId, displayName);

            Query query = getCollection()
                    .whereEqualTo("appId", appId)
                    .whereEqualTo("displayName", displayName)
                    .whereEqualTo("isActive", true)
                    .limit(1);

            List<TestModuleEntity> modules = query.get().get().getDocuments().stream()
                    .map(doc -> {
                        TestModuleEntity entity = doc.toObject(TestModuleEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());

            if (modules.isEmpty()) {
                log.debug("No test module found with app ID: {} and display name: {}", appId, displayName);
                return null;
            }
            return modules.get(0);
        } catch (Exception e) {
            log.error("Failed to find test module for app: {} and display name: {}", appId, displayName, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestModuleEntity", "appId=" + appId + ", displayName=" + displayName);
        }
    }

    @Override
    public Optional<TestModuleEntity> findByAppIdAndModuleId(String appId, String moduleId) {
        try {
            log.debug("Finding test module by app ID: {} and module ID: {}", appId, moduleId);

            // The moduleId is the document ID
            Optional<TestModuleEntity> module = findById(moduleId);

            // Verify it belongs to the correct app
            if (module.isPresent() && !module.get().getAppId().equals(appId)) {
                log.debug("Module {} found but belongs to different app: {}", moduleId, module.get().getAppId());
                return Optional.empty();
            }

            return module;
        } catch (Exception e) {
            log.error("Failed to find test module for app: {} and module: {}", appId, moduleId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "TestModuleEntity", "appId=" + appId + ", moduleId=" + moduleId);
        }
    }
}
