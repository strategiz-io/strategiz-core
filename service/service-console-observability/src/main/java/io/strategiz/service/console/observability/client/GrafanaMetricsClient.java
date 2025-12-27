package io.strategiz.service.console.observability.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Client for querying metrics from Grafana Cloud Prometheus API.
 * Fetches metrics exported via OpenTelemetry from both Java and Python services.
 */
@Component
public class GrafanaMetricsClient {

    private static final Logger log = LoggerFactory.getLogger(GrafanaMetricsClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${grafana.prometheus.url:https://prometheus-prod-01-eu-west-0.grafana.net/api/prom}")
    private String prometheusUrl;

    @Value("${grafana.prometheus.auth:}")
    private String prometheusAuth;

    public GrafanaMetricsClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Query Prometheus for a metric
     * @param query PromQL query string
     * @return Query result
     */
    public PrometheusQueryResult query(String query) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", query)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (prometheusAuth != null && !prometheusAuth.isEmpty()) {
                headers.set("Authorization", "Basic " + prometheusAuth);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseQueryResult(response.getBody());
            }

            log.warn("Failed to query Prometheus: status={}", response.getStatusCode());
            return new PrometheusQueryResult(Collections.emptyList());

        } catch (Exception e) {
            log.error("Error querying Prometheus: query={}", query, e);
            return new PrometheusQueryResult(Collections.emptyList());
        }
    }

    /**
     * Query Prometheus for a range of values
     * @param query PromQL query
     * @param start Start time (Unix timestamp)
     * @param end End time (Unix timestamp)
     * @param step Step interval (e.g., "15s", "1m")
     * @return Range query result
     */
    public PrometheusQueryResult queryRange(String query, long start, long end, String step) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("step", step)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (prometheusAuth != null && !prometheusAuth.isEmpty()) {
                headers.set("Authorization", "Basic " + prometheusAuth);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseQueryResult(response.getBody());
            }

            log.warn("Failed to query Prometheus range: status={}", response.getStatusCode());
            return new PrometheusQueryResult(Collections.emptyList());

        } catch (Exception e) {
            log.error("Error querying Prometheus range: query={}", query, e);
            return new PrometheusQueryResult(Collections.emptyList());
        }
    }

    private PrometheusQueryResult parseQueryResult(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");

            if (data == null || !data.has("result")) {
                return new PrometheusQueryResult(Collections.emptyList());
            }

            JsonNode results = data.get("result");
            List<MetricResult> metrics = new ArrayList<>();

            for (JsonNode result : results) {
                JsonNode metric = result.get("metric");
                JsonNode value = result.get("value");
                JsonNode values = result.get("values");

                Map<String, String> labels = new HashMap<>();
                if (metric != null) {
                    metric.fields().forEachRemaining(entry ->
                        labels.put(entry.getKey(), entry.getValue().asText())
                    );
                }

                if (value != null && value.isArray() && value.size() == 2) {
                    // Instant query result
                    long timestamp = value.get(0).asLong();
                    double val = Double.parseDouble(value.get(1).asText());
                    metrics.add(new MetricResult(labels, List.of(new MetricValue(timestamp, val))));
                } else if (values != null && values.isArray()) {
                    // Range query result
                    List<MetricValue> valueList = new ArrayList<>();
                    for (JsonNode v : values) {
                        if (v.isArray() && v.size() == 2) {
                            long timestamp = v.get(0).asLong();
                            double val = Double.parseDouble(v.get(1).asText());
                            valueList.add(new MetricValue(timestamp, val));
                        }
                    }
                    metrics.add(new MetricResult(labels, valueList));
                }
            }

            return new PrometheusQueryResult(metrics);

        } catch (Exception e) {
            log.error("Error parsing Prometheus response", e);
            return new PrometheusQueryResult(Collections.emptyList());
        }
    }

    /**
     * Prometheus query result
     */
    public static class PrometheusQueryResult {
        private final List<MetricResult> results;

        public PrometheusQueryResult(List<MetricResult> results) {
            this.results = results;
        }

        public List<MetricResult> getResults() {
            return results;
        }

        public boolean isEmpty() {
            return results.isEmpty();
        }

        public Optional<Double> getFirstValue() {
            if (results.isEmpty() || results.get(0).getValues().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(results.get(0).getValues().get(0).getValue());
        }
    }

    /**
     * Individual metric result with labels and values
     */
    public static class MetricResult {
        private final Map<String, String> labels;
        private final List<MetricValue> values;

        public MetricResult(Map<String, String> labels, List<MetricValue> values) {
            this.labels = labels;
            this.values = values;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public List<MetricValue> getValues() {
            return values;
        }
    }

    /**
     * Metric value with timestamp
     */
    public static class MetricValue {
        private final long timestamp;
        private final double value;

        public MetricValue(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double getValue() {
            return value;
        }
    }
}
