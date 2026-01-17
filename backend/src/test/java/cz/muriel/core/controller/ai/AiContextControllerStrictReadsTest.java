package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import cz.muriel.core.metrics.AiMetricsCollector;
import cz.muriel.core.service.ai.ContextAssembler;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.streaming.service.WorkStateService;
import cz.muriel.core.locks.EditLockService;
import cz.muriel.core.locks.EditLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiContextControllerStrictReadsTest {

  @Test
  void strictReads_enforcesWorkStateWhenEntityIdProvided() {
    ContextAssembler assembler = mock(ContextAssembler.class);
    AiMetricsCollector metricsCollector = mock(AiMetricsCollector.class);
    WorkStateService workStateService = mock(WorkStateService.class);
    TenantService tenantService = mock(TenantService.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<WorkStateService> provider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<EditLockService> editLockProvider = mock(ObjectProvider.class);

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    when(provider.getIfAvailable()).thenReturn(workStateService);
    when(editLockProvider.getIfAvailable()).thenReturn(null);
    when(assembler.assembleContext(anyString(), any())).thenReturn(Map.of("ok", true));

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService, editLockProvider);

    UUID tenantId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();

    when(tenantService.getTenantKeyFromId(eq(tenantId))).thenReturn("tenant");

    controller.getContext("users.detail", tenantId, true, null, entityId);

    verify(workStateService).enforceStrictReads(eq("User"), eq(entityId));
  }

  @Test
  void strictReads_usesEditLockFallbackWhenStreamingDisabled() {
    ContextAssembler assembler = mock(ContextAssembler.class);
    AiMetricsCollector metricsCollector = mock(AiMetricsCollector.class);
    TenantService tenantService = mock(TenantService.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<WorkStateService> provider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<EditLockService> editLockProvider = mock(ObjectProvider.class);
    EditLockService editLockService = mock(EditLockService.class);

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    when(provider.getIfAvailable()).thenReturn(null);
    when(editLockProvider.getIfAvailable()).thenReturn(editLockService);
    when(assembler.assembleContext(anyString(), any())).thenReturn(Map.of("ok", true));

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService, editLockProvider);

    UUID tenantId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    when(tenantService.getTenantKeyFromId(eq(tenantId))).thenReturn("tenant");
    when(editLockService.getLock(eq(tenantId), eq("User"), eq(entityId.toString())))
        .thenReturn(
            java.util.Optional.of(EditLock.builder().userId("user-1").build()));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.getContext("users.detail", tenantId, true, null, entityId));
    assertEquals(423, ex.getStatusCode().value());
  }
}
