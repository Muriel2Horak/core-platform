package cz.muriel.core.reporting.api;

import cz.muriel.core.reporting.cube.CubeSecurityContext;
import cz.muriel.core.reporting.model.ReportView;
import cz.muriel.core.reporting.repo.ReportViewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for managing saved report views.
 */
@Slf4j @RestController @RequestMapping("/api/reports/views") @RequiredArgsConstructor
public class ReportViewController {

  private final ReportViewRepository reportViewRepository;
  private final CubeSecurityContext cubeSecurityContext;

  /**
   * Get all accessible views for an entity.
   * 
   * GET /api/reports/views?entity=User
   */
  @GetMapping
  public ResponseEntity<List<ReportView>> getAccessibleViews(@RequestParam String entity,
      Authentication authentication) {

    String tenantId = cubeSecurityContext.extractTenantId(authentication);

    // For now, return all tenant views (simplified - no group filtering)
    List<ReportView> views = reportViewRepository.findByTenantIdAndEntity(UUID.fromString(tenantId),
        entity);

    return ResponseEntity.ok(views);
  }

  /**
   * Get a specific view by ID.
   * 
   * GET /api/reports/views/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<ReportView> getView(@PathVariable UUID id) {
    return reportViewRepository.findById(id).map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Create a new view.
   * 
   * POST /api/reports/views
   */
  @PostMapping @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'USER')")
  public ResponseEntity<ReportView> createView(@Valid @RequestBody ReportView view,
      Authentication authentication) {

    String tenantId = cubeSecurityContext.extractTenantId(authentication);
    String userId = cubeSecurityContext.extractUserId(authentication);

    // Set tenant and owner
    view.setTenantId(UUID.fromString(tenantId));
    if (view.getScope() == ReportView.Scope.PRIVATE) {
      view.setOwnerId(UUID.fromString(userId));
    }

    ReportView saved = reportViewRepository.save(view);

    log.info("Created report view: id={}, entity={}, name={}", saved.getId(), saved.getEntity(),
        saved.getName());

    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * Update an existing view.
   * 
   * PUT /api/reports/views/{id}
   */
  @PutMapping("/{id}") @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'USER')")
  public ResponseEntity<ReportView> updateView(@PathVariable UUID id,
      @Valid @RequestBody ReportView view, Authentication authentication) {

    String tenantId = cubeSecurityContext.extractTenantId(authentication);

    return reportViewRepository.findById(id).map(existing -> {
      // Verify ownership
      if (!existing.getTenantId().equals(UUID.fromString(tenantId))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).<ReportView>build();
      }

      // Update fields
      existing.setName(view.getName());
      existing.setDefinition(view.getDefinition());
      existing.setScope(view.getScope());
      existing.setIsDefault(view.getIsDefault());

      ReportView updated = reportViewRepository.save(existing);

      log.info("Updated report view: id={}, name={}", id, updated.getName());

      return ResponseEntity.ok(updated);
    }).orElse(ResponseEntity.notFound().build());
  }

  /**
   * Delete a view.
   * 
   * DELETE /api/reports/views/{id}
   */
  @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'USER')")
  public ResponseEntity<Void> deleteView(@PathVariable UUID id, Authentication authentication) {

    String tenantId = cubeSecurityContext.extractTenantId(authentication);

    return reportViewRepository.findById(id).map(view -> {
      // Verify ownership
      if (!view.getTenantId().equals(UUID.fromString(tenantId))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
      }

      reportViewRepository.delete(view);

      log.info("Deleted report view: id={}, name={}", id, view.getName());

      return ResponseEntity.noContent().<Void>build();
    }).orElse(ResponseEntity.notFound().build());
  }
}
