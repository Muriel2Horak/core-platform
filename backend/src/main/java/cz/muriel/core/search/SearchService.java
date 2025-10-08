package cz.muriel.core.search;

import cz.muriel.core.metamodel.MetamodelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔍 Fulltext Search Service
 * 
 * Kombinuje vyhledávání v entitách (metamodel fulltext fields) a dokumentech.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final JdbcTemplate jdbcTemplate;
    private final MetamodelRegistry metamodelRegistry;

    @Value("${app.fulltext.max-results:100}")
    private int maxResults;

    @Value("${app.fulltext.min-query-length:3}")
    private int minQueryLength;

    /**
     * Universal search across entities and documents
     */
    public SearchModels.SearchResponse search(String tenantId, SearchModels.SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Validate query
        if (request.getQuery() == null || request.getQuery().length() < minQueryLength) {
            return SearchModels.SearchResponse.builder()
                .results(Collections.emptyList())
                .total(0)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
        }
        
        String tsQuery = prepareTsQuery(request.getQuery());
        int limit = Math.min(request.getLimit() > 0 ? request.getLimit() : 20, maxResults);
        
        List<SearchModels.SearchResult> allResults = new ArrayList<>();
        
        // Search in entities
        if (request.getEntityTypes() != null && !request.getEntityTypes().isEmpty()) {
            for (String entityType : request.getEntityTypes()) {
                if (!"Document".equals(entityType)) {
                    allResults.addAll(searchEntities(tenantId, entityType, tsQuery, limit));
                }
            }
        }
        
        // Search in documents
        if (request.getEntityTypes() == null || request.getEntityTypes().contains("Document")) {
            allResults.addAll(searchDocuments(tenantId, tsQuery, limit));
        }
        
        // Sort by score descending
        allResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // Apply limit and min score filter
        List<SearchModels.SearchResult> filteredResults = allResults.stream()
            .filter(r -> r.getScore() >= request.getMinScore())
            .limit(limit)
            .collect(Collectors.toList());
        
        long durationMs = System.currentTimeMillis() - startTime;
        
        log.info("Search completed: tenant={}, query='{}', types={}, results={}, took={}ms",
            tenantId, request.getQuery(), request.getEntityTypes(), filteredResults.size(), durationMs);
        
        return SearchModels.SearchResponse.builder()
            .results(filteredResults)
            .total(filteredResults.size())
            .durationMs(durationMs)
            .build();
    }

    /**
     * Search in entity tables (using fulltext fields from metamodel)
     */
    private List<SearchModels.SearchResult> searchEntities(
        String tenantId, 
        String entityType, 
        String tsQuery,
        int limit
    ) {
        try {
            // Get metamodel schema
            var schemaOpt = metamodelRegistry.getSchema(entityType);
            if (schemaOpt.isEmpty()) {
                return Collections.emptyList();
            }
            
            var schema = schemaOpt.get();
            
            // Get fulltext fields
            List<String> fulltextFields = schema.getFulltext();
            if (fulltextFields == null || fulltextFields.isEmpty()) {
                return Collections.emptyList();
            }
            
            String table = schema.getTable();
            String idField = schema.getIdField();
            
            // Build tsvector expression from fulltext fields
            String tsvectorExpr = fulltextFields.stream()
                .map(f -> "coalesce(" + f + "::text, '')")
                .collect(Collectors.joining(" || ' ' || "));
            
            // Query with ranking
            String sql = String.format(
                "SELECT %s as id, " +
                "  ts_rank(to_tsvector('english', %s), to_tsquery('english', ?)) as rank, " +
                "  ts_headline('english', %s, to_tsquery('english', ?), 'MaxWords=30, MinWords=15') as headline " +
                "FROM %s " +
                "WHERE tenant_id = ? " +
                "  AND to_tsvector('english', %s) @@ to_tsquery('english', ?) " +
                "ORDER BY rank DESC " +
                "LIMIT ?",
                idField, tsvectorExpr, tsvectorExpr, table, tsvectorExpr
            );
            
            List<SearchModels.SearchResult> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> SearchModels.SearchResult.builder()
                    .type("entity")
                    .entityType(entityType)
                    .id(rs.getString("id"))
                    .title(entityType + " " + rs.getString("id"))
                    .highlights(List.of(rs.getString("headline")))
                    .score(rs.getDouble("rank"))
                    .build(),
                tsQuery, tsQuery, tenantId, tsQuery, limit
            );
            
            log.debug("Entity search: type={}, results={}", entityType, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Failed to search entities: type={}, error={}", entityType, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search in documents
     */
    private List<SearchModels.SearchResult> searchDocuments(
        String tenantId,
        String tsQuery,
        int limit
    ) {
        try {
            String sql = 
                "SELECT d.id, d.filename, d.entity_type, d.entity_id, " +
                "  ts_rank(di.content_tsv, to_tsquery('english', ?)) as rank, " +
                "  ts_headline('english', substring(di.content_tsv::text, 1, 1000), " +
                "    to_tsquery('english', ?), 'MaxWords=30, MinWords=15') as headline " +
                "FROM document d " +
                "JOIN document_index di ON d.id = di.document_id " +
                "WHERE d.tenant_id = ? " +
                "  AND di.content_tsv @@ to_tsquery('english', ?) " +
                "ORDER BY rank DESC " +
                "LIMIT ?";
            
            List<SearchModels.SearchResult> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> SearchModels.SearchResult.builder()
                    .type("document")
                    .entityType("Document")
                    .id(rs.getString("id"))
                    .title(rs.getString("filename"))
                    .highlights(List.of(rs.getString("headline")))
                    .score(rs.getDouble("rank"))
                    .metadata(Map.of(
                        "entityType", rs.getString("entity_type") != null ? rs.getString("entity_type") : "",
                        "entityId", rs.getString("entity_id") != null ? rs.getString("entity_id") : ""
                    ))
                    .build(),
                tsQuery, tsQuery, tenantId, tsQuery, limit
            );
            
            log.debug("Document search: results={}", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Failed to search documents: error={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Prepare tsquery from user input
     * Converts "john doe" -> "john:* & doe:*"
     */
    private String prepareTsQuery(String query) {
        // Simple tokenization and prefix matching
        String[] tokens = query.trim().toLowerCase().split("\\s+");
        return Arrays.stream(tokens)
            .filter(t -> t.length() >= minQueryLength)
            .map(t -> t.replaceAll("[^a-z0-9]", "") + ":*")
            .collect(Collectors.joining(" & "));
    }
}
