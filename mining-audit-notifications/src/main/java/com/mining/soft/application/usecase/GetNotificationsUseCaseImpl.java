package com.mining.soft.application.usecase;
import com.mining.soft.application.port.in.GetNotificationsUseCase;
import com.mining.soft.application.port.out.NotificationRepositoryPort;
import com.mining.soft.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
public class GetNotificationsUseCaseImpl implements GetNotificationsUseCase {
    private final NotificationRepositoryPort notificationRepositoryPort;
    @Override
    public List<Notification> getNotifications(Long userId) {
        return notificationRepositoryPort.findByUserId(userId);
    }
    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepositoryPort.findUnreadByUserId(userId);
    }
    @Override
    public void markAsRead(String notificationId) {
        notificationRepositoryPort.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepositoryPort.save(notification);
        });
    }
}
