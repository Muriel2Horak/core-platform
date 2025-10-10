package cz.muriel.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 📊 Streaming & Kafka Configuration
 * 
 * Enabled only when streaming.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "streaming")
public class StreamingConfig {

    private boolean enabled = false;
    private KafkaConfig kafka = new KafkaConfig();
    private TopicConfig topic = new TopicConfig();
    private SecurityConfig security = new SecurityConfig();
    private String prometheusPort = "9090";
    private String grafanaPublicUrl;

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
