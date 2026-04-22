package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private Long userId;
    private String type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
