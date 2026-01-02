package io.strategiz.service.console.service.tests;

import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.data.testing.entity.TestCaseEntity;
import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestSuiteEntity;
import io.strategiz.data.testing.repository.TestAppRepository;
import io.strategiz.data.testing.repository.TestCaseRepository;
import io.strategiz.data.testing.repository.TestModuleRepository;
import io.strategiz.data.testing.repository.TestSuiteRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving test hierarchy data
 * Provides methods to browse apps, modules, suites, and individual tests
 */
@Service
public class TestHierarchyService extends BaseService {

	private static final Logger log = LoggerFactory.getLogger(TestHierarchyService.class);

	private final TestAppRepository testAppRepository;

	private final TestModuleRepository testModuleRepository;

	private final TestSuiteRepository testSuiteRepository;

	private final TestCaseRepository testCaseRepository;

	@Autowired
	public TestHierarchyService(TestAppRepository testAppRepository, TestModuleRepository testModuleRepository,
			TestSuiteRepository testSuiteRepository, TestCaseRepository testCaseRepository) {
		this.testAppRepository = testAppRepository;
		this.testModuleRepository = testModuleRepository;
		this.testSuiteRepository = testSuiteRepository;
		this.testCaseRepository = testCaseRepository;
	}

	@Override
	protected String getModuleName() {
		return "TEST_HIERARCHY_SERVICE";
	}

	/**
	 * Get all test applications
	 */
	public List<TestAppResponse> getAllApps() {
		log.debug("Fetching all test applications");

		List<TestAppEntity> apps = testAppRepository.findAll();

		return apps.stream().map(this::convertToAppResponse).collect(Collectors.toList());
	}

