package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sync_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncExecution {
    
    @Id
    private String id; // Použijeme stejné UUID jako syncId
    
    @Column(nullable = false)
    private String type; // users, roles, groups, all
    
    @Column(nullable = false)
    private String tenantKey;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStatus status;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer totalItems;
    
    private Integer processedItems;
    
    @ElementCollection
    @CollectionTable(name = "sync_execution_errors", joinColumns = @JoinColumn(name = "sync_execution_id"))
    @Column(name = "error_message", length = 1000)
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    @Column(length = 500)
    private String initiatedBy; // Username kdo spustil
    
    public enum SyncStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    @PrePersist
    public void prePersist() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (status == null) {
            status = SyncStatus.RUNNING;
        }
    }
}
