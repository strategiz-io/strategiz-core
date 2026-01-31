package io.strategiz.service.base.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Session configuration for secure server-side session management Uses HttpOnly, Secure,
 * SameSite cookies for security
 */
@Configuration
public class SessionConfig {

	/**
	 * Session timeout and cookie configuration
	 */
	@Bean
	public org.springframework.boot.web.servlet.ServletContextInitializer sessionConfigurer() {
		return servletContext -> {
			// Set session timeout to 30 minutes (1800 seconds)
			servletContext.getSessionCookieConfig().setMaxAge(1800);
			servletContext.getSessionCookieConfig().setHttpOnly(true); // Prevent XSS
			servletContext.getSessionCookieConfig().setSecure(true); // HTTPS only
			servletContext.getSessionCookieConfig().setName("STRATEGIZ_SESSION");
			servletContext.getSessionCookieConfig().setPath("/");

			// Session timeout configuration
			servletContext.setSessionTimeout(30); // 30 minutes
		};
	}

}