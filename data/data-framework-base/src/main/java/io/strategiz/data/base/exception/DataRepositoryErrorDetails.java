package io.strategiz.data.base.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for data repository operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * All data layer exceptions should use these error codes for consistent error handling.
 */
public enum DataRepositoryErrorDetails implements ErrorDetails {

	// === CRUD Operations ===
	ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "entity-not-found"),
	ENTITY_NOT_FOUND_OR_UNAUTHORIZED(HttpStatus.NOT_FOUND, "entity-not-found-or-unauthorized"),
	ENTITY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "entity-save-failed"),
	ENTITY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "entity-update-failed"),
	ENTITY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "entity-delete-failed"),
	ENTITY_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "entity-restore-failed"),
	BULK_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bulk-operation-failed"),

	// === Query Operations ===
	QUERY_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "query-execution-failed"),
	QUERY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "query-timeout"),
	QUERY_INVALID_PARAMETERS(HttpStatus.BAD_REQUEST, "query-invalid-parameters"),

	// === Validation Errors ===
	ENTITY_NULL(HttpStatus.BAD_REQUEST, "entity-null"),
	USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "user-id-required"),
	ENTITY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "entity-id-required"),
	AUDIT_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "audit-fields-missing"),
	COLLECTION_ANNOTATION_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "collection-annotation-missing"),

	// === Firebase/Firestore Errors ===
	FIRESTORE_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "firestore-connection-failed"),
	FIRESTORE_INITIALIZATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "firestore-initialization-failed"),
	FIRESTORE_TRANSACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "firestore-transaction-failed"),
	FIRESTORE_OPERATION_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "firestore-operation-interrupted"),
	FIRESTORE_EXECUTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "firestore-execution-error"),

	// === Data Integrity Errors ===
	DUPLICATE_ENTITY(HttpStatus.CONFLICT, "duplicate-entity"),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "concurrent-modification"),
	DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "data-integrity-violation"),

	// === Collection/Document Errors ===
	COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "collection-not-found"),
	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "document-not-found"),
	SUBCOLLECTION_ACCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "subcollection-access-failed"),

	// === Conversion Errors ===
	ENTITY_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "entity-conversion-failed"),
	DOCUMENT_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "document-parse-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	DataRepositoryErrorDetails(HttpStatus httpStatus, String propertyKey) {
		this.httpStatus = httpStatus;
		this.propertyKey = propertyKey;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}

}
