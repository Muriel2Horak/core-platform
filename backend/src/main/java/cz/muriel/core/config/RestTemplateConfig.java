package cz.muriel.core.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 🌐 RestTemplate Configuration
 * 
 * Konfigurace REST klienta pro HTTP komunikaci
 */
@Configuration
public class RestTemplateConfig {

  /**
   * 🔧 RestTemplate Bean
   * 
   * Používán pro: - Grafana Admin API calls - Externí HTTP komunikace
   */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.connectTimeout(Duration.ofSeconds(10)).readTimeout(Duration.ofSeconds(30))
        .build();
  }
}
