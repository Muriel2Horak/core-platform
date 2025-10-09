package cz.muriel.core.reporting.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.reporting.dsl.QueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates deterministic fingerprints for query caching.
 * 
 * The fingerprint includes:
 * - Tenant ID
 * - Entity name
 * - Sorted dimensions
 * - Sorted measures
 * - Sorted filters
 * - Time range
 * - Metamodel spec version (for cache invalidation)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryFingerprint {

    private final ObjectMapper objectMapper;

    /**
     * Generate a deterministic fingerprint for a query.
     * 
     * @param tenantId Tenant ID
     * @param query Query request
     * @param specVersion Metamodel spec version
     * @return SHA-256 hex fingerprint
     */
    public String generate(String tenantId, QueryRequest query, String specVersion) {
        try {
            Map<String, Object> fingerprintData = new LinkedHashMap<>();
            
            fingerprintData.put("tenant", tenantId);
            fingerprintData.put("entity", query.getEntity());
            fingerprintData.put("specVersion", specVersion);
            
            // Sort dimensions
            if (query.getDimensions() != null && !query.getDimensions().isEmpty()) {
                List<String> sorted = new ArrayList<>(query.getDimensions());
                Collections.sort(sorted);
                fingerprintData.put("dimensions", sorted);
            }
            
            // Sort measures
            if (query.getMeasures() != null && !query.getMeasures().isEmpty()) {
                List<String> sortedMeasures = query.getMeasures().stream()
                    .sorted(Comparator.comparing(QueryRequest.Measure::getField)
                        .thenComparing(QueryRequest.Measure::getAggregation))
                    .map(m -> m.getField() + ":" + m.getAggregation())
                    .collect(Collectors.toList());
                fingerprintData.put("measures", sortedMeasures);
            }
            
            // Sort filters
            if (query.getFilters() != null && !query.getFilters().isEmpty()) {
                List<Map<String, Object>> sortedFilters = query.getFilters().stream()
                    .sorted(Comparator.comparing(QueryRequest.Filter::getField)
                        .thenComparing(QueryRequest.Filter::getOperator))
                    .map(f -> {
                        Map<String, Object> filterMap = new LinkedHashMap<>();
                        filterMap.put("field", f.getField());
                        filterMap.put("operator", f.getOperator());
                        filterMap.put("value", normalizeValue(f.getValue()));
                        return filterMap;
                    })
                    .collect(Collectors.toList());
                fingerprintData.put("filters", sortedFilters);
            }
            
            // Time range
            if (query.getTimeRange() != null) {
                Map<String, Object> timeRange = new LinkedHashMap<>();
                timeRange.put("start", query.getTimeRange().getStart().toString());
                timeRange.put("end", query.getTimeRange().getEnd().toString());
                if (query.getTimeRange().getTimeDimension() != null) {
                    timeRange.put("dimension", query.getTimeRange().getTimeDimension());
                }
                fingerprintData.put("timeRange", timeRange);
            }
            
            // Include pagination (affects result set)
            if (query.getLimit() != null) {
                fingerprintData.put("limit", query.getLimit());
            }
            if (query.getOffset() != null) {
                fingerprintData.put("offset", query.getOffset());
            }
            
            // OrderBy
            if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
                List<String> orderBy = query.getOrderBy().stream()
                    .map(o -> o.getField() + ":" + o.getDirection())
                    .collect(Collectors.toList());
                fingerprintData.put("orderBy", orderBy);
            }
            
            String json = objectMapper.writeValueAsString(fingerprintData);
            return sha256Hex(json);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to generate query fingerprint", e);
            throw new RuntimeException("Failed to generate query fingerprint", e);
        }
    }

    /**
     * Normalize value for consistent hashing.
     */
    private Object normalizeValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> sorted = new ArrayList<>(list);
            sorted.sort((a, b) -> String.valueOf(a).compareTo(String.valueOf(b)));
            return sorted;
        }
        return value;
    }

    /**
     * Generate SHA-256 hex digest.
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
