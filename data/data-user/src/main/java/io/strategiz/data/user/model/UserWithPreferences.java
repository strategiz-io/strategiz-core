package io.strategiz.data.user.model;

import io.strategiz.data.user.entity.UserEntity;

/**
 * User with preferences aggregated data
 */
public class UserWithPreferences {
    
    private UserEntity user;
    private Object preferences; // TODO: Will be typed when data-preferences module is updated
    
    // Constructors
    public UserWithPreferences() {
    }
    
    public UserWithPreferences(UserEntity user, Object preferences) {
        this.user = user;
        this.preferences = preferences;
    }
    
    // Getters and Setters
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    public Object getPreferences() {
        return preferences;
    }
    
    public void setPreferences(Object preferences) {
        this.preferences = preferences;
    }
}