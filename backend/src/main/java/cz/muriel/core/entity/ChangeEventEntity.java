package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🔄 CDC Change Event Entity - mapuje change_events tabulku z Keycloak DB
 * Minimalistická struktura - pouze ID, typ eventu a entity ID Backend si načte
 * detaily z Keycloak Admin API
 */
@Entity @Table(name = "change_events") @Data @NoArgsConstructor @AllArgsConstructor
public class ChangeEventEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_type", nullable = false)
  private String eventType; // USER_CREATED, USER_UPDATED, ROLE_CREATED, etc.

  @Column(name = "entity_id", nullable = false)
  private String entityId; // Keycloak entity ID (user_id, role_id, group_id)

  @Column(name = "realm_id", nullable = false)
  private String realmId; // Keycloak realm_id

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "processed", nullable = false)
  private Boolean processed = false;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  /**
   * Označí event jako zpracovaný
   */
  public void markAsProcessed() {
    this.processed = true;
    this.processedAt = LocalDateTime.now();
  }

  /**
   * Zkontroluje, zda je event typu USER
   */
  public boolean isUserEvent() {
    return eventType != null && eventType.startsWith("USER_");
  }

  /**
   * Zkontroluje, zda je event typu ROLE
   */
  public boolean isRoleEvent() {
    return eventType != null && eventType.startsWith("ROLE_");
  }

  /**
   * Zkontroluje, zda je event typu GROUP
   */
  public boolean isGroupEvent() {
    return eventType != null && eventType.startsWith("GROUP_");
  }
}
