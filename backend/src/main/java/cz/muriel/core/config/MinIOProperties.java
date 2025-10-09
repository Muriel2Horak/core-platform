package cz.muriel.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 📦 MinIO Configuration Properties All values configurable via
 * application.properties with minio.* prefix
 */
@Configuration @ConfigurationProperties(prefix = "minio") @Data
public class MinIOProperties {
  private String endpoint = "http://minio:9000";
  private String accessKey = "minioadmin"; // Fallback - use ${MINIO_ACCESS_KEY} in properties
  private String secretKey = "minioadmin"; // Fallback - use ${MINIO_SECRET_KEY} in properties
  private String bucketPrefix = "tenant";
  private boolean versioningEnabled = true;
  private int presignedUrlExpirySeconds = 3600;
}
