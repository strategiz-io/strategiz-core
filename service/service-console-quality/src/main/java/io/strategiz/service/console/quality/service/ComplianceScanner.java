package io.strategiz.service.console.quality.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.strategiz.service.console.quality.model.ComplianceBreakdown;
import io.strategiz.service.console.quality.model.ComplianceMetric;
import io.strategiz.service.console.quality.model.ComplianceViolation;
import io.strategiz.service.console.quality.model.ComplianceViolation.ViolationSeverity;
import io.strategiz.service.console.quality.model.ComplianceViolation.ViolationType;

/**
 * Scans codebase for framework compliance violations. Analyzes Java source files to
 * detect: - Raw exception usage (IllegalArgumentException, RuntimeException, etc.) -
 * Services not extending BaseService - Controllers not extending BaseController
 */
@Service
public class ComplianceScanner {

	private static final Logger log = LoggerFactory.getLogger(ComplianceScanner.class);

	// Patterns for detecting violations
	private static final Pattern RAW_EXCEPTION_PATTERN = Pattern.compile(
			"throw\\s+new\\s+(IllegalArgumentException|RuntimeException|IllegalStateException|NullPointerException)\\s*\\(");

	private static final Pattern SERVICE_CLASS_PATTERN = Pattern
		.compile("public\\s+class\\s+(\\w+Service)\\s+(?!extends\\s+BaseService)");

	private static final Pattern CONTROLLER_CLASS_PATTERN = Pattern
		.compile("public\\s+class\\s+(\\w+Controller)\\s+(?!extends\\s+BaseController)");

	// Project root path (set via environment or default to current directory)
	private final Path projectRoot;

	public ComplianceScanner() {
		String rootPath = System.getProperty("project.root", System.getProperty("user.dir"));
		this.projectRoot = Paths.get(rootPath);
		log.info("ComplianceScanner initialized with project root: {}", projectRoot);
	}

	/**
	 * Scan codebase for all compliance violations.
	 * @return breakdown of compliance metrics
	 */
	public ComplianceBreakdown scanCompliance() {
		log.info("Starting compliance scan...");

		// Scan for exception handling violations
		List<ComplianceViolation> exceptionViolations = scanExceptionHandling();
		int totalThrows = countTotalThrowStatements();
		int compliantThrows = totalThrows - exceptionViolations.size();
		ComplianceMetric exceptionMetric = ComplianceMetric.from(compliantThrows, totalThrows);

		// Scan for service pattern violations
		List<ComplianceViolation> serviceViolations = scanServicePattern();
		int totalServices = countTotalServices();
		int compliantServices = totalServices - serviceViolations.size();
		ComplianceMetric serviceMetric = ComplianceMetric.from(compliantServices, totalServices);

		// Scan for controller pattern violations
		List<ComplianceViolation> controllerViolations = scanControllerPattern();
		int totalControllers = countTotalControllers();
		int compliantControllers = totalControllers - controllerViolations.size();
		ComplianceMetric controllerMetric = ComplianceMetric.from(compliantControllers, totalControllers);

		log.info("Compliance scan complete. Exception: {}/{}, Service: {}/{}, Controller: {}/{}", compliantThrows,
				totalThrows, compliantServices, totalServices, compliantControllers, totalControllers);

		return ComplianceBreakdown.builder()
			.exceptionHandling(exceptionMetric)
			.servicePattern(serviceMetric)
			.controllerPattern(controllerMetric)
			.build();
	}

	/**
	 * Scan for raw exception usage violations.
	 * @return list of exception handling violations
	 */
	public List<ComplianceViolation> scanExceptionHandling() {
		List<ComplianceViolation> violations = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(projectRoot)) {
			paths.filter(this::isJavaSourceFile).forEach(file -> {
				try {
					String content = Files.readString(file);
					Matcher matcher = RAW_EXCEPTION_PATTERN.matcher(content);

					int lineNumber = 1;
					int lastIndex = 0;
					while (matcher.find()) {
						// Count lines up to this match
						lineNumber += content.substring(lastIndex, matcher.start()).split("\n", -1).length - 1;
						lastIndex = matcher.start();

						String exceptionType = matcher.group(1);
						violations.add(ComplianceViolation.builder()
							.type(ViolationType.EXCEPTION_HANDLING)
							.file(projectRoot.relativize(file).toString())
							.line(lineNumber)
							.message("Raw " + exceptionType + " - use StrategizException framework")
							.severity(ViolationSeverity.MEDIUM)
							.build());
					}
				}
				catch (IOException e) {
					log.warn("Error reading file {}: {}", file, e.getMessage());
				}
			});
		}
		catch (IOException e) {
			log.error("Error scanning for exception violations: {}", e.getMessage());
		}

