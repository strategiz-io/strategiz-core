package io.strategiz.service.console.service.tests;

import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.data.testing.entity.TestAppType;
import io.strategiz.data.testing.entity.TestCaseEntity;
import io.strategiz.data.testing.entity.TestFramework;
import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestSuiteEntity;
import io.strategiz.data.testing.repository.TestAppRepository;
import io.strategiz.data.testing.repository.TestCaseRepository;
import io.strategiz.data.testing.repository.TestModuleRepository;
import io.strategiz.data.testing.repository.TestSuiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Service for discovering tests in the codebase. Scans Playwright tests (frontend),
 * Maven/JUnit tests (backend), and Pytest tests (Python microservices). Organizes tests
 * into a hierarchical structure: App > Module > Suite > Test
 */
@Service
public class TestDiscoveryService {

	private static final Logger log = LoggerFactory.getLogger(TestDiscoveryService.class);

	// Patterns for parsing test files
	private static final Pattern PLAYWRIGHT_TEST_PATTERN = Pattern.compile("test\\(['\"](.+?)['\"]");

	private static final Pattern PLAYWRIGHT_DESCRIBE_PATTERN = Pattern.compile("describe\\(['\"](.+?)['\"]");

	private static final Pattern JUNIT_TEST_PATTERN = Pattern.compile("@Test\\s+.*?(?:public|private|protected)?\\s+void\\s+(\\w+)");

	private static final Pattern JUNIT_CLASS_PATTERN = Pattern.compile("class\\s+(\\w+Test)\\s");

	private static final Pattern PYTEST_TEST_PATTERN = Pattern.compile("def\\s+(test_\\w+)\\s*\\(");

	private static final Pattern JOURNEY_TAG_PATTERN = Pattern.compile("@journey:(\\w+)");

	private static final Pattern PAGE_TAG_PATTERN = Pattern.compile("@page:([\\w/]+)");

	private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile("@RequestMapping\\(['\"]([^'\"]+)['\"]\\)");

	private final TestAppRepository testAppRepository;

	private final TestModuleRepository testModuleRepository;

	private final TestSuiteRepository testSuiteRepository;

	private final TestCaseRepository testCaseRepository;

	@Autowired
	public TestDiscoveryService(TestAppRepository testAppRepository, TestModuleRepository testModuleRepository,
			TestSuiteRepository testSuiteRepository, TestCaseRepository testCaseRepository) {
		this.testAppRepository = testAppRepository;
		this.testModuleRepository = testModuleRepository;
		this.testSuiteRepository = testSuiteRepository;
		this.testCaseRepository = testCaseRepository;
	}

	/**
	 * Discover all tests across all frameworks
	 */
	public List<TestAppEntity> discoverAllTests() {
		List<TestAppEntity> allApps = new ArrayList<>();

		// Discover Playwright tests (frontend)
		try {
			allApps.addAll(discoverPlaywrightTests("../strategiz-ui"));
		}
		catch (Exception e) {
			log.error("Failed to discover Playwright tests", e);
		}

		// Discover Maven tests (backend)
		try {
			allApps.addAll(discoverMavenTests("."));
		}
		catch (Exception e) {
			log.error("Failed to discover Maven tests", e);
		}

		return allApps;
	}

