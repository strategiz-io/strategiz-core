package io.strategiz.service.console.service.tests;

import io.strategiz.data.testing.entity.*;
import io.strategiz.data.testing.entity.TestAppType;
import io.strategiz.data.testing.repository.*;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for discovering tests in the codebase and populating Firestore
 * Scans Playwright tests, Maven modules, and Pytest tests
 */
@Service
public class TestDiscoveryService extends BaseService {

	private static final Logger log = LoggerFactory.getLogger(TestDiscoveryService.class);

	private final TestAppRepository testAppRepository;

	private final TestModuleRepository testModuleRepository;

	private final TestSuiteRepository testSuiteRepository;

	private final TestCaseRepository testCaseRepository;

	@Value("${test.discovery.ui.directory:../strategiz-ui}")
	private String uiDirectory;

	@Value("${test.discovery.backend.directory:.}")
	private String backendDirectory;

	@Value("${test.discovery.python.directory:./application-strategy-execution}")
	private String pythonDirectory;

	@Autowired
	public TestDiscoveryService(TestAppRepository testAppRepository, TestModuleRepository testModuleRepository,
			TestSuiteRepository testSuiteRepository, TestCaseRepository testCaseRepository) {
		this.testAppRepository = testAppRepository;
		this.testModuleRepository = testModuleRepository;
		this.testSuiteRepository = testSuiteRepository;
		this.testCaseRepository = testCaseRepository;
	}

	@Override
	protected String getModuleName() {
		return "TEST_DISCOVERY_SERVICE";
	}