		return violations;
	}

	/**
	 * Scan for service pattern violations (services not extending BaseService).
	 * @return list of service pattern violations
	 */
	public List<ComplianceViolation> scanServicePattern() {
		List<ComplianceViolation> violations = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(projectRoot)) {
			paths.filter(this::isJavaSourceFile).filter(path -> path.toString().contains("service")).forEach(file -> {
				try {
					String content = Files.readString(file);
					Matcher matcher = SERVICE_CLASS_PATTERN.matcher(content);

					if (matcher.find()) {
						String className = matcher.group(1);
						// Skip BaseService itself and test classes
						if (!className.equals("BaseService") && !file.toString().contains("/test/")) {
							violations.add(ComplianceViolation.builder()
								.type(ViolationType.SERVICE_PATTERN)
								.file(projectRoot.relativize(file).toString())
								.line(findLineNumber(content, matcher.start()))
								.message("Service class '" + className + "' does not extend BaseService")
								.severity(ViolationSeverity.HIGH)
								.build());
						}
					}
				}
				catch (IOException e) {
					log.warn("Error reading file {}: {}", file, e.getMessage());
				}
			});
		}
		catch (IOException e) {
			log.error("Error scanning for service pattern violations: {}", e.getMessage());
		}

		return violations;
	}

	/**
	 * Scan for controller pattern violations (controllers not extending BaseController).
	 * @return list of controller pattern violations
	 */
	public List<ComplianceViolation> scanControllerPattern() {
		List<ComplianceViolation> violations = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(projectRoot)) {
			paths.filter(this::isJavaSourceFile)
				.filter(path -> path.toString().contains("controller"))
				.forEach(file -> {
					try {
						String content = Files.readString(file);
						Matcher matcher = CONTROLLER_CLASS_PATTERN.matcher(content);

						if (matcher.find()) {
							String className = matcher.group(1);
							// Skip BaseController itself and test classes
							if (!className.equals("BaseController") && !file.toString().contains("/test/")) {
								violations.add(ComplianceViolation.builder()
									.type(ViolationType.CONTROLLER_PATTERN)
									.file(projectRoot.relativize(file).toString())
									.line(findLineNumber(content, matcher.start()))
									.message("Controller class '" + className + "' does not extend BaseController")
									.severity(ViolationSeverity.HIGH)
									.build());
							}
						}
					}
					catch (IOException e) {
						log.warn("Error reading file {}: {}", file, e.getMessage());
					}
				});
		}
		catch (IOException e) {
			log.error("Error scanning for controller pattern violations: {}", e.getMessage());
		}

		return violations;
	}

	/**
	 * Count total throw statements in codebase.
	 * @return total number of throw statements
	 */
	private int countTotalThrowStatements() {
		Pattern throwPattern = Pattern.compile("throw\\s+");
		return countPattern(throwPattern);
	}

	/**
	 * Count total service classes.
	 * @return total number of service classes
	 */
	private int countTotalServices() {
		Pattern servicePattern = Pattern.compile("public\\s+class\\s+\\w+Service");
		return countPattern(servicePattern);
	}

	/**
	 * Count total controller classes.
	 * @return total number of controller classes
	 */
	private int countTotalControllers() {
		Pattern controllerPattern = Pattern.compile("public\\s+class\\s+\\w+Controller");
		return countPattern(controllerPattern);
	}

	/**
	 * Count occurrences of a pattern across all Java source files.
	 */
	private int countPattern(Pattern pattern) {
		int[] count = { 0 };

		try (Stream<Path> paths = Files.walk(projectRoot)) {
			paths.filter(this::isJavaSourceFile).forEach(file -> {
				try {
					String content = Files.readString(file);
					Matcher matcher = pattern.matcher(content);
					while (matcher.find()) {
						count[0]++;
					}
				}
				catch (IOException e) {
					log.warn("Error reading file {}: {}", file, e.getMessage());
				}
			});
		}
		catch (IOException e) {
			log.error("Error counting pattern: {}", e.getMessage());
		}

		return count[0];
	}

	/**
	 * Check if path is a Java source file (not in test or target directories).
	 */
	private boolean isJavaSourceFile(Path path) {
		String pathStr = path.toString();
		return Files.isRegularFile(path) && pathStr.endsWith(".java") && pathStr.contains("/src/main/")
				&& !pathStr.contains("/target/");
	}

	/**
	 * Find line number for character position in content.
	 */
	private int findLineNumber(String content, int position) {
		return content.substring(0, position).split("\n", -1).length;
	}

}
