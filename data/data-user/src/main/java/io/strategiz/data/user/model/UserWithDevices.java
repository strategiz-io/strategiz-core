package io.strategiz.data.user.model;

import io.strategiz.data.user.entity.UserEntity;

/**
 * User with devices aggregated data
 */
public class UserWithDevices {
    
    private UserEntity user;
    private Object devices; // TODO: Will be typed when data-devices module is updated
    
    // Constructors
    public UserWithDevices() {
    }
    
    public UserWithDevices(UserEntity user, Object devices) {
        this.user = user;
        this.devices = devices;
    }
    
    // Getters and Setters
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    public Object getDevices() {
        return devices;
    }
    
    public void setDevices(Object devices) {
        this.devices = devices;
    }
}