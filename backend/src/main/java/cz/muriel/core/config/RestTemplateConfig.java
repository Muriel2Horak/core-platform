package cz.muriel.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
    requestFactory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
    return new RestTemplate(requestFactory);
  }
}
