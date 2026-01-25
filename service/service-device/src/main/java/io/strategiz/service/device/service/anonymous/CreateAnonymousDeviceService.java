package io.strategiz.service.device.service.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.CreateDeviceRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.device.exception.DeviceErrorDetails;
import io.strategiz.service.device.model.anonymous.CreateAnonymousDeviceRequest;
import io.strategiz.service.device.model.anonymous.CreateAnonymousDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateAnonymousDeviceService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(CreateAnonymousDeviceService.class);

    private final CreateDeviceRepository createRepository;

    @Override
    protected String getModuleName() {
        return "service-device";
    }

    @Autowired
    public CreateAnonymousDeviceService(CreateDeviceRepository createRepository) {
        this.createRepository = createRepository;
    }

    public CreateAnonymousDeviceResponse createAnonymousDevice(CreateAnonymousDeviceRequest request) {
        log.debug("Creating anonymous device with fingerprint: {}", request.getCanvasFingerprint());

        try {
            // Create device entity
            DeviceIdentity device = new DeviceIdentity();
            device.setDeviceId(UUID.randomUUID().toString());
            device.setCanvasFingerprint(request.getCanvasFingerprint());
            device.setVisitorId(request.getVisitorId());
            device.setPlatform(request.getPlatform());
            device.setBrowserName(request.getBrowserName());
            device.setUserAgent(request.getUserAgent());
            device.setIpAddress(request.getIpAddress());
            device.setScreenResolution(request.getScreenResolution());
            device.setTimezone(request.getTimezone());
            device.setLanguage(request.getLanguage());
            device.setOsName(request.getOsName());
            device.setOsVersion(request.getOsVersion());
            device.setPublicKey(request.getPublicKey());
            device.setCookiesEnabled(request.getCookiesEnabled());
            device.setColorDepth(request.getColorDepth());
            device.setHardwareConcurrency(request.getHardwareConcurrency());
            device.setDeviceMemory(request.getDeviceMemory());
            device.setTimezoneOffset(request.getTimezoneOffset());

            // Anonymous devices don't have userId
            device.setUserId(null);
            device.setTrusted(false);
            device.setFingerprintConfidence(calculateInitialTrustScore(request));
            device.setTrustLevel(determineTrustLevel(device.getFingerprintConfidence()));
            device.setFirstSeen(Instant.now());
            device.setLastSeen(Instant.now());

            // Save to repository
            DeviceIdentity savedDevice = createRepository.createAnonymousDevice(device);

            // Create success response
            CreateAnonymousDeviceResponse response = CreateAnonymousDeviceResponse.success(
                savedDevice.getDeviceId(),
                savedDevice.getCanvasFingerprint(),
                savedDevice.getFingerprintConfidence(),
                savedDevice.getTrustLevel(),
                savedDevice.getFirstSeen()
            );

            log.info("Successfully created anonymous device: {}", savedDevice.getDeviceId());
            return response;

        } catch (Exception e) {
            log.error("Error creating anonymous device: {}", e.getMessage(), e);
            return CreateAnonymousDeviceResponse.error("Failed to register device: " + e.getMessage());
        }
    }

    private Double calculateInitialTrustScore(CreateAnonymousDeviceRequest request) {
        double score = 0.5; // Base score

        // Adjust based on available information
        if (request.getCanvasFingerprint() != null && !request.getCanvasFingerprint().isEmpty()) {
            score += 0.1;
        }
        if (request.getVisitorId() != null && !request.getVisitorId().isEmpty()) {
            score += 0.1;
        }
        if (request.getUserAgent() != null && !request.getUserAgent().isEmpty()) {
            score += 0.05;
        }
        if (request.getScreenResolution() != null && !request.getScreenResolution().isEmpty()) {
            score += 0.05;
        }
        if (request.getPublicKey() != null && !request.getPublicKey().isEmpty()) {
            score += 0.1; // Web Crypto API public key adds trust
        }

        // Cap at 1.0
        return Math.min(score, 1.0);
    }

    private String determineTrustLevel(Double trustScore) {
        if (trustScore == null) return "UNKNOWN";
        if (trustScore >= 0.8) return "HIGH";
        if (trustScore >= 0.5) return "MEDIUM";
        if (trustScore >= 0.3) return "LOW";
        return "VERY_LOW";
    }
}
