package io.strategiz.data.device.config;

import io.strategiz.data.base.config.FirebaseConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for the data-device module. Scans for components in the package
 * structure.
 */
@Configuration
@Import(FirebaseConfig.class)
@ComponentScan(basePackages = "io.strategiz.data.device")
public class DataDeviceConfig {

}
