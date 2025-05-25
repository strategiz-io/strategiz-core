package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.data.auth.DeviceIdentity;
import io.strategiz.service.auth.DeviceIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for device identity management endpoints
 */
@RestController
@RequestMapping("/auth/devices")
public class DeviceIdentityController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceIdentityController.class);

    @Autowired
    private DeviceIdentityService deviceIdentityService;

    /**
     * Register a new device identity
     *
     * @param userId User ID
     * @param deviceId Device ID
     * @param deviceName Device name
     * @return Response with device identity details
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceIdentity>> registerDevice(
            @RequestParam String userId,
            @RequestParam String deviceId,
            @RequestParam String deviceName) {
        try {
            DeviceIdentity deviceIdentity = deviceIdentityService.registerDevice(userId, deviceId, deviceName);
            return ResponseEntity.ok(
                ApiResponse.<DeviceIdentity>success("Device registered successfully", deviceIdentity)
            );
        } catch (Exception e) {
            logger.error("Error registering device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<DeviceIdentity>error("Error registering device: " + e.getMessage())
            );
        }
    }

    /**
     * Get all devices for a user
     *
     * @param userId User ID
     * @return Response with list of device identities
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceIdentity>>> getUserDevices(@RequestParam String userId) {
        try {
            List<DeviceIdentity> devices = deviceIdentityService.getUserDevices(userId);
            return ResponseEntity.ok(
                ApiResponse.<List<DeviceIdentity>>success("Devices retrieved successfully", devices)
            );
        } catch (Exception e) {
            logger.error("Error retrieving devices: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<List<DeviceIdentity>>error("Error retrieving devices: " + e.getMessage())
            );
        }
    }

    /**
     * Delete a device identity
     *
     * @param deviceId Device ID
     * @return Response with deletion status
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteDevice(@PathVariable String deviceId) {
        try {
            boolean deleted = deviceIdentityService.deleteDevice(deviceId);
            if (deleted) {
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("Device deleted successfully", true)
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Boolean>error("Device not found or could not be deleted", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error deleting device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting device: " + e.getMessage(), false)
            );
        }
    }
}
