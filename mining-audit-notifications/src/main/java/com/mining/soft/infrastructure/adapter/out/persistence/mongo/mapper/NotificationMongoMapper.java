package com.mining.soft.infrastructure.adapter.out.persistence.mongo.mapper;
import com.mining.soft.domain.model.Notification;
import com.mining.soft.infrastructure.adapter.out.persistence.mongo.entity.NotificationDocument;
import org.springframework.stereotype.Component;
@Component
public class NotificationMongoMapper {
    public NotificationDocument toDocument(Notification domain) {
        if (domain == null) return null;
        return NotificationDocument.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .type(domain.getType())
                .message(domain.getMessage())
                .read(domain.isRead())
                .createdAt(domain.getCreatedAt())
                .build();
    }
    public Notification toDomain(NotificationDocument document) {
        if (document == null) return null;
        return Notification.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .type(document.getType())
                .message(document.getMessage())
                .read(document.isRead())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
