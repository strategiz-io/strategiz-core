package io.strategiz.data.user.model;

import io.strategiz.data.user.entity.UserEntity;

/**
 * User with connected providers aggregated data
 */
public class UserWithProviders {
    
    private UserEntity user;
    private Object providers; // TODO: Will be typed when data-provider module is updated
    
    // Constructors
    public UserWithProviders() {
    }
    
    public UserWithProviders(UserEntity user, Object providers) {
        this.user = user;
        this.providers = providers;
    }
    
    // Getters and Setters
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    public Object getProviders() {
        return providers;
    }
    
    public void setProviders(Object providers) {
        this.providers = providers;
    }
}