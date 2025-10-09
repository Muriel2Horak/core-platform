package cz.muriel.core.controller;

import cz.muriel.core.dto.GroupDto;
import cz.muriel.core.entity.GroupEntity;
import cz.muriel.core.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 👥 GROUP MANAGEMENT API
 * 
 * Správa aplikačních skupin (synchronized from Keycloak)
 * 
 * Poznámka: Skupiny jsou synchronizované z Keycloaku do databáze. Pro úplnou
 * správu (create/update/delete) budou potřeba operace přes Keycloak API.
 */
@Slf4j @RestController @RequestMapping("/api/groups") @RequiredArgsConstructor
public class GroupController {

  private final GroupService groupService;

  /**
   * 📋 GET /api/groups - Seznam všech skupin pro aktuální tenant
   */
  @GetMapping @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN', 'CORE_ROLE_USER')")
  public ResponseEntity<List<GroupEntity>> getAllGroups() {
    log.info("Getting all groups for current tenant");
    List<GroupEntity> groups = groupService.getAllGroups();
    return ResponseEntity.ok(groups);
  }

  /**
   * 🔍 GET /api/groups/{id} - Detail skupiny podle ID
   */
  @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN', 'CORE_ROLE_USER')")
  public ResponseEntity<GroupEntity> getGroupById(@PathVariable UUID id) {
    log.info("Getting group by ID: {}", id);
    return groupService.getGroupById(id).map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * 📁 GET /api/groups/root - Root skupiny (top-level bez parent)
   */
  @GetMapping("/root") @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN', 'CORE_ROLE_USER')")
  public ResponseEntity<List<GroupEntity>> getRootGroups() {
    log.info("Getting root groups for current tenant");
    List<GroupEntity> groups = groupService.getRootGroups();
    return ResponseEntity.ok(groups);
  }

  /**
   * 👥 GET /api/groups/{groupName}/members - Členové skupiny
   * 
   * TODO: Implementovat načítání členů skupiny z UserDirectoryEntity
   */
  @GetMapping("/{groupName}/members") @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<List<?>> getGroupMembers(@PathVariable String groupName) {
    log.info("Getting members for group: {}", groupName);
    // TODO: Implementovat načítání members z UserDirectoryEntity
    // Prozatím vrátíme prázdný seznam
    return ResponseEntity.ok(List.of());
  }

  /**
   * 🆕 POST /api/groups - Vytvoření nové skupiny
   */
  @PostMapping @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<GroupEntity> createGroup(@RequestBody GroupDto groupDto) {
    log.info("Creating new group: {}", groupDto.getName());

    GroupEntity group = new GroupEntity();
    group.setName(groupDto.getName());
    group.setPath(groupDto.getPath());
    group.setKeycloakGroupId(groupDto.getKeycloakGroupId());

    // TODO: Handle parentGroupId if provided

    GroupEntity createdGroup = groupService.createGroup(group);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
  }

  /**
   * ✏️ PUT /api/groups/{groupName} - Aktualizace skupiny
   */
  @PutMapping("/{groupName}") @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<GroupEntity> updateGroup(@PathVariable String groupName,
      @RequestBody GroupDto groupDto) {
    log.info("Updating group: {}", groupName);

    // Find group by name
    GroupEntity existingGroup = groupService.getGroupByName(groupName)
        .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupName));

    GroupEntity updatedData = new GroupEntity();
    updatedData.setName(groupDto.getName());
    updatedData.setPath(groupDto.getPath());
    updatedData.setKeycloakGroupId(groupDto.getKeycloakGroupId());

    GroupEntity updatedGroup = groupService.updateGroup(existingGroup.getId(), updatedData);
    return ResponseEntity.ok(updatedGroup);
  }

  /**
   * 🗑️ DELETE /api/groups/{groupName} - Smazání skupiny
   */
  @DeleteMapping("/{groupName}") @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<Void> deleteGroup(@PathVariable String groupName) {
    log.info("Deleting group: {}", groupName);

    // Find group by name
    GroupEntity group = groupService.getGroupByName(groupName)
        .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupName));

    groupService.deleteGroup(group.getId());
    return ResponseEntity.noContent().build();
  }
}
