package io.strategiz.service.device.service.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.ReadDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadAnonymousDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(ReadAnonymousDeviceService.class);
    
    private final ReadDeviceRepository readRepository;
    
    @Autowired
    public ReadAnonymousDeviceService(ReadDeviceRepository readRepository) {
        this.readRepository = readRepository;
    }
    
    public Optional<DeviceIdentity> getAnonymousDevice(String deviceId) {
        log.debug("Getting anonymous device: {}", deviceId);
        return readRepository.findAnonymousDevice(deviceId);
    }
    
    public boolean deviceExists(String deviceId) {
        log.debug("Checking if anonymous device exists: {}", deviceId);
        return readRepository.deviceExists(deviceId);
    }
    
    public Optional<DeviceIdentity> findByVisitorId(String visitorId) {
        log.debug("Finding device by visitor ID: {}", visitorId);
        return readRepository.findAnonymousByVisitorId(visitorId);
    }
    
    public List<DeviceIdentity> getAllAnonymousDevices(int limit, int offset) {
        log.debug("Getting all anonymous devices with limit {} and offset {}", limit, offset);
        return readRepository.findAllAnonymousDevices(limit, offset);
    }
    
    public Map<String, Object> getAnonymousDeviceStatistics() {
        log.debug("Getting anonymous device statistics");
        
        List<DeviceIdentity> allDevices = readRepository.findAllAnonymousDevices(10000, 0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", allDevices.size());
        stats.put("activeDevices", countActiveDevices(allDevices));
        stats.put("suspiciousDevices", countSuspiciousDevices(allDevices));
        stats.put("blockedDevices", countBlockedDevices(allDevices));
        stats.put("devicesByPlatform", groupByPlatform(allDevices));
        stats.put("devicesByBrowser", groupByBrowser(allDevices));
        
        return stats;
    }
    
    public List<DeviceIdentity> searchAnonymousDevices(Map<String, Object> filters) {
        log.debug("Searching anonymous devices with filters: {}", filters);
        
        // Get all devices and filter in memory (simplified implementation)
        List<DeviceIdentity> allDevices = readRepository.findAllAnonymousDevices(10000, 0);
        
        return allDevices.stream()
            .filter(device -> matchesFilters(device, filters))
            .collect(Collectors.toList());
    }
    
    public List<DeviceIdentity> getSuspiciousDevices() {
        log.debug("Getting suspicious anonymous devices");
        
        List<DeviceIdentity> allDevices = readRepository.findAllAnonymousDevices(10000, 0);
        
        return allDevices.stream()
            .filter(this::isSuspicious)
            .collect(Collectors.toList());
    }
    
    public List<DeviceIdentity> getRecentDevices(int hours) {
        log.debug("Getting anonymous devices from last {} hours", hours);
        
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<DeviceIdentity> allDevices = readRepository.findAllAnonymousDevices(10000, 0);
        
        return allDevices.stream()
            .filter(device -> device.getLastSeen() != null && device.getLastSeen().isAfter(cutoff))
            .sorted((a, b) -> b.getLastSeen().compareTo(a.getLastSeen()))
            .collect(Collectors.toList());
    }
    
    private long countActiveDevices(List<DeviceIdentity> devices) {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        return devices.stream()
            .filter(d -> d.getLastSeen() != null && d.getLastSeen().isAfter(cutoff))
            .count();
    }
    
    private long countSuspiciousDevices(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(this::isSuspicious)
            .count();
    }
    
    private long countBlockedDevices(List<DeviceIdentity> devices) {
        return devices.stream()
            .filter(d -> "BLOCKED".equals(d.getTrustLevel()))
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
    
    private boolean matchesFilters(DeviceIdentity device, Map<String, Object> filters) {
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String value = String.valueOf(filter.getValue());
            if (value == null || value.isEmpty() || "null".equals(value)) {
                continue;
            }
            
            switch (filter.getKey()) {
                case "ipAddress":
                    if (!value.equals(device.getIpAddress())) return false;
                    break;
                case "browserName":
                    if (!value.equals(device.getBrowserName())) return false;
                    break;
                case "osName":
                    if (!value.equals(device.getOsName())) return false;
                    break;
                case "trustLevel":
                    if (!value.equals(device.getTrustLevel())) return false;
                    break;
            }
        }
        return true;
    }
    
    private boolean isSuspicious(DeviceIdentity device) {
        // Simple heuristic for suspicious devices
        if ("BLOCKED".equals(device.getTrustLevel())) {
            return true;
        }
        if (device.getFingerprintConfidence() != null && device.getFingerprintConfidence() < 0.3) {
            return true;
        }
        if ("LOW".equals(device.getTrustLevel())) {
            return true;
        }
        return false;
    }
}