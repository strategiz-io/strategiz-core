package io.strategiz.data.user.model;

import io.strategiz.data.user.entity.UserEntity;

/**
 * User with authentication methods aggregated data
 */
public class UserWithAuthMethods {
    
    private UserEntity user;
    private Object authMethods; // TODO: Will be typed when data-auth module is updated
    
    // Constructors
    public UserWithAuthMethods() {
    }
    
    public UserWithAuthMethods(UserEntity user, Object authMethods) {
        this.user = user;
        this.authMethods = authMethods;
    }
    
    // Getters and Setters
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    public Object getAuthMethods() {
        return authMethods;
    }
    
    public void setAuthMethods(Object authMethods) {
        this.authMethods = authMethods;
    }
}