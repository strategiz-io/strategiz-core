package io.strategiz.service.base.config;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration for path handling and resource prioritization This ensures API endpoints
 * are prioritized over static resources
 */
@Configuration
public class ResourceHandlingConfig implements WebMvcConfigurer {

	/**
	 * Configure path matching to ensure API endpoints take precedence This helps prevent
	 * resource handlers from intercepting API requests
	 */
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		// Setting use suffix pattern match to false prevents issues with extensions
		configurer.setUseSuffixPatternMatch(false);
	}

	/**
	 * Custom WebMvcRegistrations to ensure highest priority for API endpoints This
	 * overrides the default RequestMappingHandlerMapping to set highest precedence for
	 * API controllers, ensuring they are checked before static resources
	 * @return WebMvcRegistrations with custom handler mapping
	 */
	@Bean
	public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
		return new WebMvcRegistrations() {
			@Override
			public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
				RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
				handlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
				return handlerMapping;
			}
		};
	}

}
