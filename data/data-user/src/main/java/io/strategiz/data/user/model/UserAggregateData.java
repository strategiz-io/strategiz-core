package io.strategiz.data.user.model;

import io.strategiz.data.user.entity.UserEntity;

/**
 * Aggregated user data containing main user document + all subcollections
 * This is a composite DTO that delegates to other data modules for subcollection data
 */
public class UserAggregateData {
    
    private UserEntity user;
    private Object authMethods; // TODO: Will be typed when data-auth module is updated
    private Object watchlist; // TODO: Will be typed when data-watchlist module is updated  
    private Object providers; // TODO: Will be typed when data-providers module is updated
    private Object devices; // TODO: Will be typed when data-devices module is updated
    private Object preferences; // TODO: Will be typed when data-preferences module is updated
    
    // Constructors
    public UserAggregateData() {
    }
    
    public UserAggregateData(UserEntity user) {
        this.user = user;
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
    
    public Object getWatchlist() {
        return watchlist;
    }
    
    public void setWatchlist(Object watchlist) {
        this.watchlist = watchlist;
    }
    
    public Object getProviders() {
        return providers;
    }
    
    public void setProviders(Object providers) {
        this.providers = providers;
    }
    
    public Object getDevices() {
        return devices;
    }
    
    public void setDevices(Object devices) {
        this.devices = devices;
    }
    
    public Object getPreferences() {
        return preferences;
    }
    
    public void setPreferences(Object preferences) {
        this.preferences = preferences;
    }
}