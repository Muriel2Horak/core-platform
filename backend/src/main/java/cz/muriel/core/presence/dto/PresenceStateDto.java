package cz.muriel.core.presence.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * DTO representing current presence state for an entity
 */
@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class PresenceStateDto {

  private String tenantId;
  private String entity;
  private String id;
  private Set<Object> users;
  private Boolean stale;
  private String busyBy;
  private Long version;
}