	/**
	 * Discover Playwright E2E tests in the UI project
	 */
	public List<TestAppEntity> discoverPlaywrightTests(String uiDirectory) {
		List<TestAppEntity> apps = new ArrayList<>();
		Path uiPath = Paths.get(uiDirectory).toAbsolutePath().normalize();

		if (!Files.exists(uiPath)) {
			log.warn("UI directory not found: {}", uiPath);
			return apps;
		}

		log.info("Discovering Playwright tests in: {}", uiPath);

		// Create frontend app entity
		TestAppEntity frontendApp = new TestAppEntity("Frontend E2E Tests", TestAppType.FRONTEND);
		frontendApp.setId("frontend-e2e");
		frontendApp.setDescription("Playwright end-to-end tests for the Strategiz web applications");
		frontendApp.setCodeDirectory(uiDirectory);

		int totalModules = 0;
		int totalTests = 0;

		// Discover by journey (auth, labs, portfolio, settings)
		Map<String, List<TestCaseEntity>> journeyTests = new HashMap<>();
		journeyTests.put("auth", new ArrayList<>());
		journeyTests.put("labs", new ArrayList<>());
		journeyTests.put("portfolio", new ArrayList<>());
		journeyTests.put("settings", new ArrayList<>());
		journeyTests.put("dashboard", new ArrayList<>());

		// Scan e2e directories
		try {
			Path e2ePath = uiPath.resolve("apps/web/e2e");
			if (Files.exists(e2ePath)) {
				scanPlaywrightDirectory(e2ePath, frontendApp.getId(), journeyTests);
			}

			// Also scan console e2e
			Path consoleE2ePath = uiPath.resolve("apps/console/e2e");
			if (Files.exists(consoleE2ePath)) {
				scanPlaywrightDirectory(consoleE2ePath, frontendApp.getId(), journeyTests);
			}
		}
		catch (Exception e) {
			log.error("Error scanning Playwright directories", e);
		}

		// Create modules for each journey
		for (Map.Entry<String, List<TestCaseEntity>> entry : journeyTests.entrySet()) {
			String journey = entry.getKey();
			List<TestCaseEntity> tests = entry.getValue();

			if (!tests.isEmpty()) {
				TestModuleEntity module = new TestModuleEntity(frontendApp.getId(), journey, TestFramework.PLAYWRIGHT);
				module.setId("journey-" + journey);
				module.setDescription("E2E tests for " + journey + " journey");
				module.setTotalTests(tests.size());
				module.setTotalSuites(1);

				totalModules++;
				totalTests += tests.size();

				// Save module and tests
				try {
					testModuleRepository.save(module, "system");
					for (TestCaseEntity test : tests) {
						testCaseRepository.save(test, "system");
					}
				}
				catch (Exception e) {
					log.error("Error saving module: {}", journey, e);
				}
			}
		}

		frontendApp.setTotalModules(totalModules);
		frontendApp.setTotalTests(totalTests);

		// Save app
		try {
			testAppRepository.save(frontendApp, "system");
			apps.add(frontendApp);
		}
		catch (Exception e) {
			log.error("Error saving frontend app", e);
		}

		log.info("Discovered {} Playwright tests in {} modules", totalTests, totalModules);
		return apps;
	}

	/**
	 * Discover Maven/JUnit tests in the backend project
	 */
	public List<TestAppEntity> discoverMavenTests(String backendDirectory) {
		List<TestAppEntity> apps = new ArrayList<>();
		Path backendPath = Paths.get(backendDirectory).toAbsolutePath().normalize();

		if (!Files.exists(backendPath)) {
			log.warn("Backend directory not found: {}", backendPath);
			return apps;
		}

		log.info("Discovering Maven tests in: {}", backendPath);

		// Create backend app entity
		TestAppEntity backendApp = new TestAppEntity("Backend API Tests", TestAppType.BACKEND);
		backendApp.setId("backend-api");
		backendApp.setDescription("JUnit integration and unit tests for the Strategiz backend services");
		backendApp.setCodeDirectory(backendDirectory);

		int totalModules = 0;
		int totalTests = 0;

		// Scan service modules
		Path servicePath = backendPath.resolve("service");
		if (Files.exists(servicePath)) {
			try (Stream<Path> modules = Files.list(servicePath)) {
				List<Path> moduleList = modules.filter(Files::isDirectory)
					.filter(p -> p.getFileName().toString().startsWith("service-"))
					.toList();

				for (Path modulePath : moduleList) {
					String moduleName = modulePath.getFileName().toString();
					Path testPath = modulePath.resolve("src/test/java");

					if (Files.exists(testPath)) {
						TestModuleEntity module = scanMavenModule(backendApp.getId(), moduleName, testPath);
						if (module != null && module.getTotalTests() > 0) {
							totalModules++;
							totalTests += module.getTotalTests();

							// Save module
							try {
								testModuleRepository.save(module, "system");
							}
							catch (Exception e) {
								log.error("Error saving module: {}", moduleName, e);
							}
						}
					}
				}
			}
			catch (IOException e) {
				log.error("Error scanning service modules", e);
			}
		}

		backendApp.setTotalModules(totalModules);
		backendApp.setTotalTests(totalTests);

		// Save app
		try {
			testAppRepository.save(backendApp, "system");
			apps.add(backendApp);
		}
		catch (Exception e) {
			log.error("Error saving backend app", e);
		}

		log.info("Discovered {} Maven tests in {} modules", totalTests, totalModules);
		return apps;
	}

