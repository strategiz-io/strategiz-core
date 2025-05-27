package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Represents a user document in the Firestore "users" collection.
 */
@Data
@NoArgsConstructor
public class User {
    private String id;
    private String displayName;
    private String email;
    private Date createdAt;
    private String accountMode; // "PAPER" or "LIVE"
    
    // Additional user fields can be added as needed
}
