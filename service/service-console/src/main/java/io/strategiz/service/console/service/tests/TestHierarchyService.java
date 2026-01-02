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
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;
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
			.orElseThrow(() -> new StrategizException(ServiceBaseErrorDetails.RESOURCE_NOT_FOUND,
					"Test app not found: " + appId));

		TestAppDetailResponse response = new TestAppDetailResponse();
		response.setId(app.getId());
		response.setName(app.getName());
		response.setDescription(app.getDescription());
		response.setFramework(app.getFramework().name());
		response.setBasePath(app.getBasePath());
		response.setModuleCount(app.getModuleCount());
		response.setSuiteCount(app.getSuiteCount());
		response.setTotalTestCount(app.getTotalTestCount());
		response.setEstimatedDurationSeconds(app.getEstimatedDurationSeconds());
		response.setLastExecutedAt(app.getLastExecutedAt());
		response.setLastExecutionStatus(
				app.getLastExecutionStatus() != null ? app.getLastExecutionStatus().name() : null);
		response.setLastRunId(app.getLastRunId());
		response.setCreatedAt(app.getCreatedAt());
		response.setUpdatedAt(app.getUpdatedAt());
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
				ServiceBaseErrorDetails.RESOURCE_NOT_FOUND, "Test app not found: " + appId));

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
			.orElseThrow(() -> new StrategizException(ServiceBaseErrorDetails.RESOURCE_NOT_FOUND,
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
			.orElseThrow(() -> new StrategizException(ServiceBaseErrorDetails.RESOURCE_NOT_FOUND,
					"Test suite not found: " + suiteId));

		List<TestCaseEntity> tests = testCaseRepository.findByAppIdAndModuleIdAndSuiteId(appId, moduleId, suiteId);

		return tests.stream().map(this::convertToTestCaseResponse).collect(Collectors.toList());
	}

	/**
	 * Convert TestAppEntity to TestAppResponse
	 */
	private TestAppResponse convertToAppResponse(TestAppEntity app) {
		return new TestAppResponse(app.getId(), app.getName(), app.getDescription(), app.getFramework().name(),
				app.getModuleCount(), app.getSuiteCount(), app.getTotalTestCount(),
				app.getEstimatedDurationSeconds(), app.getLastExecutedAt(),
				app.getLastExecutionStatus() != null ? app.getLastExecutionStatus().name() : null, app.getCreatedAt(),
				app.getUpdatedAt());
	}

	/**
	 * Convert TestModuleEntity to TestModuleResponse
	 */
	private TestModuleResponse convertToModuleResponse(TestModuleEntity module) {
		return new TestModuleResponse(module.getId(), module.getAppId(), module.getName(), module.getDescription(),
				module.getPath(), module.getFramework().name(), module.getSuiteCount(), module.getTotalTestCount(),
				module.getEstimatedDurationSeconds(), module.getLastExecutedAt(),
				module.getLastExecutionStatus() != null ? module.getLastExecutionStatus().name() : null,
				module.getCreatedAt(), module.getUpdatedAt());
	}

	/**
	 * Convert TestSuiteEntity to TestSuiteResponse
	 */
	private TestSuiteResponse convertToSuiteResponse(TestSuiteEntity suite) {
		return new TestSuiteResponse(suite.getId(), suite.getAppId(), suite.getModuleId(), suite.getName(),
				suite.getDescription(), suite.getFilePath(), suite.getFramework().name(), suite.getTestCount(),
				suite.getEstimatedDurationSeconds(), suite.getLastExecutedAt(),
				suite.getLastExecutionStatus() != null ? suite.getLastExecutionStatus().name() : null,
				suite.getTags(), suite.getCreatedAt(), suite.getUpdatedAt());
	}

	/**
	 * Convert TestCaseEntity to TestCaseResponse
	 */
	private TestCaseResponse convertToTestCaseResponse(TestCaseEntity test) {
		return new TestCaseResponse(test.getId(), test.getAppId(), test.getModuleId(), test.getSuiteId(),
				test.getName(), test.getDescription(), test.getClassName(), test.getMethodName(), test.getFilePath(),
				test.getLineNumber(), test.getFramework().name(), test.getEstimatedDurationSeconds(),
				test.getLastExecutedAt(),
				test.getLastExecutionStatus() != null ? test.getLastExecutionStatus().name() : null, test.getTags(),
				test.getCreatedAt(), test.getUpdatedAt());
	}

}
