package io.strategiz.service.base.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Global Jackson configuration for JSON serialization/deserialization.
 *
 * Key configurations:
 * - Excludes null values from JSON responses (reduces payload size)
 * - Configures proper date/time serialization
 * - Ensures consistent JSON formatting across all API responses
 *
 * @author Strategiz Team
 * @version 1.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Configures the global ObjectMapper used by Spring Boot for all JSON serialization.
     *
     * Configuration:
     * - NON_EMPTY: Excludes null fields, empty strings, and empty collections from JSON output
     * - JavaTimeModule: Proper Java 8 date/time support
     * - WRITE_DATES_AS_TIMESTAMPS: Disabled for ISO-8601 format
     *
     * @param builder Jackson2ObjectMapperBuilder
     * @return Configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // Exclude null values, empty strings, and empty collections from JSON responses
        // This keeps API responses clean and reduces payload size
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // Register Java 8 date/time module for proper Instant, LocalDateTime, etc. serialization
        objectMapper.registerModule(new JavaTimeModule());

        // Use ISO-8601 format for dates instead of timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
