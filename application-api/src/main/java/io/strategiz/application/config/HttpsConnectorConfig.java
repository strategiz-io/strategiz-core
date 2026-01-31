package io.strategiz.application.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * Configuration for HTTPS connector on port 8443 Required for Charles Schwab OAuth
 * callbacks which mandate HTTPS
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "https.enabled", havingValue = "true")
public class HttpsConnectorConfig {

	@Value("${server.additional.https.port:8443}")
	private int httpsPort;

	@Value("${server.additional.https.cert-path:cert.pem}")
	private String certPath;

	@Value("${server.additional.https.key-path:key.pem}")
	private String keyPath;

	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
			@Override
			protected void postProcessContext(Context context) {
				SecurityConstraint securityConstraint = new SecurityConstraint();
				securityConstraint.setUserConstraint("CONFIDENTIAL");
				SecurityCollection collection = new SecurityCollection();
				collection.addPattern("/*");
				securityConstraint.addCollection(collection);
				context.addConstraint(securityConstraint);
			}
		};

		// Add HTTPS connector for port 8443
		tomcat.addAdditionalTomcatConnectors(createHttpsConnector());
		return tomcat;
	}

	private Connector createHttpsConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setScheme("https");
		connector.setSecure(true);
		connector.setPort(httpsPort);

		try {
			// Set SSL properties
			connector.setProperty("SSLEnabled", "true");
			connector.setProperty("sslProtocol", "TLS");
			connector.setProperty("keystoreFile", new File(certPath).getAbsolutePath());
			connector.setProperty("keystorePass", "");
			connector.setProperty("keystoreType", "PKCS12");
			connector.setProperty("keyAlias", "1");

			// Use PEM files directly
			connector.setProperty("certificateFile", new File(certPath).getAbsolutePath());
			connector.setProperty("certificateKeyFile", new File(keyPath).getAbsolutePath());

			System.out.println("HTTPS Connector configured on port " + httpsPort);
			System.out.println("Certificate file: " + new File(certPath).getAbsolutePath());
			System.out.println("Key file: " + new File(keyPath).getAbsolutePath());

		}
		catch (Exception e) {
			System.err.println("Failed to configure HTTPS connector: " + e.getMessage());
			e.printStackTrace();
		}

		return connector;
	}

}