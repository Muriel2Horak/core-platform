package cz.muriel.core.service.ai;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test for Context Assembler (META_ONLY validation)
 * 
 * @since 2025-10-14
 */
@ExtendWith(MockitoExtension.class)
class ContextAssemblerTest {

  @Mock
  private MetamodelRegistry registry;

  @Mock
  private UiContextService uiContextService;

  @Mock
  private WfContextService wfContextService;

  private GlobalMetamodelConfig globalConfig;
  private ContextAssembler assembler;

  @BeforeEach
  void setUp() {
    globalConfig = new GlobalMetamodelConfig();
    GlobalAiConfig aiConfig = new GlobalAiConfig();
    aiConfig.setEnabled(true);
    aiConfig.setMode(AiVisibilityMode.META_ONLY);
    globalConfig.setAi(aiConfig);

    assembler = new ContextAssembler(registry, uiContextService, wfContextService, globalConfig);
  }

  @Test
  void testAssembleContext_META_ONLY() {
    // Given
    String routeId = "users.detail";
    UUID tenantId = UUID.randomUUID();

    Map<String, Object> uiContext = Map.of("screen",
        Map.of("title", "User Detail", "viewKind", "detail"), "fields",
        List.of(Map.of("name", "email", "type", "email", "pii", true)), "validations", List.of());

    Map<String, Object> wfContext = Map.of("entity", "User", "states", List.of(), "actions",
        List.of());

    when(uiContextService.getCurrentView(routeId)).thenReturn(uiContext);
    when(wfContextService.getWorkflowForRoute(routeId)).thenReturn(wfContext);

    // When
    Map<String, Object> context = assembler.assembleContext(routeId, tenantId);

    // Then
    assertNotNull(context);
    assertTrue(context.containsKey("screen"));
    assertTrue(context.containsKey("fields"));
    assertTrue(context.containsKey("metadata"));

    // Verify META_ONLY mode
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) context.get("metadata");
    @SuppressWarnings("unchecked")
    Map<String, Object> policy = (Map<String, Object>) metadata.get("policy");
    assertEquals("META_ONLY", policy.get("visibility"));

    // Verify no actual values in fields (META_ONLY check)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fields = (List<Map<String, Object>>) context.get("fields");
    for (Map<String, Object> field : fields) {
      assertFalse(field.containsKey("value"), "META_ONLY mode must not contain 'value' field");
      assertFalse(field.containsKey("data"), "META_ONLY mode must not contain 'data' field");
    }
  }

  @Test
  void testAssembleContext_AiDisabled() {
    // Given
    globalConfig.getAi().setEnabled(false);

    // When/Then
    assertThrows(IllegalStateException.class, () -> {
      assembler.assembleContext("users.detail", UUID.randomUUID());
    });
  }

  @Test
  void testAssembleContext_ForcesMetaOnly() {
    // Given - try to set FULL mode
    globalConfig.getAi().setMode(AiVisibilityMode.FULL);

    String routeId = "users.detail";
    UUID tenantId = UUID.randomUUID();

    when(uiContextService.getCurrentView(routeId)).thenReturn(Map.of("screen",
        Map.of("title", "User Detail"), "fields", List.of(), "validations", List.of()));

    // When
    Map<String, Object> context = assembler.assembleContext(routeId, tenantId);

    // Then - should still be META_ONLY
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) context.get("metadata");
    @SuppressWarnings("unchecked")
    Map<String, Object> policy = (Map<String, Object>) metadata.get("policy");
    assertEquals("META_ONLY", policy.get("visibility"), "Should force META_ONLY for safety");
  }
}
