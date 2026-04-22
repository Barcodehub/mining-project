package com.mining.soft.application.port.in;
import com.mining.soft.domain.model.Notification;
import java.util.List;
public interface GetNotificationsUseCase {
    List<Notification> getNotifications(Long userId);
    List<Notification> getUnreadNotifications(Long userId);
    void markAsRead(String notificationId);
}
