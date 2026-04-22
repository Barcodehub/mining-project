package com.mining.soft.infrastructure.adapter.out.persistence.mongo.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class NotificationDocument {
    @Id
    private String id;
    private Long userId;
    private String type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
