package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import cz.muriel.core.metrics.AiMetricsCollector;
import cz.muriel.core.service.ai.ContextAssembler;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.streaming.service.WorkStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.UUID;

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

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    when(provider.getIfAvailable()).thenReturn(workStateService);
    when(assembler.assembleContext(anyString(), any())).thenReturn(Map.of("ok", true));

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService);

    UUID tenantId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();

    when(tenantService.getTenantKeyFromId(eq(tenantId))).thenReturn("tenant");

    controller.getContext("users.detail", tenantId, true, null, entityId);

    verify(workStateService).enforceStrictReads(eq("User"), eq(entityId));
  }
}
