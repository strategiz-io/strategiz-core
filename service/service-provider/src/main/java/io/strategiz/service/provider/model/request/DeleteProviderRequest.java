package io.strategiz.service.provider.model.request;

import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;

/**
 * Request model for deleting/disconnecting provider connections. Used for safely removing
 * provider integrations with optional cleanup.
 */
public class DeleteProviderRequest {

	// Identity fields
	private String userId;

	private String providerId;

	@Pattern(regexp = "^(disconnect|revoke_tokens|full_cleanup)$",
			message = "Delete type must be one of: disconnect, revoke_tokens, full_cleanup")
	private String deleteType = "disconnect"; // Default to simple disconnect

	// Cleanup options
	private boolean revokeTokens = true; // Revoke OAuth tokens on provider side

	private boolean deleteLocalData = false; // Remove cached balances, transactions, etc.

	private boolean preserveHistory = true; // Keep historical connection data for audit

	// Safety options
	private boolean confirmDeletion = false; // User confirmation required

	private String confirmationCode; // Additional security for important accounts

	// Reason and metadata
	private String reason; // Reason for disconnection

	private boolean temporary = false; // Is this a temporary disconnection?

	// Force options (admin only)
	private boolean forceDelete = false; // Override safety checks

	private boolean emergencyDisconnect = false; // Emergency disconnect (skip provider
													// API calls)

	// Constructors
	public DeleteProviderRequest() {
	}

	public DeleteProviderRequest(String deleteType) {
		this.deleteType = deleteType;
	}

	public DeleteProviderRequest(String deleteType, String reason) {
		this.deleteType = deleteType;
		this.reason = reason;
	}

	// Getters and Setters
	public String getDeleteType() {
		return deleteType;
	}

	public void setDeleteType(String deleteType) {
		this.deleteType = deleteType;
	}

	public boolean isRevokeTokens() {
		return revokeTokens;
	}

	public void setRevokeTokens(boolean revokeTokens) {
		this.revokeTokens = revokeTokens;
	}

	public boolean isDeleteLocalData() {
		return deleteLocalData;
	}

	public void setDeleteLocalData(boolean deleteLocalData) {
		this.deleteLocalData = deleteLocalData;
	}

	public boolean isPreserveHistory() {
		return preserveHistory;
	}

	public void setPreserveHistory(boolean preserveHistory) {
		this.preserveHistory = preserveHistory;
	}

	public boolean isConfirmDeletion() {
		return confirmDeletion;
	}

	public void setConfirmDeletion(boolean confirmDeletion) {
		this.confirmDeletion = confirmDeletion;
	}

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public boolean isEmergencyDisconnect() {
		return emergencyDisconnect;
	}

	public void setEmergencyDisconnect(boolean emergencyDisconnect) {
		this.emergencyDisconnect = emergencyDisconnect;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	// Convenience method for cleanup options
	public Map<String, Object> getCleanupOptions() {
		Map<String, Object> options = new HashMap<>();
		options.put("revokeTokens", revokeTokens);
		options.put("deleteLocalData", deleteLocalData);
		options.put("preserveHistory", preserveHistory);
		return options;
	}

	// Helper methods
	public boolean isSimpleDisconnect() {
		return "disconnect".equals(deleteType);
	}

	public boolean isTokenRevocation() {
		return "revoke_tokens".equals(deleteType);
	}

	public boolean isFullCleanup() {
		return "full_cleanup".equals(deleteType);
	}

	public boolean requiresConfirmation() {
		return confirmDeletion || isFullCleanup();
	}

	public boolean isSafeDelete() {
		return !forceDelete && !emergencyDisconnect;
	}

	public boolean willPreserveData() {
		return !deleteLocalData || preserveHistory;
	}

	@Override
	public String toString() {
		return "DeleteProviderRequest{" + "deleteType='" + deleteType + '\'' + ", revokeTokens=" + revokeTokens
				+ ", deleteLocalData=" + deleteLocalData + ", preserveHistory=" + preserveHistory + ", confirmDeletion="
				+ confirmDeletion + ", temporary=" + temporary + ", forceDelete=" + forceDelete
				+ ", emergencyDisconnect=" + emergencyDisconnect + ", reason='" + reason + '\'' + '}';
	}

}