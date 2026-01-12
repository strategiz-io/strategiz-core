package io.strategiz.client.yahoofinance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate interceptor that automatically manages cookies across requests.
 * This enables Yahoo Finance authentication which requires cookies.
 */
public class CookieInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory.getLogger(CookieInterceptor.class);

	private final List<String> cookies = new ArrayList<>();

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		// Add stored cookies to request
		if (!cookies.isEmpty()) {
			String cookieHeader = String.join("; ", cookies);
			request.getHeaders().set("Cookie", cookieHeader);
			log.debug("Adding cookies to request: {}", cookieHeader);
		}

		// Execute request
		ClientHttpResponse response = execution.execute(request, body);

		// Extract and store cookies from response
		List<String> setCookieHeaders = response.getHeaders().get("Set-Cookie");
		if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
			log.debug("Received {} Set-Cookie headers", setCookieHeaders.size());
			// Store cookies for future requests
			synchronized (cookies) {
				cookies.clear();
				cookies.addAll(setCookieHeaders);
			}
		}

		return response;
	}

}
