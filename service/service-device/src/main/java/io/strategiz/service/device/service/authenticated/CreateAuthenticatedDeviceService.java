package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.CreateDeviceRepository;
import io.strategiz.service.device.model.DeviceRequest;
import io.strategiz.service.device.model.CreateDeviceResponse;
import io.strategiz.service.device.service.shared.DeviceEnrichmentService;
import io.strategiz.service.device.service.shared.DeviceFingerprintValidationService;
import io.strategiz.service.device.service.shared.DeviceTrustCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service exclusively for creating/registering authenticated devices
 * Handles comprehensive device fingerprinting and registration
 * Single Responsibility: Device creation logic for authenticated users
 */
@Service
public class CreateAuthenticatedDeviceService {
    
    private static final Logger log = LoggerFactory.getLogger(CreateAuthenticatedDeviceService.class);
    
    private final CreateDeviceRepository createRepository;
    private final DeviceEnrichmentService enrichmentService;
    private final DeviceFingerprintValidationService validationService;
    private final DeviceTrustCalculationService trustService;
    
    @Autowired
    public CreateAuthenticatedDeviceService(
            CreateDeviceRepository createRepository,
            DeviceEnrichmentService enrichmentService,
            DeviceFingerprintValidationService validationService,
            DeviceTrustCalculationService trustService) {
        this.createRepository = createRepository;
        this.enrichmentService = enrichmentService;
        this.validationService = validationService;
        this.trustService = trustService;
    }
    
    /**
     * Register a new device with comprehensive fingerprinting
     * 
     * @param userId The authenticated user ID
     * @param deviceRequest Comprehensive device fingerprint from client
     * @param ipAddress Client IP address
     * @param userAgent User agent string
     * @return Response with registration details
     */
    @Transactional
    public CreateDeviceResponse registerDevice(
            String userId,
            DeviceRequest deviceRequest,
            String ipAddress,
            String userAgent) {
        
        try {
            // Validate fingerprint data
            if (!validationService.validateFingerprint(deviceRequest)) {
                log.warn("Invalid fingerprint data for user {}", userId);
                return CreateDeviceResponse.error("Invalid device fingerprint data");
            }
            
            // Create device entity from request
            DeviceIdentity device = mapRequestToEntity(deviceRequest);
            
            // Set core fields
            device.setUserId(userId);
            device.setDeviceId(deviceRequest.getDeviceId() != null ? 
                deviceRequest.getDeviceId() : generateDeviceId());
            device.setFirstSeen(Instant.now());
            device.setLastSeen(device.getFirstSeen());
            
            // Set server-side data
            device.setIpAddress(ipAddress);
            device.setUserAgent(userAgent);
            
            // Enrich with server-side data (IP location, VPN detection, etc.)
            enrichmentService.enrichDeviceData(device, ipAddress);
            
            // Calculate trust score
            int trustScore = trustService.calculateTrustScore(device);
            device.setTrustLevel(trustService.getTrustLevel(trustScore));
            
            // For authenticated users, default to trusted unless suspicious
            device.setTrusted(trustScore >= 50);
            
            // Save to repository
            DeviceIdentity savedDevice = createRepository.createAuthenticatedDevice(device, userId);
            
            log.info("Registered device {} for user {} with trust score {}", 
                savedDevice.getDeviceId(), userId, trustScore);
            
            // Build response
            return CreateDeviceResponse.success(
                savedDevice.getDeviceId(),
                savedDevice.getFirstSeen(),
                trustScore
            );
            
        } catch (Exception e) {
            log.error("Error registering device for user {}: {}", userId, e.getMessage(), e);
            return CreateDeviceResponse.error("Failed to register device");
        }
    }
    
