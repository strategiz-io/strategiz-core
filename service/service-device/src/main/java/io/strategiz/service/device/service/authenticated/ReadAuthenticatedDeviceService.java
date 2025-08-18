package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.ReadDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * Service exclusively for reading/querying authenticated devices
 */
@Service
public class ReadAuthenticatedDeviceService {
    
    @Autowired(required = false)
    private ReadDeviceRepository readRepository;
    
    public List<DeviceIdentity> getAllUserDevices(String userId) {
        if (readRepository == null) return List.of();
        return readRepository.findAllByUserId(userId);
    }
    
    public Optional<DeviceIdentity> getUserDevice(String userId, String deviceId) {
        if (readRepository == null) return Optional.empty();
        return readRepository.findAuthenticatedDevice(userId, deviceId);
    }
    
    public List<DeviceIdentity> getTrustedUserDevices(String userId) {
        if (readRepository == null) return List.of();
        return readRepository.findTrustedByUserId(userId);
    }
    
    public Map<String, Object> getUserDeviceStatistics(String userId) {
        if (readRepository == null) return new HashMap<>();
        return readRepository.getUserDeviceStatistics(userId);
    }
    
    public List<DeviceIdentity> searchUserDevices(String userId, Map<String, Object> filters) {
        if (readRepository == null) return List.of();
        return readRepository.searchUserDevices(userId, filters);
    }
    
    public Optional<DeviceIdentity> findDeviceByVisitorId(String userId, String visitorId) {
        if (readRepository == null) return Optional.empty();
        return readRepository.findByUserIdAndVisitorId(userId, visitorId);
    }
}