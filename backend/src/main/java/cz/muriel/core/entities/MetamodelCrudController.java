package cz.muriel.core.entities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Generic CRUD REST controller for metamodel entities
 */
@Slf4j @RestController @RequestMapping("/api/entities") @RequiredArgsConstructor
public class MetamodelCrudController {

  private final MetamodelCrudService crudService;

  /**
   * List entities with filtering, sorting and pagination
   * 
   * GET
   * /api/entities/UserProfile?filter=department=Engineering&sort=-created_at&page=0&size=20
   */
  @GetMapping("/{type}")
  public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String type,
      @RequestParam(required = false) Map<String, String> filter,
      @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size, Authentication auth) {
    log.debug("List {}: filter={}, sort={}, page={}, size={}", type, filter, sort, page, size);

    Map<String, String> filters = filter != null ? filter : Map.of();
    List<Map<String, Object>> entities = crudService.list(type, filters, sort, page, size, auth);

    return ResponseEntity.ok(entities);
  }

  /**
   * Get entity by ID
   * 
   * GET /api/entities/UserProfile/123 Response headers: ETag: W/"5"
   */
  @GetMapping("/{type}/{id}")
  public ResponseEntity<Map<String, Object>> getById(@PathVariable String type,
      @PathVariable String id, Authentication auth) {
    log.debug("Get {} id={}", type, id);

    Map<String, Object> entity = crudService.getById(type, id, auth);

    // Set ETag from version
    Object version = entity.get("version");
    String etag = version != null ? "W/\"" + version + "\"" : "W/\"0\"";

    return ResponseEntity.ok().eTag(etag).body(entity);
  }

  /**
   * Create new entity
   * 
   * POST /api/entities/UserProfile Body: { "full_name": "John Doe", "email":
   * "john@example.com" }
   */
  @PostMapping("/{type}")
  public ResponseEntity<Map<String, Object>> create(@PathVariable String type,
      @RequestBody Map<String, Object> data, Authentication auth) {
    log.debug("Create {}: {}", type, data);

    Map<String, Object> created = crudService.create(type, data, auth);

    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Update entity with optimistic locking
   * 
   * PUT /api/entities/UserProfile/123 Headers: If-Match: W/"5" Body: {
   * "full_name": "Jane Doe" }
   */
  @PutMapping("/{type}/{id}")
  public ResponseEntity<Map<String, Object>> update(@PathVariable String type,
      @PathVariable String id, @RequestHeader("If-Match") String ifMatch,
      @RequestBody Map<String, Object> data, Authentication auth) {
    log.debug("Update {} id={}, If-Match={}", type, id, ifMatch);

    // Parse version from ETag (format: W/"5")
    long expectedVersion = parseETag(ifMatch);

    Map<String, Object> updated = crudService.update(type, id, expectedVersion, data, auth);

    // Set new ETag
    Object version = updated.get("version");
    String etag = version != null ? "W/\"" + version + "\"" : "W/\"0\"";

    return ResponseEntity.ok().eTag(etag).body(updated);
  }

  /**
   * Delete entity
   * 
   * DELETE /api/entities/UserProfile/123
   */
  @DeleteMapping("/{type}/{id}")
  public ResponseEntity<Void> delete(@PathVariable String type, @PathVariable String id,
      Authentication auth) {
    log.debug("Delete {} id={}", type, id);

    crudService.delete(type, id, auth);

    return ResponseEntity.noContent().build();
  }

  /**
   * Parse ETag header to extract version number Supports: W/"5", "5", 5
   */
  private long parseETag(String etag) {
    if (etag == null || etag.isBlank()) {
      throw new IllegalArgumentException("ETag is required");
    }

    // Remove W/ prefix and quotes
    String cleaned = etag.replace("W/", "").replace("\"", "").trim();

    try {
      return Long.parseLong(cleaned);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid ETag format: " + etag);
    }
  }
}
