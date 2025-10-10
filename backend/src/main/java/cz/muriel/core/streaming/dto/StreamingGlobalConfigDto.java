package cz.muriel.core.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StreamingGlobalConfigDto {
    private Boolean enabled;
    private String bootstrapServers;
    private Integer replicationFactor;
    private Integer defaultPartitions;
    private Map<String, String> producerConfig;
    private Map<String, String> consumerConfig;
}
