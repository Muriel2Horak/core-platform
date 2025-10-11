package cz.muriel.core.reporting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Cube.js HTTP client.
 */
@Configuration
public class CubeWebClientConfiguration {

  @Value("${cube.api.url:http://localhost:4000}")
  private String cubeApiUrl;

  @Bean
  public WebClient cubeWebClient() {
    return WebClient.builder().baseUrl(cubeApiUrl).defaultHeader("Content-Type", "application/json")
        .build();
  }
}
