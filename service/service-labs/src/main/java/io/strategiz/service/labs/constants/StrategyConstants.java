package io.strategiz.service.labs.constants;

/**
 * Constants for the Strategy service module
 */
public class StrategyConstants {

	// Status constants
	public static final String STATUS_DRAFT = "draft";

	public static final String STATUS_ACTIVE = "active";

	public static final String STATUS_ARCHIVED = "archived";

	// Publish Status constants (marketplace readiness)
	public static final String PUBLISH_STATUS_DRAFT = "DRAFT";

	public static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";

	// Public Status constants (access control)
	public static final String PUBLIC_STATUS_PRIVATE = "PRIVATE";

	public static final String PUBLIC_STATUS_SUBSCRIBERS_ONLY = "SUBSCRIBERS_ONLY";

	public static final String PUBLIC_STATUS_PUBLIC = "PUBLIC";

	// Listed Status constants (sale listing state)
	public static final String LISTED_STATUS_NOT_LISTED = "NOT_LISTED";

	public static final String LISTED_STATUS_LISTED = "LISTED";

	// Language constants
	public static final String LANGUAGE_PYTHON = "python";

	public static final String LANGUAGE_JAVA = "java";

	public static final String LANGUAGE_PINESCRIPT = "pinescript";

	// Type constants
	public static final String TYPE_TECHNICAL = "technical";

	public static final String TYPE_FUNDAMENTAL = "fundamental";

	public static final String TYPE_HYBRID = "hybrid";

	// Default values
	public static final String DEFAULT_STATUS = STATUS_DRAFT;

	public static final String DEFAULT_TYPE = TYPE_TECHNICAL;

	// Validation constants
	public static final int MAX_NAME_LENGTH = 100;

	public static final int MAX_DESCRIPTION_LENGTH = 500;

	public static final int MAX_CODE_LENGTH = 50000;

	public static final int MAX_TAGS = 10;

	// Error message keys
	public static final String ERROR_STRATEGY_NOT_FOUND = "strategy-not-found";

	public static final String ERROR_STRATEGY_CREATION_FAILED = "strategy-creation-failed";

	public static final String ERROR_STRATEGY_UPDATE_FAILED = "strategy-update-failed";

	public static final String ERROR_STRATEGY_DELETION_FAILED = "strategy-deletion-failed";

	public static final String ERROR_STRATEGY_EXECUTION_FAILED = "strategy-execution-failed";

	public static final String ERROR_STRATEGY_INVALID_STATUS = "strategy-invalid-status";

	public static final String ERROR_STRATEGY_INVALID_LANGUAGE = "strategy-invalid-language";

	public static final String ERROR_STRATEGY_INVALID_TYPE = "strategy-invalid-type";

	private StrategyConstants() {
		// Private constructor to prevent instantiation
	}

}