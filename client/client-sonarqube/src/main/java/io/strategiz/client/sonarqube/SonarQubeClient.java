package io.strategiz.client.sonarqube;

import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.strategiz.client.sonarqube.config.SonarQubeConfig;
import io.strategiz.client.sonarqube.model.SonarQubeMetrics;

/**
 * HTTP client for SonarQube API. Fetches code quality metrics from self-hosted
 * SonarQube instance.
 *
 * SonarQube API documentation: https://next.sonarqube.com/sonarqube/web_api
 */
@Component
public class SonarQubeClient {

	private static final Logger log = LoggerFactory.getLogger(SonarQubeClient.class);

	private final SonarQubeConfig config;

	private final RestTemplate restTemplate;

	@Autowired
	public SonarQubeClient(SonarQubeConfig config, RestTemplate sonarQubeRestTemplate) {
		this.config = config;
		this.restTemplate = sonarQubeRestTemplate;
	}

	/**
	 * Get project metrics from SonarQube.
	 * @return aggregated metrics (bugs, vulnerabilities, code smells, etc.)
	 */
	public SonarQubeMetrics getProjectMetrics() {
		log.info("Fetching SonarQube metrics for project: {}", config.getProjectKey());

		try {
			// Call SonarQube API: /api/measures/component
			String url = config.getUrl() + "/api/measures/component?component=" + config.getProjectKey()
					+ "&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,sqale_index,reliability_rating";

			HttpHeaders headers = createAuthHeaders();
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

			// Parse response
			return parseMetrics(response.getBody());
		}
		catch (Exception e) {
			log.error("Failed to fetch SonarQube metrics: {}", e.getMessage());
			return createEmptyMetrics();
		}
	}

	/**
	 * Parse SonarQube API response into metrics object.
	 */
	@SuppressWarnings("unchecked")
	private SonarQubeMetrics parseMetrics(Map<String, Object> responseBody) {
		SonarQubeMetrics metrics = new SonarQubeMetrics();

		if (responseBody == null || !responseBody.containsKey("component")) {
			return createEmptyMetrics();
		}

		Map<String, Object> component = (Map<String, Object>) responseBody.get("component");
		if (!component.containsKey("measures")) {
			return createEmptyMetrics();
		}

		// Extract measures
		for (Map<String, Object> measure : (Iterable<Map<String, Object>>) component.get("measures")) {
			String metric = (String) measure.get("metric");
			String value = (String) measure.get("value");

			switch (metric) {
				case "bugs":
					metrics.setBugs(Integer.parseInt(value));
					break;
				case "vulnerabilities":
					metrics.setVulnerabilities(Integer.parseInt(value));
					break;
				case "code_smells":
					metrics.setCodeSmells(Integer.parseInt(value));
					break;
				case "coverage":
					metrics.setCoverage(Double.parseDouble(value));
					break;
				case "duplicated_lines_density":
					metrics.setDuplications(Double.parseDouble(value));
					break;
				case "sqale_index":
					metrics.setTechnicalDebt(formatTechnicalDebt(Integer.parseInt(value)));
					break;
				case "reliability_rating":
					metrics.setRating(convertRating(value));
					break;
			}
		}

		return metrics;
	}

	/**
	 * Create HTTP headers with Basic Auth using SonarQube token.
	 */
	private HttpHeaders createAuthHeaders() {
		HttpHeaders headers = new HttpHeaders();
		// SonarQube uses token as username with empty password
		String auth = config.getToken() + ":";
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		headers.set("Authorization", "Basic " + encodedAuth);
		return headers;
	}

	/**
	 * Format technical debt from minutes to human-readable format.
	 */
	private String formatTechnicalDebt(int minutes) {
		int hours = minutes / 60;
		int mins = minutes % 60;
		if (hours == 0) {
			return mins + "m";
		}
		return hours + "h " + mins + "m";
	}

	/**
	 * Convert SonarQube rating (1-5) to letter grade (A-E).
	 */
	private String convertRating(String rating) {
		switch (rating) {
			case "1.0":
			case "1":
				return "A";
			case "2.0":
			case "2":
				return "B";
			case "3.0":
			case "3":
				return "C";
			case "4.0":
			case "4":
				return "D";
			case "5.0":
			case "5":
				return "E";
			default:
				return "N/A";
		}
	}

	/**
	 * Create empty metrics (when SonarQube is unavailable).
	 */
	private SonarQubeMetrics createEmptyMetrics() {
		return new SonarQubeMetrics(0, 0, 0, 0.0, 0.0, "0m", "N/A");
	}

}
