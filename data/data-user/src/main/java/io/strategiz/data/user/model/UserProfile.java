package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the profile object within a user document.
 * Contains user identity and status information.
 */
@Data
@NoArgsConstructor
public class UserProfile {
    private String name;
    private String email;
    private String photoURL;
    private Boolean verifiedEmail = false;
    private String subscriptionTier = "free"; // premium, basic, free, etc.
    private String tradingMode = "demo"; // demo or real
    private Boolean isActive = true;
}
