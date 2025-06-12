package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Represents a financial provider connected to a user's account.
 */
@Data
@NoArgsConstructor
public class ConnectedProvider {
    private String id;
    private String providerId;
    private String providerName;
    private String accountId;
    private String accountType; // "paper" or "real"
    private String accountName;
    private String status; // "active", "inactive", "error"
    private Date lastSyncAt;
    
    // Audit fields
    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
    private Integer version = 1;
    private Boolean isActive = true;
}
