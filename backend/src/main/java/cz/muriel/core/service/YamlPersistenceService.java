package cz.muriel.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * YAML Persistence Service
 * 
 * Provides functionality to persist GlobalMetamodelConfig to YAML file.
 * Used primarily for saving AI configuration changes from Admin UI.
 * 
 * Features:
 * - Atomic write with backup
 * - Validation after write
 * - Rollback on error
 * 
 * Usage:
 * ```java
 * yamlPersistenceService.persistGlobalConfig(globalConfig);
 * ```
 */
@Slf4j
@Service
public class YamlPersistenceService {

  private final ObjectMapper yamlMapper;
  private final String configPath;

  public YamlPersistenceService(
      @Value("${metamodel.config.path:backend/src/main/resources/metamodel/global-config.yaml}") String configPath) {
    this.configPath = configPath;
    
    // Configure YAML mapper
    YAMLFactory yamlFactory = YAMLFactory.builder()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .build();
    
    this.yamlMapper = new ObjectMapper(yamlFactory);
    this.yamlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    this.yamlMapper.findAndRegisterModules(); // Register Java 8 time module
    
    log.info("✅ YamlPersistenceService initialized with config path: {}", configPath);
  }

  /**
   * Persist GlobalMetamodelConfig to YAML file
   * 
   * Process:
   * 1. Create backup of existing file
   * 2. Write new config to temp file
   * 3. Validate temp file can be read back
   * 4. Atomic move temp file to target
   * 5. Delete backup on success, restore on error
   * 
   * @param config GlobalMetamodelConfig to persist
   * @throws IOException if write fails
   */
  public void persistGlobalConfig(GlobalMetamodelConfig config) throws IOException {
    log.info("💾 Starting persistence of global config to: {}", configPath);

    Path targetPath = Paths.get(configPath);
    Path backupPath = Paths.get(configPath + ".backup");
    Path tempPath = Paths.get(configPath + ".tmp");

    try {
      // Step 1: Create backup if file exists
      if (Files.exists(targetPath)) {
        log.debug("📦 Creating backup: {}", backupPath);
        Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
      }

      // Step 2: Write to temp file
      log.debug("✍️ Writing config to temp file: {}", tempPath);
      yamlMapper.writeValue(tempPath.toFile(), config);

      // Step 3: Validate temp file
      log.debug("🔍 Validating temp file...");
      GlobalMetamodelConfig validated = yamlMapper.readValue(tempPath.toFile(), GlobalMetamodelConfig.class);
      
      if (validated == null || validated.getAi() == null) {
        throw new IOException("Validation failed: AI config is null after write");
      }

      // Step 4: Atomic move
      log.debug("🔄 Moving temp file to target (atomic)...");
      Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

      // Step 5: Delete backup on success
      if (Files.exists(backupPath)) {
        log.debug("🗑️ Deleting backup file");
        Files.delete(backupPath);
      }

      log.info("✅ Global config persisted successfully: enabled={}, mode={}", 
          validated.getAi().getEnabled(), 
          validated.getAi().getMode());

    } catch (Exception e) {
      log.error("❌ Failed to persist global config", e);

      // Rollback: restore from backup
      if (Files.exists(backupPath)) {
        log.warn("🔙 Rolling back from backup...");
        Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(backupPath);
      }

      // Clean up temp file
      if (Files.exists(tempPath)) {
        Files.delete(tempPath);
      }

      throw new IOException("Failed to persist global config: " + e.getMessage(), e);
    }
  }

  /**
   * Read GlobalMetamodelConfig from YAML file
   * 
   * @return GlobalMetamodelConfig
   * @throws IOException if read fails
   */
  public GlobalMetamodelConfig readGlobalConfig() throws IOException {
    log.debug("📖 Reading global config from: {}", configPath);
    
    File configFile = new File(configPath);
    if (!configFile.exists()) {
      throw new IOException("Config file not found: " + configPath);
    }

    GlobalMetamodelConfig config = yamlMapper.readValue(configFile, GlobalMetamodelConfig.class);
    
    log.debug("✅ Global config read successfully");
    return config;
  }

  /**
   * Check if config file exists
   */
  public boolean configExists() {
    return Files.exists(Paths.get(configPath));
  }
}
