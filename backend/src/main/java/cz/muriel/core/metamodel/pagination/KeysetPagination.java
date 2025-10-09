package cz.muriel.core.metamodel.pagination;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 📄 Keyset Pagination - Cursor-based pagination for large datasets
 * 
 * Instead of OFFSET/LIMIT, uses cursor (last seen value) for efficient paging.
 * Cursor format: "sortKey,id" (e.g., "2024-10-09T10:30:00Z,uuid-123")
 * 
 * Benefits: - No performance degradation with large offsets - Consistent
 * results even when data changes - Efficient for infinite scrolling
 * 
 * Trade-offs: - Cannot jump to arbitrary page numbers - Only forward/backward
 * navigation - Requires stable sort key (usually created_at + id)
 */
public class KeysetPagination {

  /**
   * Pagination request
   */
  @Data @Builder
  public static class Request {
    private String cursorNext; // Cursor to get next page
    private String cursorPrev; // Cursor to get previous page
    private Integer limit; // Page size (default 20, max 100)
    private String sortBy; // Sort field (default: created_at)
    private String sortOrder; // asc or desc (default: desc)
    private Boolean withTotal; // Include total count (slower, default: false)

    public int getEffectiveLimit() {
      if (limit == null || limit <= 0)
        return 20;
      return Math.min(limit, 100); // Max 100 items per page
    }

    public String getEffectiveSortBy() {
      return sortBy != null ? sortBy : "created_at";
    }

    public String getEffectiveSortOrder() {
      return sortOrder != null ? sortOrder : "desc";
    }
  }

  /**
   * Pagination response
   */
  @Data @Builder
  public static class Response<T> {
    private List<T> data;
    private String cursorNext; // Cursor for next page (null if last page)
    private String cursorPrev; // Cursor for previous page (null if first page)
    private Integer total; // Total count (only if withTotal=true)
    private Integer pageSize; // Current page size
    private Boolean hasNext; // Has next page
    private Boolean hasPrev; // Has previous page
  }

  /**
   * Cursor value (sortKey + id)
   */
  @Data @Builder
  public static class Cursor {
    private String sortKey; // Value of sort field
    private String id; // Entity ID (tie-breaker)

    /**
     * Encode cursor to string: "sortKey,id"
     */
    public String encode() {
      return sortKey + "," + id;
    }

    /**
     * Decode cursor from string
     */
    public static Cursor decode(String encoded) {
      if (encoded == null || encoded.isBlank()) {
        return null;
      }

      String[] parts = encoded.split(",", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid cursor format: " + encoded);
      }

      return Cursor.builder().sortKey(parts[0]).id(parts[1]).build();
    }
  }
}