	/**
	 * Discover Pytest tests in the Python execution service
	 */
	public List<TestAppEntity> discoverPytestTests(String pythonDirectory) {
		List<TestAppEntity> apps = new ArrayList<>();
		Path pythonPath = Paths.get(pythonDirectory).toAbsolutePath().normalize();

		if (!Files.exists(pythonPath)) {
			log.warn("Python directory not found: {}", pythonPath);
			return apps;
		}

		log.info("Discovering Pytest tests in: {}", pythonPath);

		// Create Python app entity
		TestAppEntity pythonApp = new TestAppEntity("Strategy Execution Tests", TestAppType.MICROSERVICE);
		pythonApp.setId("python-execution");
		pythonApp.setDescription("Pytest tests for the Python strategy execution service");
		pythonApp.setCodeDirectory(pythonDirectory);

		int totalTests = 0;

		// Scan tests directory
		Path testsPath = pythonPath.resolve("tests");
		if (Files.exists(testsPath)) {
			try (Stream<Path> testFiles = Files.walk(testsPath)) {
				List<Path> pyTestFiles = testFiles.filter(p -> p.toString().endsWith(".py"))
					.filter(p -> p.getFileName().toString().startsWith("test_"))
					.toList();

				TestModuleEntity module = new TestModuleEntity(pythonApp.getId(), "pytest", TestFramework.PYTEST);
				module.setId("pytest-tests");
				module.setDescription("Pytest tests for strategy execution");

				for (Path testFile : pyTestFiles) {
					try {
						String content = Files.readString(testFile);
						Matcher matcher = PYTEST_TEST_PATTERN.matcher(content);

						while (matcher.find()) {
							String testName = matcher.group(1);
							TestCaseEntity testCase = new TestCaseEntity();
							testCase.setId(UUID.randomUUID().toString());
							testCase.setAppId(pythonApp.getId());
							testCase.setModuleId(module.getId());
							testCase.setDisplayName(testName);
							testCase.setFilePath(testFile.toString());

							testCaseRepository.save(testCase, "system");
							totalTests++;
						}
					}
					catch (IOException e) {
						log.error("Error reading test file: {}", testFile, e);
					}
				}

				module.setTotalTests(totalTests);
				module.setTotalSuites(pyTestFiles.size());
				testModuleRepository.save(module, "system");
			}
			catch (IOException e) {
				log.error("Error scanning pytest directory", e);
			}
		}

		pythonApp.setTotalModules(1);
		pythonApp.setTotalTests(totalTests);

		try {
			testAppRepository.save(pythonApp, "system");
			apps.add(pythonApp);
		}
		catch (Exception e) {
			log.error("Error saving Python app", e);
		}

		log.info("Discovered {} Pytest tests", totalTests);
		return apps;
	}

	/**
	 * Get all discovered test apps
	 */
	public List<TestAppEntity> getAllApps() {
		return testAppRepository.findAll();
	}

	/**
	 * Get modules for an app
	 */
	public List<TestModuleEntity> getModulesForApp(String appId) {
		return testModuleRepository.findByAppId(appId);
	}

	/**
	 * Get suites for a module
	 */
	public List<TestSuiteEntity> getSuitesForModule(String moduleId) {
		return testSuiteRepository.findByModuleId(moduleId);
	}

	/**
	 * Get test cases for a suite
	 */
	public List<TestCaseEntity> getTestsForSuite(String suiteId) {
		return testCaseRepository.findBySuiteId(suiteId);
	}

