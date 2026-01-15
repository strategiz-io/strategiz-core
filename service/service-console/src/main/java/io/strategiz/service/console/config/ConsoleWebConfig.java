package io.strategiz.service.console.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for console module. Registers the AdminAuthInterceptor for
 * /v1/console/* paths when authentication is enabled.
 */
@Configuration
@ConditionalOnBean(AdminAuthInterceptor.class)
public class ConsoleWebConfig implements WebMvcConfigurer {

	private final AdminAuthInterceptor adminAuthInterceptor;

	@Autowired
	public ConsoleWebConfig(AdminAuthInterceptor adminAuthInterceptor) {
		this.adminAuthInterceptor = adminAuthInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/v1/console/**");
	}

}
