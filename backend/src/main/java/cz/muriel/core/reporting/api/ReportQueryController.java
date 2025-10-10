package cz.muriel.core.reporting.api;

import cz.muriel.core.reporting.app.ReportQueryService;
import cz.muriel.core.reporting.dsl.QueryRequest;
import cz.muriel.core.reporting.dsl.QueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for report queries.
 */
@Slf4j @RestController @RequestMapping("/api/reports") @RequiredArgsConstructor
public class ReportQueryController {

  private final ReportQueryService reportQueryService;

  /**
   * Execute report query.
   * 
   * POST /api/reports/query
   * 
   * @param request Query request
   * @param authentication Current user authentication
   * @return Query response with data
   */
  @PostMapping("/query")
  public ResponseEntity<QueryResponse> executeQuery(@Valid @RequestBody QueryRequest request,
      Authentication authentication) {

    log.debug("Executing report query: entity={}, measures={}", request.getEntity(),
        request.getMeasures());

    long startTime = System.currentTimeMillis();

    QueryResponse response = reportQueryService.executeQuery(request, authentication);

    long executionTime = System.currentTimeMillis() - startTime;

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Cache", Boolean.TRUE.equals(response.getCacheHit()) ? "HIT" : "MISS");
    headers.add("X-Query-Time-Ms", String.valueOf(executionTime));
    headers.add("X-Query-Fingerprint", response.getFingerprint());

    return ResponseEntity.ok().headers(headers).body(response);
  }

  /**
   * Get entity metadata.
   * 
   * GET /api/reports/metadata/{entity}
   * 
   * @param entity Entity name
   * @return Entity metadata with available dimensions/measures/filters
   */
  @GetMapping("/metadata/{entity}")
  public ResponseEntity<?> getEntityMetadata(@PathVariable String entity,
      Authentication authentication) {

    log.debug("Fetching metadata for entity: {}", entity);

    var metadata = reportQueryService.getEntityMetadata(entity);

    return ResponseEntity.ok(metadata);
  }

  /**
   * Validate query without execution (dry-run).
   * 
   * POST /api/reports/validate
   * 
   * @param request Query request
   * @return Validation result
   */
  @PostMapping("/validate")
  public ResponseEntity<?> validateQuery(@Valid @RequestBody QueryRequest request,
      Authentication authentication) {

    log.debug("Validating query: entity={}", request.getEntity());

    var result = reportQueryService.validateQuery(request, authentication);

    return ResponseEntity.ok(result);
  }

  /**
   * Health check endpoint.
   * 
   * GET /api/reports/health
   * 
   * @return Health status
   */
  @GetMapping("/health")
  public ResponseEntity<?> health() {
    return ResponseEntity.ok(java.util.Map.of("status", "UP", "service", "reporting", "timestamp",
        java.time.Instant.now()));
  }
}
