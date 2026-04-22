package com.mining.soft.infrastructure.adapter.in.web;
import com.mining.soft.application.port.in.GetNotificationsUseCase;
import com.mining.soft.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final GetNotificationsUseCase getNotificationsUseCase;
    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(getNotificationsUseCase.getNotifications(userId));
    }
    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(getNotificationsUseCase.getUnreadNotifications(userId));
    }
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        getNotificationsUseCase.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
