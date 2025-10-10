package cz.muriel.core.reporting.cube;

import cz.muriel.core.reporting.dsl.QueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps QueryRequest DSL to Cube.js query format.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CubeMapper {

    /**
     * Convert QueryRequest to Cube.js query format.
     * 
     * @param query QueryRequest DSL
     * @param tenantId Tenant ID for security context
     * @return CubeQueryRequest
     */
    public CubeQueryRequest toCubeQuery(QueryRequest query, String tenantId) {
        CubeQueryRequest.CubeQueryRequestBuilder builder = CubeQueryRequest.builder();

        // Measures
        if (query.getMeasures() != null && !query.getMeasures().isEmpty()) {
            List<String> measures = query.getMeasures().stream()
                .map(m -> toeCubeMeasure(query.getEntity(), m))
                .collect(Collectors.toList());
            builder.measures(measures);
        }

        // Dimensions
        if (query.getDimensions() != null && !query.getDimensions().isEmpty()) {
            List<String> dimensions = query.getDimensions().stream()
                .map(d -> toCubeDimension(query.getEntity(), d))
                .collect(Collectors.toList());
            builder.dimensions(dimensions);
        }

        // Time dimensions
        if (query.getTimeRange() != null) {
            String timeDimField = query.getTimeRange().getTimeDimension() != null 
                ? query.getTimeRange().getTimeDimension() 
                : "created_at";
            
            CubeQueryRequest.TimeDimension timeDim = CubeQueryRequest.TimeDimension.builder()
                .dimension(toCubeDimension(query.getEntity(), timeDimField))
                .dateRange(List.of(
                    query.getTimeRange().getStart().toString(),
                    query.getTimeRange().getEnd().toString()
                ))
                .build();
            
            builder.timeDimensions(List.of(timeDim));
        }

        // Filters
        List<CubeQueryRequest.Filter> cubeFilters = new ArrayList<>();
        
        // Add tenant filter (RLS)
        cubeFilters.add(CubeQueryRequest.Filter.builder()
            .member(toCubeDimension(query.getEntity(), "tenant_id"))
            .operator("equals")
            .values(List.of(tenantId))
            .build());

        // Add user filters
        if (query.getFilters() != null && !query.getFilters().isEmpty()) {
            for (QueryRequest.Filter filter : query.getFilters()) {
                CubeQueryRequest.Filter cubeFilter = CubeQueryRequest.Filter.builder()
                    .member(toCubeDimension(query.getEntity(), filter.getField()))
                    .operator(mapOperator(filter.getOperator()))
                    .values(normalizeValue(filter.getValue()))
                    .build();
                cubeFilters.add(cubeFilter);
            }
        }
        
        builder.filters(cubeFilters);

        // Order
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
            List<List<String>> order = query.getOrderBy().stream()
                .map(o -> List.of(
                    toCubeDimension(query.getEntity(), o.getField()),
                    o.getDirection().toLowerCase()
                ))
                .collect(Collectors.toList());
            builder.order(order);
        }

        // Pagination
        if (query.getLimit() != null) {
            builder.limit(query.getLimit());
        }
        if (query.getOffset() != null) {
            builder.offset(query.getOffset());
        }

        // Security context
        Map<String, Object> securityContext = new HashMap<>();
        securityContext.put("tenantId", tenantId);
        builder.securityContext(securityContext);

        return builder.build();
    }

    /**
     * Convert Cube.js measure to format: EntityName.measureField_aggregation
     */
    private String toeCubeMeasure(String entity, QueryRequest.Measure measure) {
        String entityName = capitalize(entity);
        String aggregation = capitalize(measure.getAggregation());
        return entityName + "." + measure.getField() + aggregation;
    }

    /**
     * Convert dimension to Cube.js format: EntityName.field
     */
    private String toCubeDimension(String entity, String field) {
        String entityName = capitalize(entity);
        return entityName + "." + field;
    }

    /**
     * Map DSL operator to Cube.js operator.
     */
    private String mapOperator(String dslOperator) {
        switch (dslOperator.toLowerCase()) {
            case "eq": return "equals";
            case "ne": return "notEquals";
            case "gt": return "gt";
            case "gte": return "gte";
            case "lt": return "lt";
            case "lte": return "lte";
            case "in": return "equals"; // Cube uses equals with array
            case "notin": return "notEquals";
            case "contains": return "contains";
            case "startswith": return "startsWith";
            case "endswith": return "endsWith";
            case "between": return "inDateRange";
            default: return "equals";
        }
    }

    /**
     * Normalize filter value to array format expected by Cube.
     */
    @SuppressWarnings("unchecked")
    private List<Object> normalizeValue(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return List.of(value);
    }

    /**
     * Capitalize first letter (User, Project, etc.)
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
