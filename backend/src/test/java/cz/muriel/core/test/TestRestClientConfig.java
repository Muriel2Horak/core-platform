package cz.muriel.core.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@TestConfiguration
@ConditionalOnProperty(name = "local.server.port")
public class TestRestClientConfig {

  @Bean
  RestTemplate testRestTemplate(@LocalServerPort int port) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
    return restTemplate;
  }
}
