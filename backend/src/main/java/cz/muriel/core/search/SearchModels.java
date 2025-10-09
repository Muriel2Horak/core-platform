package cz.muriel.core.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 🔍 Search Models
 */
public class SearchModels {

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class SearchRequest {
    private String query;
    private List<String> entityTypes;
    private int limit;
    private double minScore;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class SearchResult {
    private String type; // "entity" or "document"
    private String entityType; // UserProfile, Document, etc.
    private String id;
    private String title;
    private List<String> highlights;
    private double score;
    private Map<String, Object> metadata;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class SearchResponse {
    private List<SearchResult> results;
    private int total;
    private long durationMs;
  }
}