	/**
	 * Get frontend journeys (auth, labs, portfolio, settings)
	 */
	public List<Map<String, Object>> getFrontendJourneys() {
		List<Map<String, Object>> journeys = new ArrayList<>();

		List<TestModuleEntity> modules = testModuleRepository.findByAppId("frontend-e2e");
		for (TestModuleEntity module : modules) {
			if (module.getId().startsWith("journey-")) {
				Map<String, Object> journey = new HashMap<>();
				journey.put("id", module.getId().replace("journey-", ""));
				journey.put("name", module.getDisplayName());
				journey.put("description", module.getDescription());
				journey.put("testCount", module.getTotalTests());
				journeys.add(journey);
			}
		}

		return journeys;
	}

	/**
	 * Get frontend pages
	 */
	public List<Map<String, Object>> getFrontendPages() {
		List<Map<String, Object>> pages = new ArrayList<>();

		// Get unique pages from test cases
		List<TestCaseEntity> testCases = testCaseRepository.findByAppId("frontend-e2e");
		Map<String, Integer> pageTestCounts = new HashMap<>();

		for (TestCaseEntity testCase : testCases) {
			String[] tags = testCase.getTags();
			if (tags != null) {
				for (String tag : tags) {
					if (tag.startsWith("@page:")) {
						String page = tag.replace("@page:", "");
						pageTestCounts.merge(page, 1, Integer::sum);
					}
				}
			}
		}

		for (Map.Entry<String, Integer> entry : pageTestCounts.entrySet()) {
			Map<String, Object> page = new HashMap<>();
			page.put("id", entry.getKey());
			page.put("path", entry.getKey());
			page.put("testCount", entry.getValue());
			pages.add(page);
		}

		return pages;
	}

	/**
	 * Get backend modules (service-auth, service-labs, etc.)
	 */
	public List<Map<String, Object>> getBackendModules() {
		List<Map<String, Object>> modules = new ArrayList<>();

		List<TestModuleEntity> moduleEntities = testModuleRepository.findByAppId("backend-api");
		for (TestModuleEntity module : moduleEntities) {
			Map<String, Object> moduleMap = new HashMap<>();
			moduleMap.put("id", module.getId());
			moduleMap.put("name", module.getDisplayName());
			moduleMap.put("description", module.getDescription());
			moduleMap.put("testCount", module.getTotalTests());
			moduleMap.put("framework", module.getFramework());
			modules.add(moduleMap);
		}

		return modules;
	}

	/**
	 * Get backend APIs by scanning controller RequestMapping annotations
	 */
	public List<Map<String, Object>> getBackendApis() {
		List<Map<String, Object>> apis = new ArrayList<>();

		// Predefined API groupings based on RequestMapping
		Map<String, String> apiGroups = new HashMap<>();
		apiGroups.put("/v1/auth", "Authentication APIs");
		apiGroups.put("/v1/labs", "Labs APIs");
		apiGroups.put("/v1/users", "User APIs");
		apiGroups.put("/v1/console", "Admin Console APIs");
		apiGroups.put("/v1/strategies", "Strategy APIs");
		apiGroups.put("/v1/portfolio", "Portfolio APIs");
		apiGroups.put("/v1/marketdata", "Market Data APIs");

		for (Map.Entry<String, String> entry : apiGroups.entrySet()) {
			Map<String, Object> api = new HashMap<>();
			api.put("id", entry.getKey().replace("/", "-").substring(1));
			api.put("path", entry.getKey() + "/*");
			api.put("name", entry.getValue());
			api.put("testCount", 0); // Will be populated by scanning test files
			apis.add(api);
		}

		return apis;
	}

	// ===============================
	// Private helper methods
	// ===============================

