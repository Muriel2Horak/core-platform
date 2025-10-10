package cz.muriel.core.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 📊 Streaming & Kafka Configuration
 * 
 * Enabled only when streaming.enabled=true
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "streaming")
public class StreamingConfig {

    private boolean enabled = false;
    private KafkaConfig kafka = new KafkaConfig();
    private TopicConfig topic = new TopicConfig();
    private SecurityConfig security = new SecurityConfig();
    private String prometheusPort = "9090";
    private String grafanaPublicUrl;

    @Bean
    public AdminClient kafkaAdminClient() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getServers());
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000);
        
        if ("SASL_SSL".equals(security.getMode())) {
            props.put("security.protocol", "SASL_SSL");
            // Add SASL config if needed
        }
        
        return AdminClient.create(props);
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Idempotence & reliability settings
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 10);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Performance settings
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public KafkaConfig getKafka() {
        return kafka;
    }

    public void setKafka(KafkaConfig kafka) {
        this.kafka = kafka;
    }

    public TopicConfig getTopic() {
        return topic;
    }

    public void setTopic(TopicConfig topic) {
        this.topic = topic;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public String getPrometheusPort() {
        return prometheusPort;
    }

    public void setPrometheusPort(String prometheusPort) {
        this.prometheusPort = prometheusPort;
    }

    public String getGrafanaPublicUrl() {
        return grafanaPublicUrl;
    }

    public void setGrafanaPublicUrl(String grafanaPublicUrl) {
        this.grafanaPublicUrl = grafanaPublicUrl;
    }

    public static class KafkaConfig {
        private String servers = "kafka:9092";

        public String getServers() {
            return servers;
        }

        public void setServers(String servers) {
            this.servers = servers;
        }
    }

    public static class TopicConfig {
        private String prefix = "core";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    public static class SecurityConfig {
        private String mode = "PLAINTEXT";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }
}
