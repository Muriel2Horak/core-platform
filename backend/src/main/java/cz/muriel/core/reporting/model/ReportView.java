package cz.muriel.core.reporting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Saved report view with scope-based sharing.
 */
@Entity
@Table(name = "report_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String entity;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Scope scope;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "group_id")
    private UUID groupId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode definition;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    @JsonIgnore
    private Integer version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (version == null) {
            version = 0;
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum Scope {
        PRIVATE,  // Only owner
        GROUP,    // Group members
        TENANT,   // All tenant users
        GLOBAL    // All tenants (admin only)
    }
}
