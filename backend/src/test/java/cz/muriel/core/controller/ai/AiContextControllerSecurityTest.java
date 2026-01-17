package cz.muriel.core.controller.ai;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiVisibilityMode;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import cz.muriel.core.metrics.AiMetricsCollector;
import cz.muriel.core.service.ai.ContextAssembler;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.streaming.service.WorkStateService;
import cz.muriel.core.locks.EditLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiContextControllerSecurityTest {

  @Test
  void getContext_usesTenantIdFromJwtWhenMissingParam() {
    UUID tenantId = UUID.randomUUID();
    Jwt jwt = createMockJwt(tenantId.toString());
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

    ContextAssembler assembler = mock(ContextAssembler.class);
    AiMetricsCollector metricsCollector = mock(AiMetricsCollector.class);
    TenantService tenantService = mock(TenantService.class);
    ObjectProvider<WorkStateService> provider = providerOf(null);
    ObjectProvider<EditLockService> editLockProvider = providerOf(null);

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    when(assembler.assembleContext(anyString(), any())).thenReturn(Map.of("ok", true));

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService, editLockProvider);

    try {
      when(tenantService.getTenantKeyFromId(eq(tenantId))).thenReturn("tenant");
      controller.getContext("users.detail", null, false, null, null);
      verify(assembler).assembleContext(eq("users.detail"), eq(tenantId));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void getContext_rejectsMissingTenantClaim() {
    Jwt jwt = createMockJwt(null);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

    ContextAssembler assembler = mock(ContextAssembler.class);
    AiMetricsCollector metricsCollector = mock(AiMetricsCollector.class);
    TenantService tenantService = mock(TenantService.class);
    ObjectProvider<WorkStateService> provider = providerOf(null);
    ObjectProvider<EditLockService> editLockProvider = providerOf(null);

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService, editLockProvider);

    try {
      ResponseStatusException ex = assertThrows(ResponseStatusException.class,
          () -> controller.getContext("users.detail", null, false, null, null));
      assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void getContext_rejectsMissingAuthentication() {
    SecurityContextHolder.clearContext();

    ContextAssembler assembler = mock(ContextAssembler.class);
    AiMetricsCollector metricsCollector = mock(AiMetricsCollector.class);
    TenantService tenantService = mock(TenantService.class);
    ObjectProvider<WorkStateService> provider = providerOf(null);
    ObjectProvider<EditLockService> editLockProvider = providerOf(null);

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService, editLockProvider);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.getContext("users.detail", null, false, null, null));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void getContext_rejectsInvalidTenantClaimFormat() {
    Jwt jwt = createMockJwt("not-a-uuid");
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

    ContextAssembler assembler = mock(ContextAssembler.class);
    AiMetricsCollector metricsCollector = mock(AiMetricsCollector.class);
    TenantService tenantService = mock(TenantService.class);
    ObjectProvider<WorkStateService> provider = providerOf(null);
    ObjectProvider<EditLockService> editLockProvider = providerOf(null);

    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig ai = new GlobalAiConfig();
    ai.setEnabled(true);
    ai.setMode(AiVisibilityMode.META_ONLY);
    config.setAi(ai);

    AiContextController controller = new AiContextController(assembler, config, metricsCollector,
        provider, tenantService, editLockProvider);

    try {
      ResponseStatusException ex = assertThrows(ResponseStatusException.class,
          () -> controller.getContext("users.detail", null, false, null, null));
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private static <T> ObjectProvider<T> providerOf(T value) {
    return new ObjectProvider<>() {
      @Override
      public T getObject(Object... args) {
        if (value == null) {
          throw new IllegalStateException("No bean available");
        }
        return value;
      }

      @Override
      public T getIfAvailable() {
        return value;
      }

      @Override
      public T getIfUnique() {
        return value;
      }

      @Override
      public Stream<T> stream() {
        return value != null ? Stream.of(value) : Stream.empty();
      }

      @Override
      public Stream<T> orderedStream() {
        return stream();
      }
    };
  }

  private Jwt createMockJwt(String tenantId) {
    Map<String, Object> claims = tenantId != null
        ? Map.of("tenant_id", tenantId, "sub", "user@example.com")
        : Map.of("sub", "user@example.com");

    return new Jwt("mock-token", Instant.now(), Instant.now().plusSeconds(3600),
        Map.of("alg", "RS256"), claims);
  }
}
