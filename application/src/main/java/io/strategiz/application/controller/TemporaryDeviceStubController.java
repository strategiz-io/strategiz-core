package io.strategiz.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary stub controller for device registration
 * TODO: Remove this once service-device module is fixed and re-enabled
 */
@RestController
@RequestMapping("/v1/users/devices")
public class TemporaryDeviceStubController {

    private static final Logger log = LoggerFactory.getLogger(TemporaryDeviceStubController.class);

    @PostMapping("/registrations")
    public ResponseEntity<Map<String, Object>> registerDevice(@RequestBody(required = false) Map<String, Object> deviceData) {
        log.warn("⚠️  Using TEMPORARY device registration stub - service-device module is disabled");
        log.info("Device registration request received (stubbed)");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Device registered (stub)");
        response.put("deviceId", "stub-device-" + System.currentTimeMillis());
        response.put("registered", true);

        return ResponseEntity.ok(response);
    }
}
