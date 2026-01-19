package cz.muriel.core.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@TestConfiguration
public class MockMvcTestConfig {

  @Bean
  MockMvc mockMvc(WebApplicationContext context) {
    return webAppContextSetup(context).apply(springSecurity()).build();
  }
}
