package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.ReadDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.authenticated.ReadAuthenticatedDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadAuthenticatedDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(ReadAuthenticatedDeviceService.class);
    
    private final ReadDeviceRepository readRepository;
    
    @Autowired
    public ReadAuthenticatedDeviceService(ReadDeviceRepository readRepository) {
        this.readRepository = readRepository;
    }
    
    public Optional<ReadAuthenticatedDeviceResponse> getAuthenticatedDevice(
            String userId, String deviceId) {
        
        log.debug("Getting authenticated device {} for user {}", deviceId, userId);
        
        Optional<DeviceIdentity> device = readRepository.getAuthenticatedDevice(userId, deviceId);
        
        return device.map(this::convertToResponse);
    }
    
    public List<ReadAuthenticatedDeviceResponse> getUserDevices(String userId) {
        log.debug("Getting all devices for user {}", userId);
        
        List<DeviceIdentity> devices = readRepository.getUserDevices(userId);
        
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<ReadAuthenticatedDeviceResponse> getTrustedDevices(String userId) {
        log.debug("Getting trusted devices for user {}", userId);
        
        List<DeviceIdentity> devices = readRepository.getTrustedDevices(userId);
        
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<ReadAuthenticatedDeviceResponse> getUntrustedDevices(String userId) {
        log.debug("Getting untrusted devices for user {}", userId);
        
        List<DeviceIdentity> devices = readRepository.getUntrustedDevices(userId);
        
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<ReadAuthenticatedDeviceResponse> getActiveDevices(String userId, int hours) {
        log.debug("Getting active devices for user {} from last {} hours", userId, hours);
        
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<DeviceIdentity> devices = readRepository.getActiveDevices(userId, cutoff);
        
        return devices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Optional<ReadAuthenticatedDeviceResponse> findByFingerprint(
            String userId, String fingerprint) {
        
        log.debug("Finding device by fingerprint {} for user {}", fingerprint, userId);
        
        Optional<DeviceIdentity> device = readRepository.findAuthenticatedDeviceByFingerprint(
            userId, fingerprint);
        
        return device.map(this::convertToResponse);
    }
    
    public boolean deviceExists(String userId, String deviceId) {
        log.debug("Checking if device {} exists for user {}", deviceId, userId);
        
        return readRepository.authenticatedDeviceExists(userId, deviceId);
    }
    
    public int countUserDevices(String userId) {
        log.debug("Counting devices for user {}", userId);
        
        return readRepository.countUserDevices(userId);
    }
    
    public int countTrustedDevices(String userId) {
        log.debug("Counting trusted devices for user {}", userId);
        
        return readRepository.countTrustedDevices(userId);
    }
    
    public Map<String, Object> getUserDeviceStatistics(String userId) {
        log.debug("Getting device statistics for user {}", userId);
        
        List<DeviceIdentity> devices = readRepository.getUserDevices(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", devices.size());
        stats.put("trustedDevices", countTrusted(devices));
        stats.put("untrustedDevices", countUntrusted(devices));
        stats.put("activeDevices", countActive(devices));
        stats.put("devicesByPlatform", groupByPlatform(devices));
        stats.put("devicesByBrowser", groupByBrowser(devices));
        stats.put("averageTrustScore", calculateAverageTrustScore(devices));
        
        return stats;
    }
    
    public List<ReadAuthenticatedDeviceResponse> searchUserDevices(
            String userId, Map<String, Object> filters) {
        
        log.debug("Searching devices for user {} with filters: {}", userId, filters);
        
        List<DeviceIdentity> devices = readRepository.getUserDevices(userId);
        
        return devices.stream()
            .filter(device -> matchesFilters(device, filters))
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    private ReadAuthenticatedDeviceResponse convertToResponse(DeviceIdentity device) {
        ReadAuthenticatedDeviceResponse response = new ReadAuthenticatedDeviceResponse();
        response.setDeviceId(device.getDeviceId());
        response.setUserId(device.getUserId());
        response.setDeviceName(device.getDeviceName());
        response.setFingerprint(device.getFingerprint());
        response.setVisitorId(device.getVisitorId());
        response.setPlatform(device.getPlatform());
        response.setBrowserName(device.getBrowserName());
        response.setOsName(device.getOsName());
        response.setOsVersion(device.getOsVersion());
        response.setTrusted(device.getTrusted());
        response.setTrustScore(device.getTrustScore());
        response.setTrustLevel(device.getTrustLevel());
        response.setLastSeen(device.getLastSeen());
        response.setCreatedAt(device.getCreatedAt());
        response.setUpdatedAt(device.getUpdatedAt());
        return response;
    }
    
    private long countTrusted(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(d -> Boolean.TRUE.equals(d.getTrusted()))
            .count();
    }
    
    private long countUntrusted(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(d -> !Boolean.TRUE.equals(d.getTrusted()))
            .count();
    }
    
    private long countActive(List<DeviceIdentity> devices) {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        return devices.stream()
            .filter(d -> d.getLastSeen() != null && d.getLastSeen().isAfter(cutoff))
            .count();
    }
    
    private Map<String, Long> groupByPlatform(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(d -> d.getOsName() != null)
            .collect(Collectors.groupingBy(
                DeviceIdentity::getOsName,
                Collectors.counting()
            ));
    }
    
    private Map<String, Long> groupByBrowser(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(d -> d.getBrowserName() != null)
            .collect(Collectors.groupingBy(
                DeviceIdentity::getBrowserName,
                Collectors.counting()
            ));
    }
    
    private double calculateAverageTrustScore(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(d -> d.getTrustScore() != null)
            .mapToDouble(DeviceIdentity::getTrustScore)
            .average()
            .orElse(0.0);
    }
    
    private boolean matchesFilters(DeviceIdentity device, Map<String, Object> filters) {
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String value = String.valueOf(filter.getValue());
            if (value == null || value.isEmpty() || "null".equals(value)) {
                continue;
            }
            
            switch (filter.getKey()) {
                case "deviceName":
                    if (!value.equals(device.getDeviceName())) return false;
                    break;
                case "platform":
                    if (!value.equals(device.getPlatform())) return false;
                    break;
                case "browserName":
                    if (!value.equals(device.getBrowserName())) return false;
                    break;
                case "osName":
                    if (!value.equals(device.getOsName())) return false;
                    break;
                case "trusted":
                    if (!Boolean.valueOf(value).equals(device.getTrusted())) return false;
                    break;
                case "trustLevel":
                    if (!value.equals(device.getTrustLevel())) return false;
                    break;
            }
        }
        return true;
    }
}