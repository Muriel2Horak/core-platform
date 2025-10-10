package cz.muriel.core.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessageDto {
    private UUID id;
    private String entity;
    private UUID entityId;
    private String operation;
    private String errorType;
    private String errorMessage;
    private LocalDateTime movedToDlqAt;
    private Integer attemptCount;
    private String payload;
}
