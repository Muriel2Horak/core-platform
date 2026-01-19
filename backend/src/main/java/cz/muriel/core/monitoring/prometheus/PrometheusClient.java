package cz.muriel.core.monitoring.prometheus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

/**
 * Simple Prometheus HTTP API client for range queries.
 */
@Component
@RequiredArgsConstructor
public class PrometheusClient {

  private final RestTemplate restTemplate;

  @Value("${monitoring.prometheus.base-url:http://prometheus:9090}")
  private String baseUrl;

  public Map<String, Object> queryRange(String query, Instant start, Instant end, Duration step) {
    String url = UriComponentsBuilder.fromUriString(baseUrl)
        .path("/api/v1/query_range")
        .queryParam("query", query)
        .queryParam("start", start.getEpochSecond())
        .queryParam("end", end.getEpochSecond())
        .queryParam("step", Math.max(step.getSeconds(), 1))
        .toUriString();

    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, null,
        new ParameterizedTypeReference<Map<String, Object>>() {});
    return response.getBody();
  }
}
