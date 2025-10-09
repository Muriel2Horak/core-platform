package cz.muriel.core.dto;

import lombok.Data;

/**
 * DTO pro vytvoření/update skupiny
 */
@Data
public class GroupDto {
  private String name;
  private String path;
  private String tenantKey;
  private String parentGroupId;
  private String keycloakGroupId;
}
