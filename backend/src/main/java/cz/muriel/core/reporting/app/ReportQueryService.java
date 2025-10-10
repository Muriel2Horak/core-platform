package cz.muriel.core.reporting.app;

import cz.muriel.core.reporting.cube.CubeClient;
import cz.muriel.core.reporting.cube.CubeMapper;
import cz.muriel.core.reporting.cube.CubeQueryRequest;
import cz.muriel.core.reporting.cube.CubeSecurityContext;
import cz.muriel.core.reporting.dsl.QueryRequest;
import cz.muriel.core.reporting.dsl.QueryResponse;
import cz.muriel.core.reporting.support.QueryFingerprint;
import cz.muriel.core.reporting.support.MetamodelSpecService;
import cz.muriel.core.reporting.support.EntitySpec;
import cz.muriel.core.reporting.support.ReportingMetrics;
import cz.muriel.core.reporting.support.LoggingContextFilter;
import cz.muriel.core.reporting.security.ReportingSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for executing report queries.
 */
@Slf4j @Service @RequiredArgsConstructor
public class ReportQueryService {

  private final CubeMapper cubeMapper;
  private final CubeClient cubeClient;
  private final CubeSecurityContext cubeSecurityContext;
  private final MetamodelSpecService metamodelSpecService;
  private final CacheManager cacheManager;
  private final QueryFingerprint queryFingerprint;
  private final ReportingMetrics metrics;
  private final ReportingSecurityService securityService;

  private static final String CACHE_NAME = "reportQueryCache";
  private static final String SPEC_VERSION = "1.0";

  /**
   * Execute report query with caching.
   * 
   * @param request Query request
   * @param authentication User authentication
   * @return Query response
   */
  public QueryResponse executeQuery(QueryRequest request, Authentication authentication) {
    long startTime = System.currentTimeMillis();
    metrics.recordQueryRequest();

    String tenantId = cubeSecurityContext.extractTenantId(authentication);
    if (tenantId == null) {
      throw new IllegalStateException("Tenant ID not found in authentication");
    }

    // Add to MDC for structured logging
    LoggingContextFilter.setTenantId(tenantId);
    String userId = cubeSecurityContext.extractUserId(authentication);
    if (userId != null) {
      LoggingContextFilter.setUserId(userId);
    }

    // Security validation
    securityService.validateEntityAccess(request.getEntity(), authentication);
    securityService.validateFieldAccess(request.getDimensions(),
        request.getMeasures() != null
            ? request.getMeasures().stream().map(QueryRequest.Measure::getField).toList()
            : null,
        authentication);
    securityService.validateRowLevelSecurity(tenantId, authentication);

    // Query complexity validation
    int dimensionCount = request.getDimensions() != null ? request.getDimensions().size() : 0;
    int measureCount = request.getMeasures() != null ? request.getMeasures().size() : 0;
    int filterCount = request.getFilters() != null ? request.getFilters().size() : 0;
    securityService.validateQueryComplexity(dimensionCount, measureCount, filterCount);

    // Generate cache key
    String fingerprint = queryFingerprint.generate(tenantId, request, SPEC_VERSION);

    // Check cache
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache != null) {
      QueryResponse cached = cache.get(fingerprint, QueryResponse.class);
      if (cached != null) {
        log.debug("Cache HIT for fingerprint: {}", fingerprint);
        metrics.recordCacheHit();
        long duration = System.currentTimeMillis() - startTime;
        metrics.recordQueryExecution(duration, request.getEntity(), true);
        cached.setCacheHit(true);
        return cached;
      }
    }

    log.debug("Cache MISS for fingerprint: {}", fingerprint);
    metrics.recordCacheMiss();

    // Execute query
    long cubeStartTime = System.currentTimeMillis();

    CubeQueryRequest cubeQuery = cubeMapper.toCubeQuery(request, tenantId);
    List<Map<String, Object>> data = cubeClient.executeQuery(cubeQuery);

    long cubeExecutionTime = System.currentTimeMillis() - cubeStartTime;
    metrics.recordCubeApiCall(cubeExecutionTime, true);

    long executionTime = System.currentTimeMillis() - startTime;

    // Build response
    QueryResponse response = QueryResponse.builder().data(data).totalRows((long) data.size()) // Cube.js
                                                                                              // doesn't
                                                                                              // return
                                                                                              // total
                                                                                              // count
        .returnedRows(data.size()).fingerprint(fingerprint).cacheHit(false)
        .executionTimeMs(executionTime).metadata(buildMetadata(request)).build();

    // Cache result
    if (cache != null) {
      cache.put(fingerprint, response);
    }

    metrics.recordQueryExecution(executionTime, request.getEntity(), false);

    return response;
  }

  /**
   * Get entity metadata.
   * 
   * @param entity Entity name
   * @return Entity spec
   */
  public EntitySpec getEntityMetadata(String entity) {
    return metamodelSpecService.getEntitySpec(entity);
  }

  /**
   * Validate query without execution.
   * 
   * @param request Query request
   * @param authentication User authentication
   * @return Validation result
   */
  public Map<String, Object> validateQuery(QueryRequest request, Authentication authentication) {
    String tenantId = cubeSecurityContext.extractTenantId(authentication);
    if (tenantId == null) {
      throw new IllegalStateException("Tenant ID not found in authentication");
    }

    Map<String, Object> result = new HashMap<>();
    result.put("valid", true);
    result.put("fingerprint", queryFingerprint.generate(tenantId, request, SPEC_VERSION));
    result.put("entity", request.getEntity());
    result.put("dimensions", request.getDimensions());
    result.put("measures", request.getMeasures());

    // Validate against metamodel
    EntitySpec spec = metamodelSpecService.getEntitySpec(request.getEntity());
    result.put("metadata", spec);

    return result;
  }

  /**
   * Build response metadata.
   */
  private Map<String, Object> buildMetadata(QueryRequest request) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("entity", request.getEntity());
    metadata.put("dimensions", request.getDimensions());
    metadata.put("measures", request.getMeasures());
    metadata.put("specVersion", SPEC_VERSION);
    return metadata;
  }
}