	/**
	 * Discover all tests in the codebase and populate Firestore
	 * @return List of discovered test applications
	 */
	public List<TestAppEntity> discoverAllTests() {
		log.info("Starting test discovery for all applications");

		List<TestAppEntity> discoveredApps = new ArrayList<>();

		try {
			// Discover frontend tests (Playwright + Jest)
			List<TestAppEntity> frontendApps = discoverPlaywrightTests(uiDirectory);
			discoveredApps.addAll(frontendApps);

			// Discover backend tests (Maven)
			List<TestAppEntity> backendApps = discoverMavenTests(backendDirectory);
			discoveredApps.addAll(backendApps);

			// Discover Python tests (Pytest)
			List<TestAppEntity> pythonApps = discoverPytestTests(pythonDirectory);
			discoveredApps.addAll(pythonApps);

			log.info("Test discovery completed. Found {} applications", discoveredApps.size());
			return discoveredApps;
		}
		catch (Exception e) {
			log.error("Failed to discover tests", e);
			throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED, "Test discovery failed",
					e);
		}
	}

	/**
	 * Discover Playwright tests from strategiz-ui Turborepo Parse playwright.config.ts
	 * for test projects
	 * @param uiDir Path to strategiz-ui directory
	 * @return List of test applications discovered
	 */
	public List<TestAppEntity> discoverPlaywrightTests(String uiDir) {
		log.info("Discovering Playwright tests in directory: {}", uiDir);

		List<TestAppEntity> apps = new ArrayList<>();

		try {
			File uiDirectory = new File(uiDir);
			if (!uiDirectory.exists() || !uiDirectory.isDirectory()) {
				log.warn("UI directory not found: {}", uiDir);
				return apps;
			}

			// Discover apps in Turborepo structure (apps/web, apps/console, apps/auth)
			File appsDir = new File(uiDirectory, "apps");
			if (appsDir.exists() && appsDir.isDirectory()) {
				File[] appDirs = appsDir.listFiles(File::isDirectory);
				if (appDirs != null) {
					for (File appDir : appDirs) {
						TestAppEntity app = discoverPlaywrightApp(appDir);
						if (app != null) {
							apps.add(app);
						}
					}
				}
			}

			log.info("Discovered {} Playwright applications", apps.size());
			return apps;
		}
		catch (Exception e) {
			log.error("Failed to discover Playwright tests", e);
			throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
					"Playwright test discovery failed", e);
		}
	}

	/**
	 * Discover Playwright tests for a single app directory
	 * @param appDir App directory (e.g., apps/web)
	 * @return Test app entity or null if no tests found
	 */
	private TestAppEntity discoverPlaywrightApp(File appDir) {
		log.debug("Discovering Playwright tests in app: {}", appDir.getName());

		try {
			// Check if playwright.config.ts exists
			File playwrightConfig = new File(appDir, "playwright.config.ts");
			if (!playwrightConfig.exists()) {
				log.debug("No playwright.config.ts found in {}", appDir.getName());
				return null;
			}

			// Create app entity
			String appId = "frontend-" + appDir.getName();
			TestAppEntity app = new TestAppEntity();
			app.setId(appId);
			app.setDisplayName(appDir.getName().substring(0, 1).toUpperCase() + appDir.getName().substring(1)
					+ " App");
			app.setDescription("Frontend application with Playwright E2E tests");
			app.setType(TestAppType.FRONTEND);
			app.setCodeDirectory(appDir.getAbsolutePath());

			// Save app
			testAppRepository.save(app, "system");

			// Discover test modules (E2E tests, unit tests)
			List<TestModuleEntity> modules = discoverPlaywrightModules(appId, appDir);
			int totalTests = modules.stream().mapToInt(TestModuleEntity::getTotalTests).sum();

			// Update app with counts
			app.setTotalModules(modules.size());
			app.setTotalTests(totalTests);
			testAppRepository.save(app, "system");

			log.info("Discovered Playwright app: {} with {} modules and {} tests", app.getDisplayName(),
					modules.size(), totalTests);
			return app;
		}
		catch (Exception e) {
			log.error("Failed to discover Playwright app: {}", appDir.getName(), e);
			return null;
		}
	}

	/**
	 * Discover Playwright test modules for an app
	 * @param appId App ID
	 * @param appDir App directory
	 * @return List of test modules
	 */
	private List<TestModuleEntity> discoverPlaywrightModules(String appId, File appDir) {
		List<TestModuleEntity> modules = new ArrayList<>();

		try {
			// Discover E2E tests module
			File e2eDir = new File(appDir, "e2e");
			if (e2eDir.exists() && e2eDir.isDirectory()) {
				TestModuleEntity e2eModule = discoverPlaywrightE2EModule(appId, e2eDir);
				if (e2eModule != null) {
					modules.add(e2eModule);
				}
			}

			// Discover unit tests module
			File srcDir = new File(appDir, "src");
			if (srcDir.exists() && srcDir.isDirectory()) {
				TestModuleEntity unitModule = discoverJestModule(appId, srcDir);
				if (unitModule != null) {
					modules.add(unitModule);
				}
			}

			return modules;
		}
		catch (Exception e) {
			log.error("Failed to discover Playwright modules for app: {}", appId, e);
			return modules;
		}
	}

	/**
	 * Discover Playwright E2E tests module
	 * @param appId App ID
	 * @param e2eDir E2E directory
	 * @return Test module entity or null
	 */
	private TestModuleEntity discoverPlaywrightE2EModule(String appId, File e2eDir) {
		log.debug("Discovering Playwright E2E tests in: {}", e2eDir.getAbsolutePath());

		try {
			String moduleId = appId + "-e2e";
			TestModuleEntity module = new TestModuleEntity();
			module.setId(moduleId);
			module.setAppId(appId);
			module.setDisplayName("E2E Tests");
			module.setDescription("Playwright end-to-end tests");
			module.setFramework(TestFramework.PLAYWRIGHT);
			module.setModulePath(e2eDir.getAbsolutePath());

			// Save module
			testModuleRepository.save(module, "system");

			// Discover test suites (spec files)
			List<TestSuiteEntity> suites = discoverPlaywrightSuites(appId, moduleId, e2eDir);
			int totalTests = suites.stream().mapToInt(TestSuiteEntity::getTotalTests).sum();

			// Update module with counts
			module.setTotalSuites(suites.size());
			module.setTotalTests(totalTests);
			testModuleRepository.save(module, "system");

			log.debug("Discovered E2E module with {} suites and {} tests", suites.size(), totalTests);
			return module;
		}
		catch (Exception e) {
			log.error("Failed to discover Playwright E2E module", e);
			return null;
		}
	}

	/**
	 * Discover Playwright test suites (spec files)
	 * @param appId App ID
	 * @param moduleId Module ID
	 * @param e2eDir E2E directory
	 * @return List of test suites
	 */
	private List<TestSuiteEntity> discoverPlaywrightSuites(String appId, String moduleId, File e2eDir) {
		List<TestSuiteEntity> suites = new ArrayList<>();

		try {
			// Find all .spec.ts files
			List<File> specFiles = findFiles(e2eDir, ".*\\.spec\\.ts$");

			for (File specFile : specFiles) {
				TestSuiteEntity suite = parsePlaywrightSpecFile(appId, moduleId, specFile, e2eDir);
				if (suite != null) {
					suites.add(suite);
				}
			}

			log.debug("Discovered {} Playwright test suites", suites.size());
			return suites;
		}
		catch (Exception e) {
			log.error("Failed to discover Playwright suites", e);
			return suites;
		}
	}

	/**
	 * Parse Playwright spec file to extract test suite information
	 * @param appId App ID
	 * @param moduleId Module ID
	 * @param specFile Spec file
	 * @param baseDir Base directory for relative path
	 * @return Test suite entity or null
	 */
	private TestSuiteEntity parsePlaywrightSpecFile(String appId, String moduleId, File specFile, File baseDir) {
		try {
			String relativePath = baseDir.toPath().relativize(specFile.toPath()).toString();
			String suiteId = moduleId + "-" + relativePath.replaceAll("[/\\\\.]", "-");

			TestSuiteEntity suite = new TestSuiteEntity();
			suite.setId(suiteId);
			suite.setAppId(appId);
			suite.setModuleId(moduleId);
			suite.setDisplayName(specFile.getName().replace(".spec.ts", ""));
			suite.setDescription("Playwright test suite: " + relativePath);
			suite.setFilePath(specFile.getAbsolutePath());

			// Count tests in file (count "test(" occurrences)
			int testCount = countTestsInPlaywrightFile(specFile);
			suite.setTotalTests(testCount);

			// Save suite
			testSuiteRepository.save(suite, "system");

			// Parse individual tests
			List<TestCaseEntity> tests = parsePlaywrightTests(appId, moduleId, suiteId, specFile);
			suite.setTotalTests(tests.size());
			testSuiteRepository.save(suite, "system");

			log.debug("Parsed Playwright suite: {} with {} tests", suite.getDisplayName(), tests.size());
			return suite;
		}
		catch (Exception e) {
			log.error("Failed to parse Playwright spec file: {}", specFile.getName(), e);
			return null;
		}
	}

	/**
	 * Parse individual Playwright tests from spec file
	 * @param appId App ID
	 * @param moduleId Module ID
	 * @param suiteId Suite ID
	 * @param specFile Spec file
	 * @return List of test cases
	 */
	private List<TestCaseEntity> parsePlaywrightTests(String appId, String moduleId, String suiteId, File specFile) {
		List<TestCaseEntity> tests = new ArrayList<>();

		try {
			// Pattern to match: test('test name', async ({ page }) => {
			Pattern testPattern = Pattern.compile("test\\(['\"](.+?)['\"]");

			try (BufferedReader reader = new BufferedReader(new FileReader(specFile))) {
				String line;
				int lineNumber = 0;
				while ((line = reader.readLine()) != null) {
					lineNumber++;
					Matcher matcher = testPattern.matcher(line);
					if (matcher.find()) {
						String testName = matcher.group(1);
						String testId = suiteId + "-" + testName.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();

						TestCaseEntity test = new TestCaseEntity();
						test.setId(testId);
						test.setAppId(appId);
						test.setModuleId(moduleId);
						test.setSuiteId(suiteId);
						test.setDisplayName(testName);
						test.setMethodName(testName);
						test.setFilePath(specFile.getAbsolutePath());
						test.setLineNumber(lineNumber);

						// Save test
						testCaseRepository.save(test, "system");
						tests.add(test);
					}
				}
			}

			log.debug("Parsed {} Playwright tests from {}", tests.size(), specFile.getName());
			return tests;
		}
		catch (Exception e) {
			log.error("Failed to parse Playwright tests from: {}", specFile.getName(), e);
			return tests;
		}
	}

	/**
	 * Count tests in Playwright spec file
	 * @param specFile Spec file
	 * @return Test count
	 */
	private int countTestsInPlaywrightFile(File specFile) {
		try {
			String content = Files.readString(specFile.toPath());
			Pattern pattern = Pattern.compile("test\\(");
			Matcher matcher = pattern.matcher(content);
			int count = 0;
			while (matcher.find()) {
				count++;
			}
			return count;
		}
		catch (Exception e) {
			log.error("Failed to count tests in file: {}", specFile.getName(), e);
			return 0;
		}
	}

	/**
	 * Discover Jest unit tests module
	 * @param appId App ID
	 * @param srcDir Source directory
	 * @return Test module entity or null
	 */
	private TestModuleEntity discoverJestModule(String appId, File srcDir) {
		log.debug("Discovering Jest tests in: {}", srcDir.getAbsolutePath());

		try {
			// Find all .test.ts and .test.tsx files
			List<File> testFiles = findFiles(srcDir, ".*\\.test\\.(ts|tsx)$");

			if (testFiles.isEmpty()) {
				log.debug("No Jest test files found in {}", srcDir.getAbsolutePath());
				return null;
			}

			String moduleId = appId + "-unit";
			TestModuleEntity module = new TestModuleEntity();
			module.setId(moduleId);
			module.setAppId(appId);
			module.setDisplayName("Unit Tests");
			module.setDescription("Jest unit tests");
			module.setFramework(TestFramework.JEST);
			module.setModulePath(srcDir.getAbsolutePath());
			module.setTotalTests(testFiles.size()); // Simplified - each file as 1 test

			// Save module
			testModuleRepository.save(module, "system");

			log.debug("Discovered Jest module with {} test files", testFiles.size());
			return module;
		}
		catch (Exception e) {
			log.error("Failed to discover Jest module", e);
			return null;
		}
	}

	/**
	 * Discover Maven test modules Scans pom.xml files and test source directories
	 * @param backendDir Path to backend directory (strategiz-core)
	 * @return List of test applications discovered
	 */
	public List<TestAppEntity> discoverMavenTests(String backendDir) {
		log.info("Discovering Maven tests in directory: {}", backendDir);

		List<TestAppEntity> apps = new ArrayList<>();

		try {
			File backendDirectory = new File(backendDir);
			if (!backendDirectory.exists() || !backendDirectory.isDirectory()) {
				log.warn("Backend directory not found: {}", backendDir);
				return apps;
			}

			// Create backend app entity
			String appId = "backend-api";
			TestAppEntity app = new TestAppEntity();
			app.setId(appId);
			app.setDisplayName("Backend API");
			app.setDescription("Spring Boot backend with JUnit and Cucumber tests");
			app.setType(TestAppType.BACKEND);
			app.setCodeDirectory(backendDirectory.getAbsolutePath());

			// Save app
			testAppRepository.save(app, "system");

			// Discover Maven modules (service modules, data modules, etc.)
			List<TestModuleEntity> modules = discoverMavenModules(appId, backendDirectory);
			int totalTests = modules.stream().mapToInt(TestModuleEntity::getTotalTests).sum();

			// Update app with counts
			app.setTotalModules(modules.size());
			app.setTotalTests(totalTests);
			testAppRepository.save(app, "system");

			apps.add(app);

			log.info("Discovered Maven app: {} with {} modules and {} tests", app.getDisplayName(), modules.size(),
					totalTests);
			return apps;
		}
		catch (Exception e) {
			log.error("Failed to discover Maven tests", e);
			throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
					"Maven test discovery failed", e);
		}
	}

	/**
	 * Discover Maven test modules
	 * @param appId App ID
	 * @param backendDir Backend directory
	 * @return List of test modules
	 */
	private List<TestModuleEntity> discoverMavenModules(String appId, File backendDir) {
		List<TestModuleEntity> modules = new ArrayList<>();

		try {
			// Find all Maven modules with tests (directories containing pom.xml and
			// src/test/java)
			List<File> moduleDirectories = findMavenModulesWithTests(backendDir);

			for (File moduleDir : moduleDirectories) {
				TestModuleEntity module = parseMavenModule(appId, moduleDir, backendDir);
				if (module != null) {
					modules.add(module);
				}
			}

			log.info("Discovered {} Maven modules with tests", modules.size());
			return modules;
		}
		catch (Exception e) {
			log.error("Failed to discover Maven modules", e);
			return modules;
		}
	}

	/**
	 * Find all Maven modules that contain tests
	 * @param rootDir Root directory
	 * @return List of module directories
	 */
	private List<File> findMavenModulesWithTests(File rootDir) {
		List<File> moduleDirs = new ArrayList<>();

		try {
			// Recursively find directories with pom.xml and src/test/java
			Files.walk(Paths.get(rootDir.getAbsolutePath())).filter(Files::isDirectory).forEach(path -> {
				File dir = path.toFile();
				File pomFile = new File(dir, "pom.xml");
				File testDir = new File(dir, "src/test/java");

				if (pomFile.exists() && testDir.exists() && testDir.isDirectory()) {
					// Check if test directory has Java files
					try {
						long javaFileCount = Files.walk(testDir.toPath()).filter(p -> p.toString().endsWith(".java"))
							.count();
						if (javaFileCount > 0) {
							moduleDirs.add(dir);
						}
					}
					catch (IOException e) {
						log.warn("Failed to check test directory: {}", testDir, e);
					}
				}
			});

			log.debug("Found {} Maven modules with tests", moduleDirs.size());
			return moduleDirs;
		}
		catch (Exception e) {
			log.error("Failed to find Maven modules", e);
			return moduleDirs;
		}
	}

	/**
	 * Parse Maven module to extract test information
	 * @param appId App ID
	 * @param moduleDir Module directory
	 * @param baseDir Base directory for relative path
	 * @return Test module entity or null
	 */
	private TestModuleEntity parseMavenModule(String appId, File moduleDir, File baseDir) {
		try {
			String relativePath = baseDir.toPath().relativize(moduleDir.toPath()).toString();
			String moduleId = appId + "-" + relativePath.replaceAll("[/\\\\]", "-");

			TestModuleEntity module = new TestModuleEntity();
			module.setId(moduleId);
			module.setAppId(appId);
			module.setDisplayName(moduleDir.getName());
			module.setDescription("Maven module: " + relativePath);
			module.setFramework(TestFramework.JUNIT);
			module.setModulePath(moduleDir.getAbsolutePath());

			// Count test files
			File testDir = new File(moduleDir, "src/test/java");
			int testCount = countJavaTestFiles(testDir);
			module.setTotalTests(testCount);

			// Save module
			testModuleRepository.save(module, "system");

			log.debug("Parsed Maven module: {} with {} tests", module.getDisplayName(), testCount);
			return module;
		}
		catch (Exception e) {
			log.error("Failed to parse Maven module: {}", moduleDir.getName(), e);
			return null;
		}
	}

	/**
	 * Count Java test files in directory
	 * @param testDir Test directory
	 * @return Test file count
	 */
	private int countJavaTestFiles(File testDir) {
		try {
			return (int) Files.walk(testDir.toPath())
				.filter(p -> p.toString().endsWith("Test.java") || p.toString().contains("Test.java"))
				.count();
		}
		catch (Exception e) {
			log.error("Failed to count test files in: {}", testDir, e);
			return 0;
		}
	}

	/**
	 * Discover Pytest tests Scans tests directory for test_*.py files
	 * @param pythonDir Path to Python directory
	 * @return List of test applications discovered
	 */
	public List<TestAppEntity> discoverPytestTests(String pythonDir) {
		log.info("Discovering Pytest tests in directory: {}", pythonDir);

		List<TestAppEntity> apps = new ArrayList<>();

		try {
			File pythonDirectory = new File(pythonDir);
			if (!pythonDirectory.exists() || !pythonDirectory.isDirectory()) {
				log.warn("Python directory not found: {}", pythonDir);
				return apps;
			}

			File testsDir = new File(pythonDirectory, "tests");
			if (!testsDir.exists() || !testsDir.isDirectory()) {
				log.warn("Python tests directory not found: {}/tests", pythonDir);
				return apps;
			}

			// Create Python app entity
			String appId = "python-execution-service";
			TestAppEntity app = new TestAppEntity();
			app.setId(appId);
			app.setDisplayName("Python Execution Service");
			app.setDescription("Python gRPC service with Pytest tests");
			app.setType(TestAppType.MICROSERVICE);
			app.setCodeDirectory(pythonDirectory.getAbsolutePath());

			// Save app
			testAppRepository.save(app, "system");

			// Discover Pytest modules
			List<TestModuleEntity> modules = discoverPytestModules(appId, testsDir);
			int totalTests = modules.stream().mapToInt(TestModuleEntity::getTotalTests).sum();

			// Update app with counts
			app.setTotalModules(modules.size());
			app.setTotalTests(totalTests);
			testAppRepository.save(app, "system");

			apps.add(app);

			log.info("Discovered Pytest app: {} with {} modules and {} tests", app.getDisplayName(), modules.size(),
					totalTests);
			return apps;
		}
		catch (Exception e) {
			log.error("Failed to discover Pytest tests", e);
			throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
					"Pytest test discovery failed", e);
		}
	}

	/**
	 * Discover Pytest test modules
	 * @param appId App ID
	 * @param testsDir Tests directory
	 * @return List of test modules
	 */
	private List<TestModuleEntity> discoverPytestModules(String appId, File testsDir) {
		List<TestModuleEntity> modules = new ArrayList<>();

		try {
			// Find all test_*.py files
			List<File> testFiles = findFiles(testsDir, "test_.*\\.py$");

			for (File testFile : testFiles) {
				TestModuleEntity module = parsePytestFile(appId, testFile);
				if (module != null) {
					modules.add(module);
				}
			}

			log.debug("Discovered {} Pytest modules", modules.size());
			return modules;
		}
		catch (Exception e) {
			log.error("Failed to discover Pytest modules", e);
			return modules;
		}
	}

	/**
	 * Parse Pytest file to extract test information
	 * @param appId App ID
	 * @param testFile Test file
	 * @return Test module entity or null
	 */
	private TestModuleEntity parsePytestFile(String appId, File testFile) {
		try {
			String moduleId = appId + "-" + testFile.getName().replace(".py", "");

			TestModuleEntity module = new TestModuleEntity();
			module.setId(moduleId);
			module.setAppId(appId);
			module.setDisplayName(testFile.getName().replace("test_", "").replace(".py", ""));
			module.setDescription("Pytest module: " + testFile.getName());
			module.setFramework(TestFramework.PYTEST);
			module.setModulePath(testFile.getAbsolutePath());

			// Count test functions (lines starting with "def test_")
			int testCount = countPytestFunctions(testFile);
			module.setTotalTests(testCount);

			// Save module
			testModuleRepository.save(module, "system");

			log.debug("Parsed Pytest module: {} with {} tests", module.getDisplayName(), testCount);
			return module;
		}
		catch (Exception e) {
			log.error("Failed to parse Pytest file: {}", testFile.getName(), e);
			return null;
		}
	}

	/**
	 * Count Pytest test functions in file
	 * @param testFile Test file
	 * @return Test function count
	 */
	private int countPytestFunctions(File testFile) {
		try {
			String content = Files.readString(testFile.toPath());
			Pattern pattern = Pattern.compile("^\\s*def test_", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(content);
			int count = 0;
			while (matcher.find()) {
				count++;
			}
			return count;
		}
		catch (Exception e) {
			log.error("Failed to count tests in file: {}", testFile.getName(), e);
			return 0;
		}
	}

	/**
	 * Find files matching regex pattern in directory (recursive)
	 * @param directory Directory to search
	 * @param regex Regex pattern
	 * @return List of matching files
	 */
	private List<File> findFiles(File directory, String regex) {
		try {
			Pattern pattern = Pattern.compile(regex);
			try (Stream<Path> paths = Files.walk(directory.toPath())) {
				return paths.filter(Files::isRegularFile)
					.filter(p -> pattern.matcher(p.getFileName().toString()).matches())
					.map(Path::toFile)
					.collect(Collectors.toList());
			}
		}
		catch (Exception e) {
			log.error("Failed to find files in directory: {}", directory, e);
			return new ArrayList<>();
		}
	}

}
