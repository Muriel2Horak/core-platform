package cz.muriel.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j @Service
public class S3StorageService {

  @Value("${spring.cloud.aws.s3.endpoint}")
  private String s3Endpoint;

  @Value("${spring.cloud.aws.credentials.access-key}")
  private String accessKey;

  @Value("${spring.cloud.aws.credentials.secret-key}")
  private String secretKey;

  @Value("${storage.s3.bucket-name}")
  private String bucketName;

  @Value("${spring.cloud.aws.s3.region}")
  private String region;

  private S3Client s3Client;

  // Povolené MIME typy pro obrázky
  private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg",
      "image/png", "image/gif", "image/webp");

  // Maximální velikost souboru (5MB)
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

  @PostConstruct
  public void init() {
    try {
      AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

      this.s3Client = S3Client.builder().endpointOverride(URI.create(s3Endpoint))
          .credentialsProvider(StaticCredentialsProvider.create(credentials))
          .region(Region.of(region)).forcePathStyle(true) // Nutné pro MinIO
          .build();

      // Vytvoř bucket pokud neexistuje
      createBucketIfNotExists();

      log.info("S3StorageService initialized successfully with endpoint: {}", s3Endpoint);
    } catch (Exception e) {
      log.error("Failed to initialize S3StorageService", e);
      throw new RuntimeException("Failed to initialize S3 storage", e);
    }
  }

  private void createBucketIfNotExists() {
    try {
      HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();

      s3Client.headBucket(headBucketRequest);
      log.info("Bucket {} already exists", bucketName);

      // Zkontroluj a povol verzování pokud není povoleno
      enableVersioningIfNeeded();

    } catch (NoSuchBucketException e) {
      log.info("Creating bucket: {}", bucketName);
      CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucketName)
          .build();
      s3Client.createBucket(createBucketRequest);
      log.info("Bucket {} created successfully", bucketName);

      // Povol verzování pro nový bucket
      enableVersioning();
    }
  }

  /**
   * Povolí verzování pro bucket
   */
  private void enableVersioning() {
    try {
      VersioningConfiguration versioningConfig = VersioningConfiguration.builder()
          .status(BucketVersioningStatus.ENABLED).build();

      PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest.builder()
          .bucket(bucketName).versioningConfiguration(versioningConfig).build();

      s3Client.putBucketVersioning(versioningRequest);
      log.info("Versioning enabled for bucket: {}", bucketName);
    } catch (Exception e) {
      log.warn("Failed to enable versioning for bucket {}: {}", bucketName, e.getMessage());
    }
  }

  /**
   * Zkontroluje a povolí verzování pokud není aktivní
   */
  private void enableVersioningIfNeeded() {
    try {
      GetBucketVersioningRequest versioningRequest = GetBucketVersioningRequest.builder()
          .bucket(bucketName).build();

      GetBucketVersioningResponse response = s3Client.getBucketVersioning(versioningRequest);

      if (response.status() != BucketVersioningStatus.ENABLED) {
        log.info("Versioning not enabled for bucket {}, enabling now...", bucketName);
        enableVersioning();
      } else {
        log.info("Versioning already enabled for bucket: {}", bucketName);
      }
    } catch (Exception e) {
      log.warn("Failed to check versioning status for bucket {}: {}", bucketName, e.getMessage());
    }
  }

  /**
   * Upload souboru do S3
   */
  public String uploadFile(MultipartFile file, String folder) throws IOException {
    validateFile(file);

    String fileName = generateFileName(file.getOriginalFilename());
    String key = folder + "/" + fileName;

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key)
          .contentType(file.getContentType()).contentLength(file.getSize()).build();

      s3Client.putObject(putObjectRequest,
          RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      log.info("File uploaded successfully: {}", key);
      return key;

    } catch (Exception e) {
      log.error("Failed to upload file: {}", key, e);
      throw new RuntimeException("Failed to upload file", e);
    }
  }

  /**
   * Smazání souboru z S3
   */
  public void deleteFile(String key) {
    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName)
          .key(key).build();

      s3Client.deleteObject(deleteObjectRequest);
      log.info("File deleted successfully: {}", key);

    } catch (Exception e) {
      log.error("Failed to delete file: {}", key, e);
      throw new RuntimeException("Failed to delete file", e);
    }
  }

  /**
   * Získání URL pro soubor - přes zabezpečený backend endpoint
   */
  public String getFileUrl(String key) {
    if (key == null || key.isEmpty()) {
      return null;
    }

    // 🔧 BEZPEČNÉ řešení: URL přes backend API s autentizací
    // Frontend bude přistupovat přes
    // https://core-platform.local/api/files/download?key=...
    String domain = System.getenv("DOMAIN");
    if (domain == null || domain.isEmpty()) {
      domain = "core-platform.local";
    }

    // URL-encode key pro bezpečnost
    try {
      String encodedKey = java.net.URLEncoder.encode(key, "UTF-8");
      return "https://" + domain + "/api/files/download?key=" + encodedKey;
    } catch (Exception e) {
      log.warn("Failed to encode file key: {}", key, e);
      return "https://" + domain + "/api/files/download?key=" + key;
    }
  }

  /**
   * Validace uploadovaného souboru
   */
  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
      throw new IllegalArgumentException(
          "File type not allowed. Allowed types: " + String.join(", ", ALLOWED_IMAGE_TYPES));
    }
  }

  /**
   * Generování jedinečného názvu souboru
   */
  private String generateFileName(String originalFilename) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String uuid = UUID.randomUUID().toString().substring(0, 8);

    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    return timestamp + "_" + uuid + extension;
  }

  /**
   * Upload profilové fotky s verzováním - používá konzistentní názvy
   */
  public String uploadProfilePicture(MultipartFile file, String userId) throws IOException {
    validateFile(file);

    // Detekuj příponu souboru
    String extension = ".jpg"; // default
    String originalFilename = file.getOriginalFilename();
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
    }

    // Konzistentní název - MinIO verzování se postará o verze
    String key = "profile-pictures/" + userId + "/avatar" + extension;

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key)
          .contentType(file.getContentType()).contentLength(file.getSize()).build();

      PutObjectResponse response = s3Client.putObject(putObjectRequest,
          RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      log.info("Profile picture uploaded successfully: {} (versionId: {})", key,
          response.versionId() != null ? response.versionId() : "latest");

      return key;

    } catch (Exception e) {
      log.error("Failed to upload profile picture: {}", key, e);
      throw new RuntimeException("Failed to upload profile picture", e);
    }
  }

  /**
   * Smazání profilové fotky s verzováním - smaže všechny verze
   */
  public void deleteProfilePicture(String userId, String oldImageKey) {
    // S verzováním používáme konzistentní názvy, takže můžeme odvodit klíč
    String expectedKey = "profile-pictures/" + userId + "/avatar";

    try {
      // Pokud máme starý klíč, zkusíme ho použít
      String keyToDelete = oldImageKey;

      // Pro nový systém verzování odvodíme klíč z userId
      if (oldImageKey == null || oldImageKey.isEmpty()) {
        // Pokusíme se najít soubor s různými příponami
        for (String ext : Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp")) {
          String testKey = expectedKey + ext;
          if (fileExists(testKey)) {
            keyToDelete = testKey;
            break;
          }
        }
      }

      if (keyToDelete != null && !keyToDelete.isEmpty()) {
        // Zkontroluj, že klíč patří tomuto uživateli
        if (keyToDelete.startsWith("profile-pictures/" + userId)) {
          deleteAllVersions(keyToDelete);
          log.info("Deleted profile picture and all versions: {} for user: {}", keyToDelete,
              userId);
        } else {
          log.warn("Attempted to delete profile picture that doesn't belong to user: {}. Key: {}",
              userId, keyToDelete);
        }
      } else {
        log.info("No profile picture found to delete for user: {}", userId);
      }
    } catch (Exception e) {
      log.error("Failed to delete profile picture for user: {}", userId, e);
    }
  }

  /**
   * Zkontroluje, zda soubor existuje
   */
  private boolean fileExists(String key) {
    try {
      HeadObjectRequest headRequest = HeadObjectRequest.builder().bucket(bucketName).key(key)
          .build();
      s3Client.headObject(headRequest);
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (Exception e) {
      log.warn("Error checking if file exists: {}", key, e);
      return false;
    }
  }

  /**
   * Smaže všechny verze souboru (pro verzované buckety)
   */
  private void deleteAllVersions(String key) {
    try {
      // Najdi všechny verze souboru
      ListObjectVersionsRequest listVersionsRequest = ListObjectVersionsRequest.builder()
          .bucket(bucketName).prefix(key).build();

      ListObjectVersionsResponse versionsResponse = s3Client
          .listObjectVersions(listVersionsRequest);

      // Smaž všechny verze
      for (ObjectVersion version : versionsResponse.versions()) {
        if (version.key().equals(key)) {
          DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucketName)
              .key(key).versionId(version.versionId()).build();

          s3Client.deleteObject(deleteRequest);
          log.debug("Deleted version {} of file {}", version.versionId(), key);
        }
      }

      // Smaž i delete markery pokud existují
      for (DeleteMarkerEntry deleteMarker : versionsResponse.deleteMarkers()) {
        if (deleteMarker.key().equals(key)) {
          DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucketName)
              .key(key).versionId(deleteMarker.versionId()).build();

          s3Client.deleteObject(deleteRequest);
          log.debug("Deleted delete marker {} of file {}", deleteMarker.versionId(), key);
        }
      }

    } catch (Exception e) {
      log.error("Failed to delete all versions of file: {}", key, e);
      // Fallback - pokus o smazání bez version ID
      deleteFile(key);
    }
  }
}