    /**
     * Map DeviceRequest to DeviceIdentity entity
     */
    private DeviceIdentity mapRequestToEntity(DeviceRequest request) {
        DeviceIdentity device = new DeviceIdentity();
        
        // Map fingerprint data
        if (request.getFingerprint() != null) {
            device.setVisitorId(request.getFingerprint().getVisitorId());
            device.setFingerprintConfidence(request.getFingerprint().getConfidence());
        }
        
        // Map browser info
        if (request.getBrowser() != null) {
            device.setBrowserName(request.getBrowser().getName());
            device.setBrowserVersion(request.getBrowser().getVersion());
            device.setBrowserVendor(request.getBrowser().getVendor());
            device.setCookiesEnabled(request.getBrowser().getCookiesEnabled());
            device.setDoNotTrack(request.getBrowser().getDoNotTrack());
            device.setLanguage(request.getBrowser().getLanguage());
            device.setLanguages(request.getBrowser().getLanguages());
        }
        
        // Map OS info
        if (request.getOs() != null) {
            device.setOsName(request.getOs().getName());
            device.setOsVersion(request.getOs().getVersion());
            device.setPlatform(request.getOs().getPlatform());
            device.setArchitecture(request.getOs().getArchitecture());
        }
        
        // Map hardware info
        if (request.getHardware() != null) {
            device.setScreenResolution(request.getHardware().getScreenResolution());
            device.setAvailableScreenResolution(request.getHardware().getAvailableScreenResolution());
            device.setColorDepth(request.getHardware().getColorDepth());
            device.setPixelRatio(request.getHardware().getPixelRatio());
            device.setHardwareConcurrency(request.getHardware().getHardwareConcurrency());
            device.setDeviceMemory(request.getHardware().getDeviceMemory());
            device.setMaxTouchPoints(request.getHardware().getMaxTouchPoints());
        }
        
        // Map rendering info
        if (request.getRendering() != null) {
            device.setCanvasFingerprint(request.getRendering().getCanvas());
            if (request.getRendering().getWebgl() != null) {
                device.setWebglVendor(request.getRendering().getWebgl().getVendor());
                device.setWebglRenderer(request.getRendering().getWebgl().getRenderer());
                device.setWebglUnmaskedVendor(request.getRendering().getWebgl().getUnmaskedVendor());
                device.setWebglUnmaskedRenderer(request.getRendering().getWebgl().getUnmaskedRenderer());
            }
            device.setFonts(request.getRendering().getFonts());
        }
        
        // Map network info
        if (request.getNetwork() != null) {
            device.setTimezone(request.getNetwork().getTimezone());
            device.setTimezoneOffset(request.getNetwork().getTimezoneOffset());
        }
        
        // Map media capabilities
        if (request.getMedia() != null) {
            device.setAudioFingerprint(request.getMedia().getAudioContext());
            device.setAudioCodecs(request.getMedia().getAudioCodecs());
            device.setVideoCodecs(request.getMedia().getVideoCodecs());
            if (request.getMedia().getMediaDevices() != null) {
                device.setSpeakerCount(request.getMedia().getMediaDevices().getSpeakers());
                device.setMicrophoneCount(request.getMedia().getMediaDevices().getMicrophones());
                device.setWebcamCount(request.getMedia().getMediaDevices().getWebcams());
            }
        }
        
        // Map browser features
        if (request.getFeatures() != null) {
            device.setHasLocalStorage(request.getFeatures().getLocalStorage());
            device.setHasSessionStorage(request.getFeatures().getSessionStorage());
            device.setHasIndexedDB(request.getFeatures().getIndexedDB());
            device.setHasWebRTC(request.getFeatures().getWebRTC());
            device.setHasWebAssembly(request.getFeatures().getWebAssembly());
            device.setHasServiceWorker(request.getFeatures().getServiceWorker());
            device.setHasPushNotifications(request.getFeatures().getPushNotifications());
        }
        
        // Map trust indicators
        if (request.getTrust() != null) {
            device.setIncognitoMode(request.getTrust().getIncognito());
            device.setAdBlockEnabled(request.getTrust().getAdBlock());
            device.setTamperingDetected(request.getTrust().getTampering());
            device.setHasLiedLanguages(request.getTrust().getHasLiedLanguages());
            device.setHasLiedResolution(request.getTrust().getHasLiedResolution());
            device.setHasLiedOs(request.getTrust().getHasLiedOs());
            device.setHasLiedBrowser(request.getTrust().getHasLiedBrowser());
        }
        
        // Map metadata
        if (request.getMetadata() != null) {
            device.setDeviceName(request.getMetadata().getDeviceName());
            device.setRegistrationType(request.getMetadata().getRegistrationType());
        }
        
        // Set public key
        device.setPublicKey(request.getPublicKey());
        
        return device;
    }
    
    /**
     * Generate a unique device ID
     */
    private String generateDeviceId() {
        return "dev_" + UUID.randomUUID().toString();
    }
}