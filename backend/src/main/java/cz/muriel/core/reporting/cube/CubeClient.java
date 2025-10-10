package cz.muriel.core.reporting.cube;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client for Cube.js REST API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CubeClient {

    private final RestClient cubeRestClient;

    /**
     * Execute query against Cube.js API.
     * 
     * @param request Cube.js query request
     * @return Query results as list of maps
     * @throws IllegalArgumentException if query is invalid (4xx)
     * @throws RuntimeException if Cube.js server error (5xx)
     */
    public List<Map<String, Object>> executeQuery(CubeQueryRequest request) {
        log.debug("Executing Cube.js query: entity={}, measures={}, dimensions={}", 
            extractEntityFromMeasures(request), request.getMeasures(), request.getDimensions());

        try {
            CubeQueryResponse response = cubeRestClient.post()
                .uri("/cubejs-api/v1/load")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CubeQueryResponse.class);

            if (response == null || response.getData() == null) {
                log.warn("Cube.js returned null response or null data");
                return List.of();
            }

            // Check for errors in annotation
            if (response.getAnnotation() != null && response.getAnnotation().containsKey("error")) {
                String errorMsg = String.valueOf(response.getAnnotation().get("error"));
                log.error("Cube.js query error: {}", errorMsg);
                throw new IllegalArgumentException("Query execution failed: " + errorMsg);
            }

            // Log slow query warning
            if (Boolean.TRUE.equals(response.getSlowQuery())) {
                log.warn("Slow query detected: {}", request);
            }

            log.debug("Cube.js query successful: returned {} rows", response.getData().size());
            return response.getData();

        } catch (HttpClientErrorException e) {
            log.error("Cube.js client error (4xx): {}", e.getMessage());
            throw new IllegalArgumentException("Invalid query: " + e.getResponseBodyAsString(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("Cube.js server error (5xx): {}", e.getMessage());
            throw new RuntimeException("Cube.js server error - please retry later: " + e.getResponseBodyAsString(), e);
            
        } catch (Exception e) {
            log.error("Unexpected error during Cube.js query", e);
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract entity name from measures for logging.
     */
    private String extractEntityFromMeasures(CubeQueryRequest request) {
        if (request.getMeasures() != null && !request.getMeasures().isEmpty()) {
            String measure = request.getMeasures().get(0);
            if (measure.contains(".")) {
                return measure.substring(0, measure.indexOf('.'));
            }
        }
        if (request.getDimensions() != null && !request.getDimensions().isEmpty()) {
            String dimension = request.getDimensions().get(0);
            if (dimension.contains(".")) {
                return dimension.substring(0, dimension.indexOf('.'));
            }
        }
        return "unknown";
    }
}
