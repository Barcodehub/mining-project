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
public class ExternalEvent {
    private String eventType;
    private Long entityId;
    private Long userId;
    private Long targetUserId;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}

