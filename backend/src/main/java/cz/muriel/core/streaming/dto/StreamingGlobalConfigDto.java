package cz.muriel.core.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingGlobalConfigDto {
    private Boolean enabled;
    private String bootstrapServers;
    private Integer replicationFactor;
    private Integer defaultPartitions;
    private Map<String, String> producerConfig;
    private Map<String, String> consumerConfig;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StreamingEntityConfigDto {
    private String entityName;
    private Boolean enabled;
    private String topicName;
    private Integer partitions;
    private String eventStrategy;
    private Integer retentionDays;
    private Integer workerPoolSize;
    private Integer batchSize;
    private Integer maxRetries;
    private Integer retryBackoffMs;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StreamingConfigResponse {
    private StreamingGlobalConfigDto global;
    private List<StreamingEntityConfigDto> entities;
}
