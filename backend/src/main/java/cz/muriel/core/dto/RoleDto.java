package cz.muriel.core.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoleDto {
  private String id;
  private String name;
  private String description;
  private boolean composite;
  private Map<String, Object> attributes;
}
