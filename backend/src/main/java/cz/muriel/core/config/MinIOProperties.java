package cz.muriel.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 📦 MinIO Configuration Properties
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinIOProperties {
    private String endpoint = "http://minio:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketPrefix = "tenant";
    private boolean versioningEnabled = true;
    private int presignedUrlExpirySeconds = 3600;
}
