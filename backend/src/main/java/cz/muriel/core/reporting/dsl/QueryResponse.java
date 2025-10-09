package cz.muriel.core.reporting.dsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response for a query execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {

    /**
     * Query results.
     */
    private List<Map<String, Object>> data;

    /**
     * Total number of rows (before pagination).
     */
    private Long totalRows;

    /**
     * Number of rows returned in this response.
     */
    private Integer returnedRows;

    /**
     * Query fingerprint (for caching).
     */
    private String fingerprint;

    /**
     * Cache hit indicator.
     */
    private Boolean cacheHit;

    /**
     * Query execution time in milliseconds.
     */
    private Long executionTimeMs;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;
}
