package io.strategiz.framework.apidocs.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core configuration for OpenAPI/Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

	@Value("${strategiz.api.version:1.0.0}")
	private String apiVersion;

	@Value("${strategiz.api.title:Strategiz Core API}")
	private String apiTitle;

	@Value("${strategiz.api.description:Comprehensive API documentation for the Strategiz Core platform}")
	private String apiDescription;

	@Value("${strategiz.api.contact.name:Strategiz Team}")
	private String contactName;

	@Value("${strategiz.api.contact.url:https://strategiz.io}")
	private String contactUrl;

	@Value("${strategiz.api.contact.email:support@strategiz.io}")
	private String contactEmail;

	@Value("${strategiz.api.license.name:Private}")
	private String licenseName;

	@Value("${strategiz.api.license.url:https://strategiz.io/license}")
	private String licenseUrl;

	@Value("${strategiz.api.server.url:http://localhost:8080}")
	private String serverUrl;

	@Value("${strategiz.api.server.description:Local Development Server}")
	private String serverDescription;

	@Bean
	public OpenAPI strategizOpenAPI() {
		return new OpenAPI()
			.info(new Info().title(apiTitle)
				.description(apiDescription + " All endpoints use real data from actual cryptocurrency exchanges.")
				.version(apiVersion).contact(new Contact().name(contactName).url(contactUrl).email(contactEmail))
				.license(new License().name(licenseName).url(licenseUrl)))
			.servers(List.of(new Server().url(serverUrl).description(serverDescription)))
			.components(new Components().addSecuritySchemes("bearer-jwt",
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
						.description("JWT token authentication using the Bearer scheme.")));
	}

}
