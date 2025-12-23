package io.strategiz.business.provider.plaid.model;

import java.util.List;
import java.util.Map;

/**
 * Result of a Plaid connection operation.
 */
public class PlaidConnectionResult {

    public enum Status {
        SUCCESS,
        PENDING_UPDATE,
        ITEM_LOGIN_REQUIRED,
        ERROR
    }

    private Status status;
    private String message;
    private String itemId;
    private String institutionId;
    private String institutionName;
    private List<PlaidAccount> accounts;
    private Map<String, Object> metadata;

    public PlaidConnectionResult() {
    }

    public PlaidConnectionResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static PlaidConnectionResult success(String itemId, String institutionName, List<PlaidAccount> accounts) {
        PlaidConnectionResult result = new PlaidConnectionResult();
        result.status = Status.SUCCESS;
        result.message = "Successfully connected to " + institutionName;
        result.itemId = itemId;
        result.institutionName = institutionName;
        result.accounts = accounts;
        return result;
    }

    public static PlaidConnectionResult error(String message) {
        return new PlaidConnectionResult(Status.ERROR, message);
    }

    public static PlaidConnectionResult loginRequired(String itemId, String institutionName) {
        PlaidConnectionResult result = new PlaidConnectionResult();
        result.status = Status.ITEM_LOGIN_REQUIRED;
        result.message = "Please re-authenticate with " + institutionName;
        result.itemId = itemId;
        result.institutionName = institutionName;
        return result;
    }

    // Getters and setters
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public List<PlaidAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<PlaidAccount> accounts) {
        this.accounts = accounts;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