	/**
	 * Get detailed information for a specific app
	 */
	public TestAppDetailResponse getAppDetail(String appId) {
		log.debug("Fetching detailed info for app: {}", appId);

		TestAppEntity app = testAppRepository.findById(appId)
			.orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_APP_NOT_FOUND,
					"Test app not found: " + appId));

		TestAppDetailResponse response = new TestAppDetailResponse();
		response.setId(app.getId());
		response.setName(app.getDisplayName());
		response.setDescription(app.getDescription());
		response.setFramework(app.getType() != null ? app.getType().name() : null);
		response.setBasePath(app.getCodeDirectory());
		response.setModuleCount(app.getTotalModules());
		response.setSuiteCount(0); // suiteCount not available in entity, would need to be calculated
		response.setTotalTestCount(app.getTotalTests());
		response.setEstimatedDurationSeconds(app.getEstimatedDurationSeconds());
		response.setLastExecutedAt(null); // lastExecutedAt not tracked in TestAppEntity yet
		response.setLastExecutionStatus(null); // lastExecutionStatus not tracked in TestAppEntity yet
		response.setLastRunId(null); // lastRunId not tracked in TestAppEntity yet
		response.setCreatedAt(app.getCreatedDate() != null ? app.getCreatedDate().toDate().toInstant() : null);
		response.setUpdatedAt(app.getModifiedDate() != null ? app.getModifiedDate().toDate().toInstant() : null);
		response.setCreatedBy(app.getCreatedBy());

		// Load nested modules
		List<TestModuleEntity> modules = testModuleRepository.findByAppId(appId);
		response.setModules(modules.stream().map(this::convertToModuleResponse).collect(Collectors.toList()));

		return response;
	}

	/**
	 * Get all modules for an app
	 */
	public List<TestModuleResponse> getModulesByApp(String appId) {
		log.debug("Fetching modules for app: {}", appId);

		// Verify app exists
		testAppRepository.findById(appId).orElseThrow(() -> new StrategizException(
				ServiceConsoleErrorDetails.TEST_APP_NOT_FOUND, "Test app not found: " + appId));

		List<TestModuleEntity> modules = testModuleRepository.findByAppId(appId);

		return modules.stream().map(this::convertToModuleResponse).collect(Collectors.toList());
	}

	/**
	 * Get all suites for a module
	 */
	public List<TestSuiteResponse> getSuitesByModule(String appId, String moduleId) {
		log.debug("Fetching suites for app: {}, module: {}", appId, moduleId);

		// Verify module exists
		testModuleRepository.findByAppIdAndModuleId(appId, moduleId)
			.orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_MODULE_NOT_FOUND,
					"Test module not found: " + moduleId));

		List<TestSuiteEntity> suites = testSuiteRepository.findByAppIdAndModuleId(appId, moduleId);

		return suites.stream().map(this::convertToSuiteResponse).collect(Collectors.toList());
	}

	/**
	 * Get all test cases for a suite
	 */
	public List<TestCaseResponse> getTestsBySuite(String appId, String moduleId, String suiteId) {
		log.debug("Fetching tests for app: {}, module: {}, suite: {}", appId, moduleId, suiteId);

		// Verify suite exists
		testSuiteRepository.findByAppIdModuleIdAndSuiteId(appId, moduleId, suiteId)
			.orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_SUITE_NOT_FOUND,
					"Test suite not found: " + suiteId));

		List<TestCaseEntity> tests = testCaseRepository.findByAppIdAndModuleIdAndSuiteId(appId, moduleId, suiteId);

		return tests.stream().map(this::convertToTestCaseResponse).collect(Collectors.toList());
	}

	/**
	 * Convert TestAppEntity to TestAppResponse
	 */
	private TestAppResponse convertToAppResponse(TestAppEntity app) {
		// Convert Google Cloud Timestamp to Instant for API response
		java.time.Instant createdAt = app.getCreatedDate() != null
				? app.getCreatedDate().toDate().toInstant()
				: null;
		java.time.Instant updatedAt = app.getModifiedDate() != null
				? app.getModifiedDate().toDate().toInstant()
				: null;

		return new TestAppResponse(app.getId(), app.getDisplayName(), app.getDescription(),
				app.getType() != null ? app.getType().name() : null, app.getTotalModules(),
				0, // suiteCount not available in entity, would need to be calculated
				app.getTotalTests(), app.getEstimatedDurationSeconds(),
				null, // lastExecutedAt not tracked in TestAppEntity yet
				null, // lastExecutionStatus not tracked in TestAppEntity yet
				createdAt, updatedAt);
	}

	/**
	 * Convert TestModuleEntity to TestModuleResponse
	 */
	private TestModuleResponse convertToModuleResponse(TestModuleEntity module) {
		// Convert Google Cloud Timestamp to Instant for API response
		java.time.Instant createdAt = module.getCreatedDate() != null
				? module.getCreatedDate().toDate().toInstant()
				: null;
		java.time.Instant updatedAt = module.getModifiedDate() != null
				? module.getModifiedDate().toDate().toInstant()
				: null;

		return new TestModuleResponse(module.getId(), module.getAppId(), module.getDisplayName(),
				module.getDescription(), module.getModulePath(), module.getFramework().name(),
				module.getTotalSuites(), module.getTotalTests(), module.getEstimatedDurationSeconds(),
				null, // lastExecutedAt not tracked in TestModuleEntity yet
				null, // lastExecutionStatus not tracked in TestModuleEntity yet
				createdAt, updatedAt);
	}

	/**
	 * Convert TestSuiteEntity to TestSuiteResponse
	 */
	private TestSuiteResponse convertToSuiteResponse(TestSuiteEntity suite) {
		// Convert Google Cloud Timestamp to Instant for API response
		java.time.Instant createdAt = suite.getCreatedDate() != null
				? suite.getCreatedDate().toDate().toInstant()
				: null;
		java.time.Instant updatedAt = suite.getModifiedDate() != null
				? suite.getModifiedDate().toDate().toInstant()
				: null;

		return new TestSuiteResponse(suite.getId(), suite.getAppId(), suite.getModuleId(), suite.getDisplayName(),
				suite.getDescription(), suite.getFilePath(),
				null, // framework not available in TestSuiteEntity
				suite.getTotalTests(), suite.getEstimatedDurationSeconds(),
				null, // lastExecutedAt not tracked in TestSuiteEntity yet
				null, // lastExecutionStatus not tracked in TestSuiteEntity yet
				null, // tags not available in TestSuiteEntity
				createdAt, updatedAt);
	}

	/**
	 * Convert TestCaseEntity to TestCaseResponse
	 */
	private TestCaseResponse convertToTestCaseResponse(TestCaseEntity test) {
		return new TestCaseResponse(test.getId(), test.getAppId(), test.getModuleId(), test.getSuiteId(),
				test.getName(), test.getDescription(), test.getClassName(), test.getMethodName(), test.getFilePath(),
				test.getLineNumber(), test.getFramework(), // Already a String
				test.getEstimatedDurationSeconds(), test.getLastExecutedAt(), test.getLastExecutionStatus(), // Already a String
				test.getTags(), test.getCreatedAt(), test.getUpdatedAt());
	}

}
