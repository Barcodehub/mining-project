package com.mining.soft.infrastructure.adapter.out.persistence.mongo.repository;
import com.mining.soft.application.port.out.NotificationRepositoryPort;
import com.mining.soft.domain.model.Notification;
import com.mining.soft.infrastructure.adapter.out.persistence.mongo.entity.NotificationDocument;
import com.mining.soft.infrastructure.adapter.out.persistence.mongo.mapper.NotificationMongoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {
    private final SpringDataNotificationRepository repository;
    private final NotificationMongoMapper mapper;
    @Override
    public Notification save(Notification notification) {
        NotificationDocument document = mapper.toDocument(notification);
        document = repository.save(document);
        return mapper.toDomain(document);
    }
    @Override
    public List<Notification> findByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    @Override
    public Optional<Notification> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
