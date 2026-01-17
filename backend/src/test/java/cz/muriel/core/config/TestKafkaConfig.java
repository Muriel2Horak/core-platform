package cz.muriel.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.mockito.Mockito.mock;

/**
 * Test Configuration for Kafka
 * 
 * Provides mock KafkaTemplate bean for tests This is needed because Kafka
 * autoconfiguration is disabled in test profile
 */
@TestConfiguration
public class TestKafkaConfig {

  @Bean @Primary
  public KafkaTemplate<String, String> kafkaTemplate() {
    // Return mock KafkaTemplate for tests
    ProducerFactory<String, String> producerFactory = mock(ProducerFactory.class);
    return new KafkaTemplate<>(producerFactory);
  }
}
