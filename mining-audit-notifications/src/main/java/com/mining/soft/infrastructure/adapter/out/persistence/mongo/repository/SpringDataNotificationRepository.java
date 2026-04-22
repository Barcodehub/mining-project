package com.mining.soft.infrastructure.adapter.out.persistence.mongo.repository;
import com.mining.soft.infrastructure.adapter.out.persistence.mongo.entity.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface SpringDataNotificationRepository extends MongoRepository<NotificationDocument, String> {
    List<NotificationDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<NotificationDocument> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
}
