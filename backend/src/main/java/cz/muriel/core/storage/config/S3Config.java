package cz.muriel.core.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Konfigurace pro S3-kompatibilní úložiště. Zajišťuje vytvoření S3 klienta a
 * automatické vytvoření bucketu při startu aplikace.
 */
@Configuration
public class S3Config {

  @Value("${storage.s3.bucket-name}")
  private String bucketName;

  /**
   * Bean, který se spustí po startu aplikace a zajistí, že S3 bucket existuje.
   * Pokud bucket neexistuje, vytvoří ho.
   *
   * @param s3Client S3 klient.
   * @return ApplicationRunner, který provede kontrolu a vytvoření bucketu.
   */
  @Bean
  public ApplicationRunner createBucketOnStartup(S3Client s3Client) {
    return args -> {
      try {
        // Zkusí získat informace o bucketu
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        System.out.println("S3 Bucket '" + bucketName + "' already exists.");
      } catch (NoSuchBucketException e) {
        // Pokud bucket neexistuje, vytvoří ho
        System.out.println("S3 Bucket '" + bucketName + "' not found. Creating it...");
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        System.out.println("S3 Bucket '" + bucketName + "' created successfully.");
      }
    };
  }
}
