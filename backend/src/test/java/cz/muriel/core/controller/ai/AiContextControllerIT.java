package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for AI Context Controller
 * 
 * Tests META_ONLY enforcement and AI disabled scenarios.
 * 
 * @since 2025-10-14
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AiContextControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private GlobalMetamodelConfig globalConfig;

  @Test
  void testAiContextHealth() throws Exception {
    mockMvc.perform(get("/api/ai/health")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists()).andExpect(jsonPath("$.mode").exists());
  }

  @Test
  void testAiContext_WhenDisabled() throws Exception {
    // Ensure AI is disabled
    if (globalConfig.getAi() != null) {
      globalConfig.getAi().setEnabled(false);
    }

    mockMvc.perform(get("/api/ai/context").param("routeId", "users.detail"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("AI_DISABLED"));
  }

  @Test
  void testAiContext_MetaOnly_NoValues() throws Exception {
    // Enable AI with META_ONLY mode
    if (globalConfig.getAi() == null) {
      globalConfig.setAi(new GlobalAiConfig());
    }
    globalConfig.getAi().setEnabled(true);
    globalConfig.getAi().setMode(AiVisibilityMode.META_ONLY);

    mockMvc.perform(get("/api/ai/context").param("routeId", "users.detail"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.metadata").exists())
        .andExpect(jsonPath("$.metadata.policy.visibility").value("META_ONLY"))
        // Ensure no 'value' or 'data' fields in response
        .andExpect(jsonPath("$..value").doesNotExist())
        .andExpect(jsonPath("$..rows").doesNotExist());
  }
}
