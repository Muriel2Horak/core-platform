package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import cz.muriel.core.service.YamlPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AI Config Persistence
 * 
 * Tests:
 * - YAML persistence after config update
 * - Atomic write with backup
 * - Hot reload after update
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AI Config Persistence IT")
class AiConfigPersistenceIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private YamlPersistenceService yamlPersistenceService;

  @Autowired
  private GlobalMetamodelConfig globalConfig;

  @Autowired
  private MetamodelRegistry metamodelRegistry;

  private Path testConfigPath;
  private Path backupPath;
  private GlobalAiConfig originalConfig;

  @BeforeEach
  void setUp() throws IOException {
    // Setup test paths
    testConfigPath = Paths.get("backend/src/main/resources/metamodel/global-config.yaml");
    backupPath = Paths.get("backend/src/main/resources/metamodel/global-config.yaml.test-backup");

    // Backup original config
    if (Files.exists(testConfigPath)) {
      Files.copy(testConfigPath, backupPath);
    }

    // Store original AI config
    originalConfig = globalConfig.getAi();
  }

  @AfterEach
  void tearDown() throws IOException {
    // Restore original config
    if (Files.exists(backupPath)) {
      Files.copy(backupPath, testConfigPath);
      Files.delete(backupPath);
    }

    // Restore in-memory config
    if (originalConfig != null) {
      globalConfig.setAi(originalConfig);
    }
  }

  @Test
  @WithMockUser(roles = "PLATFORM_ADMIN")
  void testPersistAiConfig_success() throws Exception {
    // Given: New AI config
    GlobalAiConfig newConfig = new GlobalAiConfig();
    newConfig.setEnabled(true);
    newConfig.setMode(AiVisibilityMode.META_ONLY);

    String json = """
        {
          "enabled": true,
          "mode": "META_ONLY"
        }
        """;

    // When: Update config via REST API
    mockMvc.perform(put("/api/admin/ai/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").exists());

    // Then: Config should be persisted to YAML
    assertThat(Files.exists(testConfigPath)).isTrue();

    // Verify by reading back
    GlobalMetamodelConfig persisted = yamlPersistenceService.readGlobalConfig();
    assertThat(persisted.getAi()).isNotNull();
    assertThat(persisted.getAi().getEnabled()).isTrue();
    assertThat(persisted.getAi().getMode()).isEqualTo(AiVisibilityMode.META_ONLY);
  }

  @Test
  @WithMockUser(roles = "PLATFORM_ADMIN")
  void testPersistAiConfig_enforcesMetaOnly() throws Exception {
    // Given: Config with FULL mode (not allowed)
    String json = """
        {
          "enabled": true,
          "mode": "FULL"
        }
        """;

    // When: Update config
    mockMvc.perform(put("/api/admin/ai/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk());

    // Then: Mode should be forced to META_ONLY
    GlobalMetamodelConfig persisted = yamlPersistenceService.readGlobalConfig();
    assertThat(persisted.getAi().getMode()).isEqualTo(AiVisibilityMode.META_ONLY);
  }

  @Test
  @WithMockUser(roles = "TENANT_ADMIN")
  void testPersistAiConfig_forbidden() throws Exception {
    // Given: TenantAdmin tries to update
    String json = """
        {
          "enabled": true,
          "mode": "META_ONLY"
        }
        """;

    // When/Then: Should be forbidden
    mockMvc.perform(put("/api/admin/ai/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "PLATFORM_ADMIN")
  void testDisableAiConfig() throws Exception {
    // Given: Disable AI
    String json = """
        {
          "enabled": false,
          "mode": "META_ONLY"
        }
        """;

    // When: Update config
    mockMvc.perform(put("/api/admin/ai/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk());

    // Then: Should be disabled in YAML
    GlobalMetamodelConfig persisted = yamlPersistenceService.readGlobalConfig();
    assertThat(persisted.getAi().getEnabled()).isFalse();

    // And: Status endpoint should reflect disabled state
    mockMvc.perform(get("/api/admin/ai/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.status").value("disabled"));
  }

  @Test
  void testYamlPersistence_atomicWrite() throws IOException {
    // Given: Valid config
    GlobalAiConfig aiConfig = new GlobalAiConfig();
    aiConfig.setEnabled(true);
    aiConfig.setMode(AiVisibilityMode.META_ONLY);
    globalConfig.setAi(aiConfig);

    // When: Persist
    yamlPersistenceService.persistGlobalConfig(globalConfig);

    // Then: File should exist
    assertThat(Files.exists(testConfigPath)).isTrue();

    // And: Backup should be deleted after success
    Path backupPath = Paths.get(testConfigPath + ".backup");
    assertThat(Files.exists(backupPath)).isFalse();

    // And: Temp file should be deleted
    Path tempPath = Paths.get(testConfigPath + ".tmp");
    assertThat(Files.exists(tempPath)).isFalse();
  }

  @Test
  void testYamlPersistence_readBack() throws IOException {
    // Given: Config with specific values
    GlobalAiConfig aiConfig = new GlobalAiConfig();
    aiConfig.setEnabled(true);
    aiConfig.setMode(AiVisibilityMode.META_ONLY);
    globalConfig.setAi(aiConfig);

    // When: Persist and read back
    yamlPersistenceService.persistGlobalConfig(globalConfig);
    GlobalMetamodelConfig readBack = yamlPersistenceService.readGlobalConfig();

    // Then: Should match
    assertThat(readBack.getAi()).isNotNull();
    assertThat(readBack.getAi().getEnabled()).isTrue();
    assertThat(readBack.getAi().getMode()).isEqualTo(AiVisibilityMode.META_ONLY);
  }

  @Test
  @WithMockUser(roles = "PLATFORM_ADMIN")
  @DisplayName("Update AI config should trigger hot reload")
  void testUpdateAiConfig_triggersHotReload() throws Exception {
    // Given: Initial schema count
    int initialSchemaCount = metamodelRegistry.getAllSchemas().size();

    String json = """
        {
          "enabled": true,
          "mode": "META_ONLY"
        }
        """;

    // When: Update AI config
    mockMvc.perform(put("/api/admin/ai/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("AI config updated, persisted, and metamodel reloaded successfully"));

    // Then: Schema count should be preserved (hot reload successful)
    int afterSchemaCount = metamodelRegistry.getAllSchemas().size();
    assertThat(afterSchemaCount).isEqualTo(initialSchemaCount);

    // And: Config should be persisted and match
    GlobalMetamodelConfig persisted = yamlPersistenceService.readGlobalConfig();
    assertThat(persisted.getAi().getEnabled()).isTrue();
    assertThat(persisted.getAi().getMode()).isEqualTo(AiVisibilityMode.META_ONLY);
  }
}
