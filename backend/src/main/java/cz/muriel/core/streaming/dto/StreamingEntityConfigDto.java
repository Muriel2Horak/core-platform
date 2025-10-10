package cz.muriel.core.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StreamingEntityConfigDto {
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