	private void scanPlaywrightDirectory(Path e2ePath, String appId, Map<String, List<TestCaseEntity>> journeyTests) {
		try (Stream<Path> files = Files.walk(e2ePath)) {
			List<Path> specFiles = files.filter(p -> p.toString().endsWith(".spec.ts") || p.toString().endsWith(".spec.js"))
				.toList();

			for (Path specFile : specFiles) {
				try {
					String content = Files.readString(specFile);
					String fileName = specFile.getFileName().toString();

					// Determine journey from file path or tags
					String journey = determineJourney(specFile, content);

					// Find all test cases
					Matcher testMatcher = PLAYWRIGHT_TEST_PATTERN.matcher(content);
					while (testMatcher.find()) {
						String testName = testMatcher.group(1);

						TestCaseEntity testCase = new TestCaseEntity();
						testCase.setId(UUID.randomUUID().toString());
						testCase.setAppId(appId);
						testCase.setModuleId("journey-" + journey);
						testCase.setDisplayName(testName);
						testCase.setFilePath(specFile.toString());

						// Extract tags
						List<String> tagList = new ArrayList<>();
						tagList.add("@journey:" + journey);

						Matcher pageMatcher = PAGE_TAG_PATTERN.matcher(content);
						if (pageMatcher.find()) {
							tagList.add("@page:" + pageMatcher.group(1));
						}

						testCase.setTags(tagList.toArray(new String[0]));

						journeyTests.get(journey).add(testCase);
					}
				}
				catch (IOException e) {
					log.error("Error reading spec file: {}", specFile, e);
				}
			}
		}
		catch (IOException e) {
			log.error("Error walking e2e directory", e);
		}
	}

	private String determineJourney(Path specFile, String content) {
		String filePath = specFile.toString().toLowerCase();

		// Check for explicit journey tag
		Matcher journeyMatcher = JOURNEY_TAG_PATTERN.matcher(content);
		if (journeyMatcher.find()) {
			return journeyMatcher.group(1);
		}

		// Infer from file path
		if (filePath.contains("/auth/") || filePath.contains("signin") || filePath.contains("signup")
				|| filePath.contains("login")) {
			return "auth";
		}
		if (filePath.contains("/labs/") || filePath.contains("strategy") || filePath.contains("backtest")) {
			return "labs";
		}
		if (filePath.contains("/portfolio/") || filePath.contains("watchlist") || filePath.contains("holdings")) {
			return "portfolio";
		}
		if (filePath.contains("/settings/") || filePath.contains("profile") || filePath.contains("preferences")) {
			return "settings";
		}
		if (filePath.contains("/dashboard/")) {
			return "dashboard";
		}

		return "dashboard"; // Default
	}

	private TestModuleEntity scanMavenModule(String appId, String moduleName, Path testPath) {
		TestModuleEntity module = new TestModuleEntity(appId, moduleName, TestFramework.JUNIT);
		module.setId(moduleName);
		module.setModulePath("service/" + moduleName);

		int totalTests = 0;
		int totalSuites = 0;

		try (Stream<Path> files = Files.walk(testPath)) {
			List<Path> testFiles = files.filter(p -> p.toString().endsWith("Test.java")).toList();

			totalSuites = testFiles.size();

			for (Path testFile : testFiles) {
				try {
					String content = Files.readString(testFile);
					String className = testFile.getFileName().toString().replace(".java", "");

					// Create suite for each test class
					TestSuiteEntity suite = new TestSuiteEntity();
					suite.setId(moduleName + "-" + className);
					suite.setModuleId(module.getId());
					suite.setDisplayName(className);
					suite.setFilePath(testFile.toString());

					int suiteTestCount = 0;

					// Find all @Test methods
					Matcher testMatcher = JUNIT_TEST_PATTERN.matcher(content);
					while (testMatcher.find()) {
						String methodName = testMatcher.group(1);

						TestCaseEntity testCase = new TestCaseEntity();
						testCase.setId(UUID.randomUUID().toString());
						testCase.setAppId(appId);
						testCase.setModuleId(module.getId());
						testCase.setSuiteId(suite.getId());
						testCase.setDisplayName(methodName);
						testCase.setClassName(className);
						testCase.setMethodName(methodName);
						testCase.setFilePath(testFile.toString());

						testCaseRepository.save(testCase, "system");
						totalTests++;
						suiteTestCount++;
					}

					suite.setTotalTests(suiteTestCount);
					if (suiteTestCount > 0) {
						testSuiteRepository.save(suite, "system");
					}
				}
				catch (IOException e) {
					log.error("Error reading test file: {}", testFile, e);
				}
			}
		}
		catch (IOException e) {
			log.error("Error walking test directory: {}", testPath, e);
		}

		module.setTotalSuites(totalSuites);
		module.setTotalTests(totalTests);
		module.setDescription("JUnit tests for " + moduleName);

		return module;
	}

}
