package cz.muriel.core.controller;

import cz.muriel.core.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController @RequestMapping("/api/tenants") @RequiredArgsConstructor @Slf4j @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER', 'CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
public class TenantController {

  private final TenantService tenantService;

  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getCurrentTenant() {
    var tenant = tenantService.getCurrentTenant();

    if (tenant.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var response = Map.<String, Object>of("key", tenant.get().getKey(), "name",
        tenant.get().getName(), "realm", tenant.get().getRealm());

    log.debug("Returning current tenant info: {}", response);
    return ResponseEntity.ok(response);
  }
}
