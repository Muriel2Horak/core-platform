package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 🔄 User Change Event Entity
 * 
 * Entita pro zachycení změn uživatelů z Keycloak DB triggers. Nahrazuje SPI
 * webhook implementaci pomocí Postgres NOTIFY/LISTEN.
 */
@Entity @Table(name = "user_change_events") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserChangeEventEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "operation", nullable = false) @Enumerated(EnumType.STRING)
  private OperationType operation;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "realm_id", nullable = false)
  private String realmId;

  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload", columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "processed", nullable = false)
  private Boolean processed = false;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  public enum OperationType {
    INSERT, UPDATE, DELETE
  }

  /**
   * Označí event jako zpracovaný
   */
  public void markAsProcessed() {
    this.processed = true;
    this.processedAt = LocalDateTime.now();
  }

  /**
   * Zjistí, zda je event starší než daný počet sekund
   */
  public boolean isOlderThan(int seconds) {
    return createdAt.isBefore(LocalDateTime.now().minusSeconds(seconds));
  }
}
