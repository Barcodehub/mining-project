package com.mining.soft.application.port.out;
import com.mining.soft.domain.model.Notification;
import java.util.List;
import java.util.Optional;
public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    List<Notification> findByUserId(Long userId);
    List<Notification> findUnreadByUserId(Long userId);
    Optional<Notification> findById(String id);
}
