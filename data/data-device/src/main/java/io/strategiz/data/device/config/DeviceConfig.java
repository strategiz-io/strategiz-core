package io.strategiz.data.device.config;

import io.strategiz.data.base.document.DocumentStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for the data-device module.
 * Scans for components in the package structure.
 * Enables JPA repositories for the device package.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.data.device")
@EnableJpaRepositories(basePackages = "io.strategiz.data.device.repository")
@EntityScan(basePackages = "io.strategiz.data.device.model")
public class DeviceConfig {
    
    private final DocumentStorageRepository documentStorageRepository;
    
    /**
     * Constructor for DeviceConfig with required dependencies
     * @param documentStorageRepository The document storage repository for Firestore access
     */
    @Autowired
    public DeviceConfig(DocumentStorageRepository documentStorageRepository) {
        this.documentStorageRepository = documentStorageRepository;
    }
}
