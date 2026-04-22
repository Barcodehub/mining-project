package com.mining.soft.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    private Long userId;
    private String action;
    private String entity;
    private Long entityId;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
