package cz.muriel.core.search;

import cz.muriel.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 🔍 Search REST Controller
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Universal fulltext search
     * 
     * Example: GET /api/search?q=john&types=UserProfile,Document&limit=20&minScore=0.1
     */
    @GetMapping
    public ResponseEntity<SearchModels.SearchResponse> search(
        @RequestParam String q,
        @RequestParam(required = false) String types,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0.0") double minScore
    ) {
        String tenantId = TenantContext.getTenantKey();
        
        List<String> entityTypes = null;
        if (types != null && !types.isEmpty()) {
            entityTypes = Arrays.asList(types.split(","));
        }
        
        SearchModels.SearchRequest request = SearchModels.SearchRequest.builder()
            .query(q)
            .entityTypes(entityTypes)
            .limit(limit)
            .minScore(minScore)
            .build();
        
        SearchModels.SearchResponse response = searchService.search(tenantId, request);
        
        return ResponseEntity.ok(response);
    }
}
